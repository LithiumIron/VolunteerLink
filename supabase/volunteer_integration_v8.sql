-- VolunteerLink volunteer integration for the 2026-08-18 composite-role schema.
-- Run as one migration after reviewing in the Supabase SQL Editor.

create or replace function public.get_published_role_metrics()
returns table (
    post_role_id text,
    application_count integer,
    accepted_count integer,
    available_spots integer
)
language sql
stable
security definer
set search_path = public
as $$
    select
        pr.post_id || '|' || pr.role_template_id as post_role_id,
        count(rp.participation_id) filter (
            where rp.application_status not in ('DECLINED', 'CANCELLED')
        )::integer,
        count(rp.participation_id) filter (
            where rp.application_status = 'ACCEPTED'
        )::integer,
        greatest(
            pr.capacity - count(rp.participation_id) filter (
                where rp.application_status = 'ACCEPTED'
            ),
            0
        )::integer
    from public.post_roles pr
    join public.volunteer_posts vp on vp.post_id = pr.post_id
    left join public.role_participations rp
      on rp.post_id = pr.post_id
     and rp.role_template_id = pr.role_template_id
    where vp.status = 'PUBLISHED'
    group by pr.post_id, pr.role_template_id, pr.capacity
    order by pr.post_id, pr.role_template_id;
$$;

create or replace function public.submit_role_application(
    target_post_role_id text,
    provided_screening_answers jsonb default '[]'::jsonb
)
returns public.role_participations
language plpgsql
security definer
set search_path = ''
as $$
declare
    volunteer_profile_id text;
    selected_post_id text := split_part(target_post_role_id, '|', 1);
    selected_role_template_id text := split_part(target_post_role_id, '|', 2);
    selected_method text;
    selected_questions jsonb;
    selected_capacity integer;
    accepted_count integer;
    existing_row public.role_participations%rowtype;
    created_row public.role_participations%rowtype;
begin
    if auth.uid() is null then
        raise exception 'Authentication is required.' using errcode = '28000';
    end if;

    if selected_post_id = '' or selected_role_template_id = '' then
        raise exception 'Invalid volunteer role identifier.' using errcode = '22023';
    end if;

    select up.user_id into volunteer_profile_id
    from public.user_profiles up
    where up.auth_user_id = auth.uid()
      and up.account_type = 'VOLUNTEER'
    limit 1;

    if volunteer_profile_id is null then
        raise exception 'A volunteer profile is required.' using errcode = 'P0001';
    end if;

    select pr.application_method, pr.screening_questions, pr.capacity
      into selected_method, selected_questions, selected_capacity
    from public.post_roles pr
    join public.volunteer_posts vp on vp.post_id = pr.post_id
    where pr.post_id = selected_post_id
      and pr.role_template_id = selected_role_template_id
      and vp.status = 'PUBLISHED'
    for update of pr;

    if not found then
        raise exception 'The selected role is unavailable.' using errcode = 'P0001';
    end if;

    select rp.* into existing_row
    from public.role_participations rp
    where rp.post_id = selected_post_id
      and rp.role_template_id = selected_role_template_id
      and rp.user_id = volunteer_profile_id
    limit 1;

    if found then return existing_row; end if;

    if provided_screening_answers is null
       or jsonb_typeof(provided_screening_answers) <> 'array' then
        raise exception 'Screening answers must be a JSON array.' using errcode = '22023';
    end if;

    if selected_method = 'REVIEW_APPLICANTS' then
        if jsonb_array_length(provided_screening_answers) <>
           jsonb_array_length(selected_questions) then
            raise exception 'Every screening question must be answered.' using errcode = 'P0001';
        end if;
        if exists (
            select 1
            from jsonb_array_elements(provided_screening_answers) answer
            where jsonb_typeof(answer) <> 'object'
               or btrim(coalesce(answer ->> 'answer', '')) = ''
        ) then
            raise exception 'Screening answers cannot be empty.' using errcode = 'P0001';
        end if;
    end if;

    if selected_method = 'INSTANT_JOIN' then
        select count(*) into accepted_count
        from public.role_participations rp
        where rp.post_id = selected_post_id
          and rp.role_template_id = selected_role_template_id
          and rp.application_status = 'ACCEPTED';
        if accepted_count >= selected_capacity then
            raise exception 'This volunteer role is full.' using errcode = 'P0001';
        end if;
    end if;

    insert into public.role_participations (
        user_id, application_status, completion_status,
        screening_answers, auto_completed, joined_at,
        post_id, role_template_id
    ) values (
        volunteer_profile_id,
        case when selected_method = 'INSTANT_JOIN' then 'ACCEPTED' else 'PENDING' end,
        'IN_PROGRESS',
        provided_screening_answers,
        false,
        case when selected_method = 'INSTANT_JOIN' then now() else null end,
        selected_post_id,
        selected_role_template_id
    ) returning * into created_row;

    return created_row;
end;
$$;

create or replace function public.get_my_volunteer_applications()
returns table (
    participation_id text,
    post_id text,
    post_role_id text,
    event_title text,
    organisation_name text,
    role_title text,
    volunteer_name text,
    application_status text,
    completion_status text,
    decision_note text,
    verified_minutes integer,
    completed_at timestamptz,
    feedback text,
    primary_skill_path text,
    practised_skill_names jsonb,
    event_date text,
    event_time text,
    event_location text,
    created_at timestamptz
)
language sql
stable
security definer
set search_path = public
as $$
    select
        rp.participation_id,
        rp.post_id,
        rp.post_id || '|' || rp.role_template_id,
        vp.title,
        o.organisation_name,
        rt.role_name,
        up.full_name,
        rp.application_status,
        rp.completion_status,
        rp.decision_note,
        case when rp.completion_status = 'COMPLETED'
            then coalesce(att.verified_minutes, 0)::integer else null end,
        rp.completed_at,
        ve.feedback,
        sp.name,
        coalesce(skill_data.skill_names, '[]'::jsonb),
        coalesce(pd.start_date, rd.start_date)::text,
        case when pd.post_id is not null
            then to_char(pd.start_time, 'FMHH12:MI AM') || ' - ' ||
                 to_char(pd.end_time, 'FMHH12:MI AM')
            else 'Flexible' end,
        case when pd.post_id is not null
            then coalesce(nullif(pd.location_address, ''), pd.location_name)
            else 'Online' end,
        rp.created_at
    from public.role_participations rp
    join public.user_profiles up on up.user_id = rp.user_id
    join public.post_roles pr
      on pr.post_id = rp.post_id
     and pr.role_template_id = rp.role_template_id
    join public.volunteer_posts vp on vp.post_id = rp.post_id
    join public.organisations o on o.organisation_id = vp.organisation_id
    join public.role_templates rt on rt.role_template_id = rp.role_template_id
    join public.skill_paths sp on sp.skill_path_id = rt.skill_path_id
    left join public.physical_details pd on pd.post_id = rp.post_id
    left join public.remote_details rd on rd.post_id = rp.post_id
    left join public.volunteer_evaluations ve
      on ve.participation_id = rp.participation_id
    left join lateral (
        select sum(ar.verified_minutes)::integer as verified_minutes
        from public.attendance_records ar
        where ar.participation_id = rp.participation_id
    ) att on true
    left join lateral (
        select jsonb_agg(s.name order by s.name) as skill_names
        from jsonb_array_elements_text(
            case when jsonb_array_length(pr.practised_skills) > 0
                then pr.practised_skills else rt.skills_practised end
        ) ids(skill_id)
        join public.skills s on s.skill_id = ids.skill_id
    ) skill_data on true
    where up.auth_user_id = auth.uid()
      and up.account_type = 'VOLUNTEER'
    order by rp.created_at desc;
$$;

create or replace function public.refresh_my_skill_path_progress()
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    volunteer_id text;
    path_record record;
    completed_assignments integer;
    completed_minutes integer;
    calculated_level integer;
begin
    select up.user_id into volunteer_id
    from public.user_profiles up
    where up.auth_user_id = auth.uid()
      and up.account_type = 'VOLUNTEER'
    limit 1;
    if volunteer_id is null then
        raise exception 'No volunteer profile is linked to the authenticated user';
    end if;

    for path_record in select * from public.skill_paths loop
        select count(distinct rp.participation_id)::integer,
               coalesce(sum(ar.verified_minutes), 0)::integer
          into completed_assignments, completed_minutes
        from public.role_participations rp
        join public.role_templates rt
          on rt.role_template_id = rp.role_template_id
        left join public.attendance_records ar
          on ar.participation_id = rp.participation_id
        where rp.user_id = volunteer_id
          and rt.skill_path_id = path_record.skill_path_id
          and rp.application_status = 'ACCEPTED'
          and rp.completion_status = 'COMPLETED';

        select coalesce(max(level_number), 1) into calculated_level
        from public.skill_path_levels
        where skill_path_id = path_record.skill_path_id
          and required_assignments <= completed_assignments
          and (required_minutes is null or required_minutes <= completed_minutes);

        insert into public.volunteer_skill_path_progress (
            user_id, skill_path_id, current_level,
            verified_assignments, verified_minutes, updated_at
        ) values (
            volunteer_id, path_record.skill_path_id, calculated_level,
            completed_assignments,
            case when path_record.progression_type = 'ASSIGNMENTS_ONLY'
                then null else completed_minutes end,
            now()
        )
        on conflict (user_id, skill_path_id) do update set
            current_level = excluded.current_level,
            verified_assignments = excluded.verified_assignments,
            verified_minutes = excluded.verified_minutes,
            updated_at = excluded.updated_at;
    end loop;
end;
$$;

-- Cloud notification inbox. Local Room caching can mirror these rows later.
create table if not exists public.volunteer_notifications (
    notification_id bigint generated always as identity primary key,
    user_id text not null references public.user_profiles(user_id) on delete cascade,
    notification_type text not null check (notification_type in (
        'APPLICATION_SUBMITTED', 'APPLICATION_ACCEPTED',
        'APPLICATION_REJECTED', 'SCHEDULE_CHANGED', 'EVENT_REMINDER',
        'CERTIFICATE_ISSUED', 'SKILL_LEVEL_UP', 'MATCHING_OPPORTUNITY'
    )),
    title text not null,
    message text not null,
    related_post_id text references public.volunteer_posts(post_id) on delete cascade,
    related_participation_id text references public.role_participations(participation_id) on delete cascade,
    is_read boolean not null default false,
    created_at timestamptz not null default now()
);

alter table public.volunteer_notifications enable row level security;

drop policy if exists volunteer_notifications_select_own
    on public.volunteer_notifications;
create policy volunteer_notifications_select_own
on public.volunteer_notifications for select to authenticated
using (user_id in (
    select up.user_id from public.user_profiles up
    where up.auth_user_id = auth.uid()
));

drop policy if exists volunteer_notifications_update_own
    on public.volunteer_notifications;
create policy volunteer_notifications_update_own
on public.volunteer_notifications for update to authenticated
using (user_id in (
    select up.user_id from public.user_profiles up
    where up.auth_user_id = auth.uid()
))
with check (user_id in (
    select up.user_id from public.user_profiles up
    where up.auth_user_id = auth.uid()
));

create or replace function public.notify_volunteer_application_change()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    event_title text;
begin
    select vp.title into event_title
    from public.volunteer_posts vp where vp.post_id = new.post_id;

    if tg_op = 'INSERT' then
        insert into public.volunteer_notifications (
            user_id, notification_type, title, message,
            related_post_id, related_participation_id
        ) values (
            new.user_id,
            'APPLICATION_SUBMITTED',
            case when new.application_status = 'ACCEPTED'
                then 'Place confirmed' else 'Application submitted' end,
            case when new.application_status = 'ACCEPTED'
                then 'You joined ' || event_title || ' successfully.'
                else event_title || ' is reviewing your application.' end,
            new.post_id,
            new.participation_id
        );
    elsif new.application_status is distinct from old.application_status then
        if new.application_status in ('ACCEPTED', 'DECLINED') then
            insert into public.volunteer_notifications (
                user_id, notification_type, title, message,
                related_post_id, related_participation_id
            ) values (
                new.user_id,
                case when new.application_status = 'ACCEPTED'
                    then 'APPLICATION_ACCEPTED' else 'APPLICATION_REJECTED' end,
                case when new.application_status = 'ACCEPTED'
                    then 'Application accepted' else 'Application update' end,
                case when new.application_status = 'ACCEPTED'
                    then 'Your place for ' || event_title || ' is confirmed.'
                    else 'Your application for ' || event_title || ' was not selected.' end,
                new.post_id,
                new.participation_id
            );
        end if;
    end if;
    return new;
end;
$$;

drop trigger if exists role_participation_notification
    on public.role_participations;
create trigger role_participation_notification
after insert or update of application_status
on public.role_participations
for each row execute function public.notify_volunteer_application_change();

grant execute on function public.submit_role_application(text, jsonb)
    to authenticated;
grant execute on function public.get_published_role_metrics()
    to anon, authenticated;
grant execute on function public.get_my_volunteer_applications()
    to authenticated;
grant execute on function public.refresh_my_skill_path_progress()
    to authenticated;
grant select, update on public.volunteer_notifications to authenticated;

create or replace function public.mark_my_notifications_read()
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
    affected integer;
begin
    update public.volunteer_notifications n
    set is_read = true
    where n.is_read = false
      and n.user_id in (
          select up.user_id from public.user_profiles up
          where up.auth_user_id = auth.uid()
      );
    get diagnostics affected = row_count;
    return affected;
end;
$$;

grant execute on function public.mark_my_notifications_read()
    to authenticated;
