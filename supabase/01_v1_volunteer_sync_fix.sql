-- VolunteerLink V13: one-source sync fix for v1_erd_test.
-- Scope: Volunteer application/history/growth data only.
-- Safe to rerun. It does not drop tables and does not touch Organisation UI code.

begin;

-- The existing evaluation row is the organisation's verification record.
-- Store the verified duration there instead of introducing another evidence table.
alter table v1_erd_test.volunteer_evaluations
    add column if not exists verified_minutes integer;

alter table v1_erd_test.volunteer_evaluations
    drop constraint if exists volunteer_evaluations_verified_minutes_check;

alter table v1_erd_test.volunteer_evaluations
    add constraint volunteer_evaluations_verified_minutes_check
    check (verified_minutes is null or verified_minutes >= 0);

-- Normalized answers for Review Applicants. No JSON file/column is used.
create table if not exists v1_erd_test.role_participation_screening_answers (
    post_id text not null,
    role_template_id text not null,
    user_id text not null,
    question_no smallint not null,
    question_text text not null,
    answer_text text not null,
    created_at timestamptz not null default now(),
    primary key (post_id, role_template_id, user_id, question_no),
    foreign key (post_id, role_template_id, user_id)
        references v1_erd_test.role_participations
            (post_id, role_template_id, user_id)
        on delete cascade,
    check (question_no > 0),
    check (btrim(answer_text) <> '')
);

alter table v1_erd_test.role_participation_screening_answers
    enable row level security;

drop policy if exists volunteer_read_own_screening_answers
on v1_erd_test.role_participation_screening_answers;

create policy volunteer_read_own_screening_answers
on v1_erd_test.role_participation_screening_answers
for select to authenticated
using (
    user_id in (
        select profile.user_id
        from v1_erd_test.user_profiles profile
        where profile.auth_user_id = auth.uid()
    )
);

grant usage on schema v1_erd_test to authenticated;
grant select on v1_erd_test.role_participation_screening_answers
to authenticated;

-- Database-side equivalent of AppClock. New writes use the same clock as Android.
create or replace function v1_erd_test.volunteer_app_now()
returns timestamptz
language sql
stable
security definer
set search_path = v1_erd_test, pg_temp
as $$
    select coalesce(
        (
            select case
                when clock.use_test_time
                    then clock.test_datetime
                else now()
            end
            from v1_erd_test.app_test_clock clock
            where clock.clock_name = 'APP'
            limit 1
        ),
        now()
    );
$$;

create or replace function v1_erd_test.submit_role_application(
    target_post_role_id text,
    provided_screening_answers jsonb default '[]'::jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = v1_erd_test, pg_temp
as $$
declare
    volunteer_id text;
    selected_post_id text := split_part(target_post_role_id, '|', 1);
    selected_role_id text := split_part(target_post_role_id, '|', 2);
    selected_method text;
    selected_capacity integer;
    question_count integer;
    accepted_count integer;
    event_start date;
    submitted_at timestamptz := v1_erd_test.volunteer_app_now();
begin
    if auth.uid() is null then
        raise exception 'Authentication is required.' using errcode = '28000';
    end if;

    if selected_post_id = '' or selected_role_id = '' then
        raise exception 'Invalid volunteer role identifier.' using errcode = '22023';
    end if;

    select profile.user_id
    into volunteer_id
    from v1_erd_test.user_profiles profile
    where profile.auth_user_id = auth.uid()
      and profile.account_type = 'VOLUNTEER'
    limit 1;

    if volunteer_id is null then
        raise exception 'A volunteer profile is required.';
    end if;

    select role.application_method, role.capacity
    into selected_method, selected_capacity
    from v1_erd_test.post_roles role
    join v1_erd_test.volunteer_posts post
      on post.post_id = role.post_id
    where role.post_id = selected_post_id
      and role.role_template_id = selected_role_id
      and post.status = 'PUBLISHED'
    for update of role;

    if not found then
        raise exception 'The selected role is unavailable.';
    end if;

    select min(candidate.start_date)
    into event_start
    from (
        select physical.start_date
        from v1_erd_test.physical_details physical
        where physical.post_id = selected_post_id
        union all
        select remote.start_date
        from v1_erd_test.remote_details remote
        where remote.post_id = selected_post_id
    ) candidate;

    if event_start is null then
        raise exception 'The opportunity has no valid start date.';
    end if;

    if submitted_at::date >= event_start then
        raise exception 'Applications are closed because this opportunity has started.';
    end if;

    if exists (
        select 1
        from v1_erd_test.role_participations participation
        where participation.post_id = selected_post_id
          and participation.role_template_id = selected_role_id
          and participation.user_id = volunteer_id
    ) then
        raise exception 'You already have an application for this role.';
    end if;

    select count(*)::integer
    into question_count
    from v1_erd_test.post_role_screening_questions question
    where question.post_id = selected_post_id
      and question.role_template_id = selected_role_id;

    if provided_screening_answers is null
       or jsonb_typeof(provided_screening_answers) <> 'array' then
        raise exception 'Screening answers must be an array.';
    end if;

    if question_count > 0 then
        if jsonb_array_length(provided_screening_answers) <> question_count then
            raise exception 'Every screening question must be answered.';
        end if;

        if exists (
            select 1
            from jsonb_array_elements(provided_screening_answers) answer
            where btrim(coalesce(answer ->> 'answer', '')) = ''
        ) then
            raise exception 'Screening answers cannot be empty.';
        end if;
    end if;

    if selected_method = 'INSTANT_JOIN' then
        select count(*)::integer
        into accepted_count
        from v1_erd_test.role_participations participation
        where participation.post_id = selected_post_id
          and participation.role_template_id = selected_role_id
          and participation.application_status = 'ACCEPTED';

        if accepted_count >= selected_capacity then
            raise exception 'ROLE_FULL: This volunteer role has reached its capacity.';
        end if;
    end if;

    insert into v1_erd_test.role_participations (
        post_id,
        role_template_id,
        user_id,
        application_status,
        completion_status,
        auto_completed,
        joined_at,
        created_at
    ) values (
        selected_post_id,
        selected_role_id,
        volunteer_id,
        case
            when selected_method = 'INSTANT_JOIN' then 'ACCEPTED'
            else 'PENDING'
        end,
        'IN_PROGRESS',
        false,
        case
            when selected_method = 'INSTANT_JOIN' then submitted_at
            else null
        end,
        submitted_at
    );

    if question_count > 0 then
        insert into v1_erd_test.role_participation_screening_answers (
            post_id,
            role_template_id,
            user_id,
            question_no,
            question_text,
            answer_text,
            created_at
        )
        select
            selected_post_id,
            selected_role_id,
            volunteer_id,
            question.question_no,
            question.question_text,
            answer.value ->> 'answer',
            submitted_at
        from v1_erd_test.post_role_screening_questions question
        join lateral jsonb_array_elements(provided_screening_answers)
            with ordinality answer(value, answer_no)
          on answer.answer_no = question.question_no
        where question.post_id = selected_post_id
          and question.role_template_id = selected_role_id;
    end if;

    return jsonb_build_object(
        'post_id', selected_post_id,
        'role_template_id', selected_role_id,
        'user_id', volunteer_id,
        'application_status',
            case
                when selected_method = 'INSTANT_JOIN' then 'ACCEPTED'
                else 'PENDING'
            end,
        'created_at', submitted_at
    );
end;
$$;

create or replace function v1_erd_test.cancel_my_application(
    target_participation_id text
)
returns jsonb
language plpgsql
security definer
set search_path = v1_erd_test, pg_temp
as $$
declare
    volunteer_id text;
    selected_post_id text := split_part(target_participation_id, '|', 1);
    selected_role_id text := split_part(target_participation_id, '|', 2);
    affected integer;
    cancelled_time timestamptz := v1_erd_test.volunteer_app_now();
begin
    if auth.uid() is null then
        raise exception 'Authentication is required.' using errcode = '28000';
    end if;

    select profile.user_id
    into volunteer_id
    from v1_erd_test.user_profiles profile
    where profile.auth_user_id = auth.uid()
      and profile.account_type = 'VOLUNTEER'
    limit 1;

    update v1_erd_test.role_participations participation
    set application_status = 'CANCELLED',
        cancelled_at = cancelled_time,
        decision_note = 'Cancelled by volunteer.',
        is_shortlisted = false
    where participation.post_id = selected_post_id
      and participation.role_template_id = selected_role_id
      and participation.user_id = volunteer_id
      and participation.application_status in ('PENDING', 'ACCEPTED')
      and participation.completion_status <> 'COMPLETED';

    get diagnostics affected = row_count;
    if affected = 0 then
        raise exception 'This application cannot be cancelled.';
    end if;

    return jsonb_build_object('cancelled', true);
end;
$$;

create or replace function v1_erd_test.get_published_opportunity_metrics()
returns table (
    post_id text,
    application_count integer,
    available_spots integer
)
language sql
stable
security definer
set search_path = v1_erd_test, pg_temp
as $$
    select
        post.post_id,
        coalesce(metric.application_count, 0),
        coalesce(metric.available_spots, 0)
    from v1_erd_test.volunteer_posts post
    left join lateral (
        select
            coalesce(sum(role_count.application_count), 0)::integer
                as application_count,
            coalesce(sum(role_count.available_spots), 0)::integer
                as available_spots
        from (
            select
                role.role_template_id,
                count(participation.user_id) filter (
                    where participation.application_status not in ('DECLINED', 'CANCELLED')
                )::integer as application_count,
                greatest(
                    role.capacity - count(participation.user_id) filter (
                        where participation.application_status = 'ACCEPTED'
                    ),
                    0
                )::integer as available_spots
            from v1_erd_test.post_roles role
            left join v1_erd_test.role_participations participation
              on participation.post_id = role.post_id
             and participation.role_template_id = role.role_template_id
            where role.post_id = post.post_id
            group by role.role_template_id, role.capacity
        ) role_count
    ) metric on true
    where post.status = 'PUBLISHED'
    order by post.post_id;
$$;

create or replace function v1_erd_test.get_published_role_metrics()
returns table (
    post_role_id text,
    application_count integer,
    accepted_count integer,
    available_spots integer
)
language sql
stable
security definer
set search_path = v1_erd_test, pg_temp
as $$
    select
        role.post_id || '|' || role.role_template_id,
        count(participation.user_id) filter (
            where participation.application_status not in ('DECLINED', 'CANCELLED')
        )::integer,
        count(participation.user_id) filter (
            where participation.application_status = 'ACCEPTED'
        )::integer,
        greatest(
            role.capacity - count(participation.user_id) filter (
                where participation.application_status = 'ACCEPTED'
            ),
            0
        )::integer
    from v1_erd_test.post_roles role
    join v1_erd_test.volunteer_posts post
      on post.post_id = role.post_id
    left join v1_erd_test.role_participations participation
      on participation.post_id = role.post_id
     and participation.role_template_id = role.role_template_id
    where post.status = 'PUBLISHED'
    group by role.post_id, role.role_template_id, role.capacity;
$$;

create or replace function v1_erd_test.get_my_volunteer_achievement_records()
returns table (
    participation_id text,
    verified_minutes integer,
    completed_at timestamptz,
    rating integer,
    feedback text,
    volunteer_name text
)
language sql
stable
security definer
set search_path = v1_erd_test, pg_temp
as $$
    select
        participation.post_id || '|' || participation.role_template_id || '|' || participation.user_id,
        evaluation.verified_minutes,
        participation.completed_at,
        evaluation.rating,
        evaluation.feedback,
        profile.full_name
    from v1_erd_test.role_participations participation
    join v1_erd_test.user_profiles profile
      on profile.user_id = participation.user_id
    join v1_erd_test.volunteer_evaluations evaluation
      on evaluation.post_id = participation.post_id
     and evaluation.role_template_id = participation.role_template_id
     and evaluation.user_id = participation.user_id
    join v1_erd_test.volunteer_certificates certificate
      on certificate.post_id = participation.post_id
     and certificate.role_template_id = participation.role_template_id
     and certificate.user_id = participation.user_id
    where profile.auth_user_id = auth.uid()
      and participation.application_status = 'ACCEPTED'
      and participation.completion_status = 'COMPLETED'
      and evaluation.verified_minutes is not null
    order by participation.completed_at desc;
$$;

create or replace function v1_erd_test.get_my_volunteer_applications()
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
set search_path = v1_erd_test, pg_temp
as $$
    select
        participation.post_id || '|' || participation.role_template_id || '|' || participation.user_id,
        participation.post_id,
        participation.post_id || '|' || participation.role_template_id,
        post.title,
        organisation.organisation_name,
        template.role_name,
        profile.full_name,
        participation.application_status,
        participation.completion_status,
        participation.decision_note,
        case
            when participation.completion_status = 'COMPLETED'
                then evaluation.verified_minutes
            else null
        end,
        participation.completed_at,
        evaluation.feedback,
        path.name,
        coalesce(skill_data.skill_names, '[]'::jsonb),
        event_data.start_date::text,
        event_data.event_time,
        event_data.event_location,
        participation.created_at
    from v1_erd_test.role_participations participation
    join v1_erd_test.user_profiles profile
      on profile.user_id = participation.user_id
    join v1_erd_test.volunteer_posts post
      on post.post_id = participation.post_id
    join v1_erd_test.organisations organisation
      on organisation.organisation_id = post.organisation_id
    join v1_erd_test.role_templates template
      on template.role_template_id = participation.role_template_id
    join v1_erd_test.skill_paths path
      on path.skill_path_id = template.skill_path_id
    left join v1_erd_test.volunteer_evaluations evaluation
      on evaluation.post_id = participation.post_id
     and evaluation.role_template_id = participation.role_template_id
     and evaluation.user_id = participation.user_id
    left join lateral (
        select
            min(source.start_date) as start_date,
            max(source.event_time) filter (where source.event_time <> 'Flexible') as event_time,
            max(source.event_location) filter (where source.event_location <> 'Online') as event_location
        from (
            select
                physical.start_date,
                to_char(physical.start_time, 'FMHH12:MI AM') || ' - ' ||
                    to_char(physical.end_time, 'FMHH12:MI AM') as event_time,
                coalesce(nullif(physical.location_address, ''), physical.location_name) as event_location
            from v1_erd_test.physical_details physical
            where physical.post_id = participation.post_id
            union all
            select remote.start_date, 'Flexible', 'Online'
            from v1_erd_test.remote_details remote
            where remote.post_id = participation.post_id
        ) source
    ) event_data on true
    left join lateral (
        select jsonb_agg(skill.name order by skill.name) as skill_names
        from (
            select role_skill.skill_id
            from v1_erd_test.post_role_skills role_skill
            where role_skill.post_id = participation.post_id
              and role_skill.role_template_id = participation.role_template_id
            union
            select template_skill.skill_id
            from v1_erd_test.role_template_skills template_skill
            where template_skill.role_template_id = participation.role_template_id
              and not exists (
                  select 1
                  from v1_erd_test.post_role_skills configured
                  where configured.post_id = participation.post_id
                    and configured.role_template_id = participation.role_template_id
              )
        ) selected_skill
        join v1_erd_test.skills skill
          on skill.skill_id = selected_skill.skill_id
    ) skill_data on true
    where profile.auth_user_id = auth.uid()
      and profile.account_type = 'VOLUNTEER'
    order by participation.created_at desc;
$$;

create or replace function v1_erd_test.refresh_my_skill_path_progress()
returns void
language plpgsql
security definer
set search_path = v1_erd_test, pg_temp
as $$
declare
    volunteer_id text;
begin
    select profile.user_id
    into volunteer_id
    from v1_erd_test.user_profiles profile
    where profile.auth_user_id = auth.uid()
      and profile.account_type = 'VOLUNTEER'
    limit 1;

    if volunteer_id is null then
        raise exception 'A volunteer profile is required.';
    end if;

    insert into v1_erd_test.volunteer_skill_path_progress (
        user_id,
        skill_path_id,
        current_level,
        verified_assignments,
        verified_minutes,
        updated_at
    )
    select
        volunteer_id,
        path.skill_path_id,
        coalesce((
            select max(level.level_number)
            from v1_erd_test.skill_path_levels level
            where level.skill_path_id = path.skill_path_id
              and level.required_assignments <= coalesce(total.assignments, 0)
              and (
                  level.required_minutes is null
                  or level.required_minutes <= coalesce(total.minutes, 0)
              )
        ), 1),
        coalesce(total.assignments, 0),
        case
            when path.progression_type = 'ASSIGNMENTS_ONLY' then null
            else coalesce(total.minutes, 0)
        end,
        v1_erd_test.volunteer_app_now()
    from v1_erd_test.skill_paths path
    left join lateral (
        select
            count(*)::integer as assignments,
            coalesce(sum(evaluation.verified_minutes), 0)::integer as minutes
        from v1_erd_test.role_participations participation
        join v1_erd_test.role_templates template
          on template.role_template_id = participation.role_template_id
        join v1_erd_test.volunteer_evaluations evaluation
          on evaluation.post_id = participation.post_id
         and evaluation.role_template_id = participation.role_template_id
         and evaluation.user_id = participation.user_id
        join v1_erd_test.volunteer_certificates certificate
          on certificate.post_id = participation.post_id
         and certificate.role_template_id = participation.role_template_id
         and certificate.user_id = participation.user_id
        where participation.user_id = volunteer_id
          and template.skill_path_id = path.skill_path_id
          and participation.application_status = 'ACCEPTED'
          and participation.completion_status = 'COMPLETED'
          and evaluation.verified_minutes is not null
    ) total on true
    on conflict (user_id, skill_path_id) do update set
        current_level = excluded.current_level,
        verified_assignments = excluded.verified_assignments,
        verified_minutes = excluded.verified_minutes,
        updated_at = excluded.updated_at;
end;
$$;

revoke all on function v1_erd_test.submit_role_application(text, jsonb) from public;
revoke all on function v1_erd_test.cancel_my_application(text) from public;
revoke all on function v1_erd_test.get_my_volunteer_applications() from public;
revoke all on function v1_erd_test.get_my_volunteer_achievement_records() from public;
revoke all on function v1_erd_test.refresh_my_skill_path_progress() from public;

grant execute on function v1_erd_test.submit_role_application(text, jsonb) to authenticated;
grant execute on function v1_erd_test.cancel_my_application(text) to authenticated;
grant execute on function v1_erd_test.get_my_volunteer_applications() to authenticated;
grant execute on function v1_erd_test.get_my_volunteer_achievement_records() to authenticated;
grant execute on function v1_erd_test.refresh_my_skill_path_progress() to authenticated;
grant execute on function v1_erd_test.get_published_opportunity_metrics() to anon, authenticated;
grant execute on function v1_erd_test.get_published_role_metrics() to anon, authenticated;

-- ---------------------------------------------------------------------------
-- USER005 timeline repair, limited to invalid future fixtures.
-- ---------------------------------------------------------------------------

with app_time as (
    select v1_erd_test.volunteer_app_now() as now_value
), event_bounds as (
    select
        source.post_id,
        min(source.start_date) as start_date,
        max(source.end_date) as end_date
    from (
        select post_id, start_date, end_date
        from v1_erd_test.physical_details
        union all
        select post_id, start_date, end_date
        from v1_erd_test.remote_details
    ) source
    group by source.post_id
)
update v1_erd_test.role_participations participation
set created_at = least(
        app_time.now_value - interval '1 day',
        ((event_bounds.start_date - 1)::timestamp
            at time zone 'Asia/Kuala_Lumpur')
    )
from event_bounds, app_time
where participation.user_id = 'USER005'
  and participation.post_id = event_bounds.post_id
  and (
      participation.created_at > app_time.now_value
      or participation.created_at::date >= event_bounds.start_date
  );

-- If a fixture says Completed before its event has ended, remove only that
-- USER005 completion evidence and return it to IN_PROGRESS.
with app_time as (
    select v1_erd_test.volunteer_app_now() as now_value
), future_completed as (
    select participation.post_id, participation.role_template_id, participation.user_id
    from v1_erd_test.role_participations participation
    join (
        select source.post_id, max(source.end_date) as end_date
        from (
            select post_id, end_date from v1_erd_test.physical_details
            union all
            select post_id, end_date from v1_erd_test.remote_details
        ) source
        group by source.post_id
    ) event_bounds on event_bounds.post_id = participation.post_id
    cross join app_time
    where participation.user_id = 'USER005'
      and participation.completion_status = 'COMPLETED'
      and event_bounds.end_date > app_time.now_value::date
)
delete from v1_erd_test.volunteer_skill_experiences experience
using future_completed invalid
where experience.post_id = invalid.post_id
  and experience.role_template_id = invalid.role_template_id
  and experience.user_id = invalid.user_id;

with app_time as (
    select v1_erd_test.volunteer_app_now() as now_value
), future_completed as (
    select participation.post_id, participation.role_template_id, participation.user_id
    from v1_erd_test.role_participations participation
    join (
        select source.post_id, max(source.end_date) as end_date
        from (
            select post_id, end_date from v1_erd_test.physical_details
            union all
            select post_id, end_date from v1_erd_test.remote_details
        ) source
        group by source.post_id
    ) event_bounds on event_bounds.post_id = participation.post_id
    cross join app_time
    where participation.user_id = 'USER005'
      and participation.completion_status = 'COMPLETED'
      and event_bounds.end_date > app_time.now_value::date
)
delete from v1_erd_test.volunteer_certificates certificate
using future_completed invalid
where certificate.post_id = invalid.post_id
  and certificate.role_template_id = invalid.role_template_id
  and certificate.user_id = invalid.user_id;

with app_time as (
    select v1_erd_test.volunteer_app_now() as now_value
), future_completed as (
    select participation.post_id, participation.role_template_id, participation.user_id
    from v1_erd_test.role_participations participation
    join (
        select source.post_id, max(source.end_date) as end_date
        from (
            select post_id, end_date from v1_erd_test.physical_details
            union all
            select post_id, end_date from v1_erd_test.remote_details
        ) source
        group by source.post_id
    ) event_bounds on event_bounds.post_id = participation.post_id
    cross join app_time
    where participation.user_id = 'USER005'
      and participation.completion_status = 'COMPLETED'
      and event_bounds.end_date > app_time.now_value::date
)
delete from v1_erd_test.volunteer_evaluations evaluation
using future_completed invalid
where evaluation.post_id = invalid.post_id
  and evaluation.role_template_id = invalid.role_template_id
  and evaluation.user_id = invalid.user_id;

with app_time as (
    select v1_erd_test.volunteer_app_now() as now_value
), event_bounds as (
    select source.post_id, max(source.end_date) as end_date
    from (
        select post_id, end_date from v1_erd_test.physical_details
        union all
        select post_id, end_date from v1_erd_test.remote_details
    ) source
    group by source.post_id
)
update v1_erd_test.role_participations participation
set completion_status = 'IN_PROGRESS',
    completed_at = null,
    auto_completed = false
from event_bounds, app_time
where participation.user_id = 'USER005'
  and participation.post_id = event_bounds.post_id
  and participation.completion_status = 'COMPLETED'
  and event_bounds.end_date > app_time.now_value::date;

-- Repair a completed fixture whose event is already past but completion time
-- itself was incorrectly placed after AppClock.
with app_time as (
    select v1_erd_test.volunteer_app_now() as now_value
), event_bounds as (
    select source.post_id, max(source.end_date) as end_date
    from (
        select post_id, end_date from v1_erd_test.physical_details
        union all
        select post_id, end_date from v1_erd_test.remote_details
    ) source
    group by source.post_id
)
update v1_erd_test.role_participations participation
set completed_at = (
        event_bounds.end_date::timestamp + time '17:00'
    ) at time zone 'Asia/Kuala_Lumpur'
from event_bounds, app_time
where participation.user_id = 'USER005'
  and participation.post_id = event_bounds.post_id
  and participation.completion_status = 'COMPLETED'
  and event_bounds.end_date <= app_time.now_value::date
  and participation.completed_at > app_time.now_value;

-- Existing POST012 is a valid past completed role after its bad date is repaired.
update v1_erd_test.volunteer_evaluations evaluation
set verified_minutes = 480,
    created_at = participation.completed_at
from v1_erd_test.role_participations participation
where evaluation.post_id = participation.post_id
  and evaluation.role_template_id = participation.role_template_id
  and evaluation.user_id = participation.user_id
  and participation.post_id = 'POST012'
  and participation.role_template_id = 'ROLE019'
  and participation.user_id = 'USER005'
  and participation.completion_status = 'COMPLETED';

update v1_erd_test.volunteer_certificates certificate
set issued_at = participation.completed_at
from v1_erd_test.role_participations participation
where certificate.post_id = participation.post_id
  and certificate.role_template_id = participation.role_template_id
  and certificate.user_id = participation.user_id
  and participation.post_id = 'POST012'
  and participation.role_template_id = 'ROLE019'
  and participation.user_id = 'USER005'
  and participation.completion_status = 'COMPLETED';

update v1_erd_test.volunteer_skill_experiences experience
set verified_at = participation.completed_at
from v1_erd_test.role_participations participation
where experience.post_id = participation.post_id
  and experience.role_template_id = participation.role_template_id
  and experience.user_id = participation.user_id
  and participation.post_id = 'POST012'
  and participation.role_template_id = 'ROLE019'
  and participation.user_id = 'USER005'
  and participation.completion_status = 'COMPLETED';

-- ---------------------------------------------------------------------------
-- Two realistic past records. Together with POST012 this gives USER005 three
-- verified completions and 1,140 minutes (19 hours). All dates precede AppClock.
-- ---------------------------------------------------------------------------

insert into v1_erd_test.volunteer_posts (
    post_id, organisation_id, title, description, mode, status,
    created_at, published_at, updated_at, category
) values
(
    'VHIST001', 'ORG0001', 'George Town Community Welcome Day',
    'Supported registration, welcomed residents and guided visitors to community services.',
    'PHYSICAL', 'COMPLETED',
    '2026-06-01 09:00:00+08', '2026-06-01 09:00:00+08',
    '2026-06-20 14:00:00+08', 'COMMUNITY'
),
(
    'VHIST002', 'ORG0001', 'Family Food Pack Distribution',
    'Prepared food packs, checked quantities and supported a respectful collection flow.',
    'PHYSICAL', 'COMPLETED',
    '2026-06-22 09:00:00+08', '2026-06-22 09:00:00+08',
    '2026-07-12 14:30:00+08', 'COMMUNITY'
)
on conflict (post_id) do update set
    title = excluded.title,
    description = excluded.description,
    mode = excluded.mode,
    status = excluded.status,
    updated_at = excluded.updated_at,
    category = excluded.category;

insert into v1_erd_test.physical_details (
    post_id, start_date, end_date, start_time, end_time,
    location_name, location_address, state_region, country,
    latitude, longitude, meeting_point, volunteer_capacity, time_zone
) values
(
    'VHIST001', '2026-06-20', '2026-06-20', '09:00', '14:00',
    'George Town Community Centre', 'Lebuh Acheh, 10200 George Town, Penang',
    'Penang', 'Malaysia', 5.4144, 100.3296,
    'Main registration desk', 12, 'Asia/Kuala_Lumpur'
),
(
    'VHIST002', '2026-07-12', '2026-07-12', '08:30', '14:30',
    'Pusat Komuniti Seberang Jaya', '13700 Perai, Penang',
    'Penang', 'Malaysia', 5.3979, 100.4025,
    'Volunteer service entrance', 16, 'Asia/Kuala_Lumpur'
)
on conflict (post_id) do update set
    start_date = excluded.start_date,
    end_date = excluded.end_date,
    start_time = excluded.start_time,
    end_time = excluded.end_time,
    location_name = excluded.location_name,
    location_address = excluded.location_address,
    latitude = excluded.latitude,
    longitude = excluded.longitude;

insert into v1_erd_test.post_roles (
    post_id, role_template_id, capacity, application_method, role_notes
) values
('VHIST001', 'ROLE003', 12, 'INSTANT_JOIN', 'Welcome participants and explain the service flow.'),
('VHIST002', 'ROLE001', 16, 'INSTANT_JOIN', 'Prepare supplies and maintain accurate packing quantities.')
on conflict (post_id, role_template_id) do update set
    capacity = excluded.capacity,
    application_method = excluded.application_method,
    role_notes = excluded.role_notes;

insert into v1_erd_test.post_role_skills (
    post_id, role_template_id, skill_id, required_experience
)
select history.post_id, history.role_template_id, template_skill.skill_id, 0
from (
    values ('VHIST001'::text, 'ROLE003'::text),
           ('VHIST002'::text, 'ROLE001'::text)
) history(post_id, role_template_id)
join v1_erd_test.role_template_skills template_skill
  on template_skill.role_template_id = history.role_template_id
on conflict (post_id, role_template_id, skill_id) do update set
    required_experience = excluded.required_experience;

insert into v1_erd_test.schedule_items (
    schedule_item_id, post_id, schedule_type, schedule_date,
    title, start_time, end_time, location, notes
) values
('VHSCH001', 'VHIST001', 'PHYSICAL', '2026-06-20',
 'Welcome service and verified checkout', '09:00', '14:00',
 'George Town Community Centre', 'Completion verified by the organisation.'),
('VHSCH002', 'VHIST002', 'PHYSICAL', '2026-07-12',
 'Packing service and verified checkout', '08:30', '14:30',
 'Pusat Komuniti Seberang Jaya', 'Completion verified by the organisation.')
on conflict (schedule_item_id) do update set
    schedule_date = excluded.schedule_date,
    title = excluded.title,
    start_time = excluded.start_time,
    end_time = excluded.end_time,
    location = excluded.location,
    notes = excluded.notes;

insert into v1_erd_test.role_participations (
    post_id, role_template_id, user_id, application_status,
    completion_status, auto_completed, joined_at, completed_at,
    created_at, decision_note
) values
('VHIST001', 'ROLE003', 'USER005', 'ACCEPTED', 'COMPLETED', false,
 '2026-06-20 08:45:00+08', '2026-06-20 14:00:00+08',
 '2026-06-08 10:00:00+08', 'Completion verified by the organisation.'),
('VHIST002', 'ROLE001', 'USER005', 'ACCEPTED', 'COMPLETED', false,
 '2026-07-12 08:15:00+08', '2026-07-12 14:30:00+08',
 '2026-06-29 11:00:00+08', 'Completion verified by the organisation.')
on conflict (post_id, role_template_id, user_id) do update set
    application_status = excluded.application_status,
    completion_status = excluded.completion_status,
    auto_completed = excluded.auto_completed,
    joined_at = excluded.joined_at,
    completed_at = excluded.completed_at,
    created_at = excluded.created_at,
    decision_note = excluded.decision_note;

insert into v1_erd_test.volunteer_evaluations (
    post_id, role_template_id, user_id, organisation_id,
    rating, feedback, verified_minutes, created_at
) values
('VHIST001', 'ROLE003', 'USER005', 'ORG0001', 5,
 'Communicated clearly and guided participants confidently.', 300,
 '2026-06-20 14:00:00+08'),
('VHIST002', 'ROLE001', 'USER005', 'ORG0001', 4,
 'Worked reliably with the packing team and maintained accurate quantities.', 360,
 '2026-07-12 14:30:00+08')
on conflict (post_id, role_template_id, user_id) do update set
    rating = excluded.rating,
    feedback = excluded.feedback,
    verified_minutes = excluded.verified_minutes,
    created_at = excluded.created_at;

insert into v1_erd_test.volunteer_certificates (
    post_id, role_template_id, user_id, certificate_path, issued_at
) values
('VHIST001', 'ROLE003', 'USER005', null, '2026-06-20 14:00:00+08'),
('VHIST002', 'ROLE001', 'USER005', null, '2026-07-12 14:30:00+08')
on conflict (post_id, role_template_id, user_id) do update set
    issued_at = excluded.issued_at;

insert into v1_erd_test.volunteer_skill_experiences (
    post_id, role_template_id, user_id, skill_id, verified_at
)
select
    role_skill.post_id,
    role_skill.role_template_id,
    'USER005',
    role_skill.skill_id,
    case
        when role_skill.post_id = 'VHIST001' then '2026-06-20 14:00:00+08'::timestamptz
        else '2026-07-12 14:30:00+08'::timestamptz
    end
from v1_erd_test.post_role_skills role_skill
where role_skill.post_id in ('VHIST001', 'VHIST002')
on conflict (post_id, role_template_id, user_id, skill_id) do update set
    verified_at = excluded.verified_at;

-- Rebuild USER005 Skill Path totals from the same completed/evaluated/certified rows.
insert into v1_erd_test.volunteer_skill_path_progress (
    user_id, skill_path_id, current_level,
    verified_assignments, verified_minutes, updated_at
)
select
    'USER005',
    path.skill_path_id,
    coalesce((
        select max(level.level_number)
        from v1_erd_test.skill_path_levels level
        where level.skill_path_id = path.skill_path_id
          and level.required_assignments <= coalesce(total.assignments, 0)
          and (
              level.required_minutes is null
              or level.required_minutes <= coalesce(total.minutes, 0)
          )
    ), 1),
    coalesce(total.assignments, 0),
    case
        when path.progression_type = 'ASSIGNMENTS_ONLY' then null
        else coalesce(total.minutes, 0)
    end,
    v1_erd_test.volunteer_app_now()
from v1_erd_test.skill_paths path
left join lateral (
    select
        count(*)::integer as assignments,
        coalesce(sum(evaluation.verified_minutes), 0)::integer as minutes
    from v1_erd_test.role_participations participation
    join v1_erd_test.role_templates template
      on template.role_template_id = participation.role_template_id
    join v1_erd_test.volunteer_evaluations evaluation
      on evaluation.post_id = participation.post_id
     and evaluation.role_template_id = participation.role_template_id
     and evaluation.user_id = participation.user_id
    join v1_erd_test.volunteer_certificates certificate
      on certificate.post_id = participation.post_id
     and certificate.role_template_id = participation.role_template_id
     and certificate.user_id = participation.user_id
    where participation.user_id = 'USER005'
      and template.skill_path_id = path.skill_path_id
      and participation.application_status = 'ACCEPTED'
      and participation.completion_status = 'COMPLETED'
      and evaluation.verified_minutes is not null
) total on true
on conflict (user_id, skill_path_id) do update set
    current_level = excluded.current_level,
    verified_assignments = excluded.verified_assignments,
    verified_minutes = excluded.verified_minutes,
    updated_at = excluded.updated_at;

commit;

-- Result must show no future timeline violations and at least 3 verified rows.
with event_bounds as (
    select source.post_id, min(source.start_date) start_date, max(source.end_date) end_date
    from (
        select post_id, start_date, end_date from v1_erd_test.physical_details
        union all
        select post_id, start_date, end_date from v1_erd_test.remote_details
    ) source
    group by source.post_id
)
select
    count(*) filter (where participation.completion_status = 'COMPLETED')
        as completed_roles,
    coalesce(sum(evaluation.verified_minutes) filter (
        where participation.completion_status = 'COMPLETED'
    ), 0) as verified_minutes,
    count(certificate.post_id) filter (
        where participation.completion_status = 'COMPLETED'
    ) as certificates,
    count(*) filter (
        where participation.created_at::date >= event_bounds.start_date
    ) as invalid_application_dates,
    count(*) filter (
        where participation.completion_status = 'COMPLETED'
          and participation.completed_at::date < event_bounds.end_date
    ) as completed_before_event_end,
    count(*) filter (
        where participation.completion_status = 'COMPLETED'
          and participation.completed_at > v1_erd_test.volunteer_app_now()
    ) as future_completed_dates
from v1_erd_test.role_participations participation
join event_bounds on event_bounds.post_id = participation.post_id
left join v1_erd_test.volunteer_evaluations evaluation
  on evaluation.post_id = participation.post_id
 and evaluation.role_template_id = participation.role_template_id
 and evaluation.user_id = participation.user_id
left join v1_erd_test.volunteer_certificates certificate
  on certificate.post_id = participation.post_id
 and certificate.role_template_id = participation.role_template_id
 and certificate.user_id = participation.user_id
where participation.user_id = 'USER005';
