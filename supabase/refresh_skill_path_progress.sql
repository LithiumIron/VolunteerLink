-- Correctness rule:
-- A Skill Path assignment is verified only when the volunteer's application
-- is ACCEPTED and the participation itself is COMPLETED. Attendance minutes
-- are counted only for those same completed participations.
--
-- Run this file once in Supabase SQL Editor. The Android app then calls the
-- function whenever it loads Skill Path data.

create or replace function public.refresh_my_skill_path_progress()
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    current_volunteer_user_id text;
    path_record record;
    completed_assignments integer;
    completed_minutes integer;
    calculated_level integer;
begin
    select up.user_id
    into current_volunteer_user_id
    from public.user_profiles up
    where up.auth_user_id = auth.uid()
      and up.account_type = 'VOLUNTEER'
    limit 1;

    if current_volunteer_user_id is null then
        raise exception 'No volunteer profile is linked to the authenticated user';
    end if;

    for path_record in
        select
            sp.skill_path_id,
            sp.progression_type
        from public.skill_paths sp
        order by sp.skill_path_id
    loop
        select
            count(distinct rp.participation_id)::integer,
            coalesce(sum(ar.verified_minutes), 0)::integer
        into
            completed_assignments,
            completed_minutes
        from public.role_participations rp
        join public.post_roles pr
          on pr.post_role_id = rp.post_role_id
        join public.role_templates rt
          on rt.role_template_id = pr.role_template_id
        left join public.attendance_records ar
          on ar.participation_id = rp.participation_id
        where rp.user_id = current_volunteer_user_id
          and rt.skill_path_id = path_record.skill_path_id
          and rp.application_status = 'ACCEPTED'
          and rp.completion_status = 'COMPLETED';

        select coalesce(max(spl.level_number), 1)
        into calculated_level
        from public.skill_path_levels spl
        where spl.skill_path_id = path_record.skill_path_id
          and spl.required_assignments <= completed_assignments
          and (
              spl.required_minutes is null
              or spl.required_minutes <= completed_minutes
          );

        update public.volunteer_skill_path_progress progress
        set
            current_level = calculated_level,
            verified_assignments = completed_assignments,
            verified_minutes = case
                when path_record.progression_type = 'ASSIGNMENTS_ONLY'
                    then null
                else completed_minutes
            end,
            updated_at = now()
        where progress.user_id = current_volunteer_user_id
          and progress.skill_path_id = path_record.skill_path_id;

        if not found then
            insert into public.volunteer_skill_path_progress (
                user_id,
                skill_path_id,
                current_level,
                verified_assignments,
                verified_minutes
            ) values (
                current_volunteer_user_id,
                path_record.skill_path_id,
                calculated_level,
                completed_assignments,
                case
                    when path_record.progression_type = 'ASSIGNMENTS_ONLY'
                        then null
                    else completed_minutes
                end
            );
        end if;
    end loop;
end;
$$;

revoke all on function
    public.refresh_my_skill_path_progress()
    from public;

grant execute on function
    public.refresh_my_skill_path_progress()
    to authenticated;

comment on function
    public.refresh_my_skill_path_progress()
is 'Recomputes the authenticated volunteer Skill Path progress from ACCEPTED and COMPLETED role participations only.';

-- Correct the currently signed-in demo volunteer immediately when this script
-- is run from an authenticated client. SQL Editor runs as postgres and has no
-- auth.uid(), so the Android app performs the first refresh automatically.
