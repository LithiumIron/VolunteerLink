-- VolunteerLink V16: isolated Volunteer CRUD and application status support.
-- Does not replace Organisation attendance/review/finalisation functions.

begin;

-- Shared app uses real database/device time. Preserve test_datetime for
-- teammates who need it later; only disable the override.
update v1_erd_test.app_test_clock
set use_test_time = false
where clock_name = 'APP';

create or replace function v1_erd_test.get_my_volunteer_applications_v2()
returns table(
    participation_id text, post_id text, post_role_id text,
    event_title text, organisation_name text, role_title text,
    volunteer_name text, application_status text, completion_status text,
    decision_note text, verified_minutes integer, completed_at timestamptz,
    feedback text, primary_skill_path text, practised_skill_names jsonb,
    event_date text, event_time text, event_location text, created_at timestamptz,
    completion_reason text, screening_answers jsonb
)
language sql stable security definer
set search_path = v1_erd_test, pg_temp
as $$
    select
        application.participation_id, application.post_id,
        application.post_role_id, application.event_title,
        application.organisation_name, application.role_title,
        application.volunteer_name, application.application_status,
        application.completion_status, application.decision_note,
        application.verified_minutes, application.completed_at,
        application.feedback, application.primary_skill_path,
        application.practised_skill_names, application.event_date,
        application.event_time, application.event_location,
        application.created_at, evaluation.completion_reason,
        coalesce(answer_data.answers, '[]'::jsonb)
    from v1_erd_test.get_my_volunteer_applications() application
    left join v1_erd_test.user_profiles profile
      on profile.auth_user_id = auth.uid()
     and profile.account_type = 'VOLUNTEER'
    left join v1_erd_test.volunteer_evaluations evaluation
      on evaluation.post_id = application.post_id
     and evaluation.role_template_id = split_part(application.post_role_id, '|', 2)
     and evaluation.user_id = profile.user_id
    left join lateral (
        select jsonb_agg(
            jsonb_build_object(
                'question_no', answer.question_no,
                'question_text', answer.question_text,
                'answer_text', answer.answer_text
            ) order by answer.question_no
        ) as answers
        from v1_erd_test.role_participation_screening_answers answer
        where answer.post_id = application.post_id
          and answer.role_template_id = split_part(application.post_role_id, '|', 2)
          and answer.user_id = profile.user_id
    ) answer_data on true;
$$;

create or replace function v1_erd_test.volunteer_update_pending_application_answers(
    target_participation_id text,
    provided_screening_answers jsonb
) returns jsonb
language plpgsql security definer
set search_path = v1_erd_test, pg_temp
as $$
declare
    volunteer_id text;
    selected_post_id text := split_part(target_participation_id, '|', 1);
    selected_role_id text := split_part(target_participation_id, '|', 2);
    question_count integer;
    event_start date;
begin
    if auth.uid() is null then
        raise exception 'Authentication is required.' using errcode = '28000';
    end if;
    select user_id into volunteer_id from v1_erd_test.user_profiles
    where auth_user_id = auth.uid() and account_type = 'VOLUNTEER' limit 1;

    if not exists (
        select 1 from v1_erd_test.role_participations
        where post_id = selected_post_id and role_template_id = selected_role_id
          and user_id = volunteer_id and application_status = 'PENDING'
          and completion_status = 'IN_PROGRESS'
    ) then raise exception 'Only a pending application can be edited.'; end if;

    select min(start_date) into event_start from (
        select start_date from v1_erd_test.physical_details where post_id = selected_post_id
        union all
        select start_date from v1_erd_test.remote_details where post_id = selected_post_id
    ) dates;
    if event_start is null or v1_erd_test.volunteer_app_now()::date >= event_start then
        raise exception 'This application can no longer be edited.';
    end if;

    select count(*)::integer into question_count
    from v1_erd_test.post_role_screening_questions
    where post_id = selected_post_id and role_template_id = selected_role_id;
    if jsonb_typeof(provided_screening_answers) <> 'array'
       or jsonb_array_length(provided_screening_answers) <> question_count
       or exists (select 1 from jsonb_array_elements_text(provided_screening_answers) value
                  where btrim(value) = '') then
        raise exception 'Every screening question must be answered.';
    end if;

    update v1_erd_test.role_participation_screening_answers answer
    set answer_text = supplied.value
    from v1_erd_test.post_role_screening_questions question
    join lateral jsonb_array_elements_text(provided_screening_answers)
        with ordinality supplied(value, answer_no)
      on supplied.answer_no = question.question_no
    where answer.post_id = selected_post_id
      and answer.role_template_id = selected_role_id
      and answer.user_id = volunteer_id
      and question.post_id = answer.post_id
      and question.role_template_id = answer.role_template_id
      and question.question_no = answer.question_no;

    return jsonb_build_object('updated', true);
end;
$$;

create or replace function v1_erd_test.volunteer_cancel_application_v2(
    target_participation_id text,
    cancellation_reason text,
    cancellation_details text default ''
) returns jsonb
language plpgsql security definer
set search_path = v1_erd_test, pg_temp
as $$
declare
    volunteer_id text;
    selected_post_id text := split_part(target_participation_id, '|', 1);
    selected_role_id text := split_part(target_participation_id, '|', 2);
    event_start date;
    current_status text;
    affected integer;
    note text;
begin
    if auth.uid() is null then
        raise exception 'Authentication is required.' using errcode = '28000';
    end if;
    if btrim(coalesce(cancellation_reason, '')) = '' then
        raise exception 'A cancellation reason is required.';
    end if;
    if cancellation_reason = 'Other' and btrim(coalesce(cancellation_details, '')) = '' then
        raise exception 'Please provide cancellation details.';
    end if;
    select user_id into volunteer_id from v1_erd_test.user_profiles
    where auth_user_id = auth.uid() and account_type = 'VOLUNTEER' limit 1;
    select application_status into current_status
    from v1_erd_test.role_participations
    where post_id = selected_post_id and role_template_id = selected_role_id
      and user_id = volunteer_id;
    if current_status not in ('PENDING', 'ACCEPTED') then
        raise exception 'This application cannot be cancelled.';
    end if;
    select min(start_date) into event_start from (
        select start_date from v1_erd_test.physical_details where post_id = selected_post_id
        union all
        select start_date from v1_erd_test.remote_details where post_id = selected_post_id
    ) dates;
    if event_start is null or v1_erd_test.volunteer_app_now()::date >= event_start then
        raise exception 'This role has started and can no longer be cancelled.';
    end if;
    note := 'Cancelled by volunteer: ' || btrim(cancellation_reason) || '.';
    if btrim(coalesce(cancellation_details, '')) <> '' then
        note := note || ' ' || btrim(cancellation_details);
    end if;
    update v1_erd_test.role_participations
    set application_status = 'CANCELLED', cancelled_at = v1_erd_test.volunteer_app_now(),
        decision_note = note, is_shortlisted = false
    where post_id = selected_post_id and role_template_id = selected_role_id
      and user_id = volunteer_id and application_status in ('PENDING', 'ACCEPTED')
      and completion_status = 'IN_PROGRESS';
    get diagnostics affected = row_count;
    if affected <> 1 then raise exception 'This application cannot be cancelled.'; end if;
    return jsonb_build_object('cancelled', true, 'reason', note);
end;
$$;

create or replace function v1_erd_test.volunteer_delete_application(
    target_participation_id text
) returns jsonb
language plpgsql security definer
set search_path = v1_erd_test, pg_temp
as $$
declare
    volunteer_id text;
    selected_post_id text := split_part(target_participation_id, '|', 1);
    selected_role_id text := split_part(target_participation_id, '|', 2);
    affected integer;
begin
    if auth.uid() is null then raise exception 'Authentication is required.'; end if;
    select user_id into volunteer_id from v1_erd_test.user_profiles
    where auth_user_id = auth.uid() and account_type = 'VOLUNTEER' limit 1;
    delete from v1_erd_test.role_participations
    where post_id = selected_post_id and role_template_id = selected_role_id
      and user_id = volunteer_id and application_status in ('CANCELLED', 'DECLINED')
      and completion_status = 'IN_PROGRESS';
    get diagnostics affected = row_count;
    if affected <> 1 then raise exception 'Only a cancelled or rejected application can be deleted.'; end if;
    return jsonb_build_object('deleted', true);
end;
$$;

create or replace function v1_erd_test.volunteer_reapply_for_role(
    target_post_role_id text,
    provided_screening_answers jsonb default '[]'::jsonb
) returns jsonb
language plpgsql security definer
set search_path = v1_erd_test, pg_temp
as $$
declare
    volunteer_id text;
    selected_post_id text := split_part(target_post_role_id, '|', 1);
    selected_role_id text := split_part(target_post_role_id, '|', 2);
begin
    if auth.uid() is null then raise exception 'Authentication is required.'; end if;
    select user_id into volunteer_id from v1_erd_test.user_profiles
    where auth_user_id = auth.uid() and account_type = 'VOLUNTEER' limit 1;
    if not exists (
        select 1 from v1_erd_test.role_participations
        where post_id = selected_post_id and role_template_id = selected_role_id
          and user_id = volunteer_id and application_status in ('CANCELLED', 'DECLINED')
          and completion_status = 'IN_PROGRESS'
    ) then raise exception 'Only a cancelled or rejected application can be submitted again.'; end if;
    delete from v1_erd_test.role_participations
    where post_id = selected_post_id and role_template_id = selected_role_id
      and user_id = volunteer_id and application_status in ('CANCELLED', 'DECLINED')
      and completion_status = 'IN_PROGRESS';
    return v1_erd_test.submit_role_application(target_post_role_id, provided_screening_answers);
end;
$$;

grant execute on function v1_erd_test.get_my_volunteer_applications_v2() to authenticated;
grant execute on function v1_erd_test.volunteer_update_pending_application_answers(text, jsonb) to authenticated;
grant execute on function v1_erd_test.volunteer_cancel_application_v2(text, text, text) to authenticated;
grant execute on function v1_erd_test.volunteer_delete_application(text) to authenticated;
grant execute on function v1_erd_test.volunteer_reapply_for_role(text, jsonb) to authenticated;

commit;

