-- VolunteerLink V16.1 USER005 demonstration fixture and narrow timeline repair.
-- This file intentionally does not touch POST012 or any Organisation workflow.

begin;

-- Repair only three previously confirmed impossible acceptance timestamps.
-- No post content, schedule, attendance, evaluation or completion is changed.
update v1_erd_test.role_participations
set joined_at = created_at,
    decision_note = case
        when post_id = 'POST006' then
            'Accepted for the Remote phase and later cancelled before it began.'
        else decision_note
    end
where user_id = 'USER005'
  and post_id in ('POST006', 'POST011')
  and joined_at is not null
  and joined_at > v1_erd_test.volunteer_app_now();

update v1_erd_test.role_participations
set created_at = cancelled_at - interval '5 minutes',
    joined_at = cancelled_at - interval '5 minutes'
where user_id = 'USER005'
  and post_id = 'DEMO_POST_009'
  and cancelled_at is not null
  and (created_at > cancelled_at or joined_at > cancelled_at);

-- Independent real-case history. It demonstrates an accepted volunteer who
-- was reviewed as NOT_COMPLETED due to missing attendance. It awards 0 minutes,
-- creates no certificate and creates no skill experience.
insert into v1_erd_test.volunteer_posts (
    post_id, organisation_id, title, description, mode, status,
    created_at, published_at, updated_at, category
) values (
    'HISTORY_POST_004', 'ORG0002', 'Community Pantry Volunteer Support Day',
    'Volunteers prepared the community pantry, checked supply quantities and supported an orderly collection session for local households.',
    'PHYSICAL', 'COMPLETED',
    timestamptz '2026-07-25 02:00:00+00',
    timestamptz '2026-07-26 02:00:00+00',
    timestamptz '2026-08-09 06:30:00+00', 'COMMUNITY'
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
) values (
    'HISTORY_POST_004', date '2026-08-09', date '2026-08-09',
    time '09:00', time '13:00',
    'Pusat Komuniti Seberang Jaya', '13700 Perai, Penang',
    'Penang', 'Malaysia', 5.3979, 100.4025,
    'Community hall registration desk', 12, 'Asia/Kuala_Lumpur'
)
on conflict (post_id) do update set
    start_date = excluded.start_date,
    end_date = excluded.end_date,
    start_time = excluded.start_time,
    end_time = excluded.end_time,
    location_name = excluded.location_name,
    location_address = excluded.location_address,
    latitude = excluded.latitude,
    longitude = excluded.longitude,
    meeting_point = excluded.meeting_point;

insert into v1_erd_test.post_roles (
    post_id, role_template_id, capacity, application_method,
    role_notes, individual_submission_requirement
) values (
    'HISTORY_POST_004', 'ROLE007', 6, 'REVIEW_APPLICANTS',
    'Prepare assigned pantry supplies, check quantities and assist the coordinator during collection.',
    null
)
on conflict (post_id, role_template_id) do update set
    capacity = excluded.capacity,
    application_method = excluded.application_method,
    role_notes = excluded.role_notes;

insert into v1_erd_test.post_role_skills (
    post_id, role_template_id, skill_id, required_experience
)
select 'HISTORY_POST_004', 'ROLE007', template.skill_id, null::integer
from v1_erd_test.role_template_skills template
where template.role_template_id = 'ROLE007'
on conflict (post_id, role_template_id, skill_id) do update set
    required_experience = excluded.required_experience;

insert into v1_erd_test.schedule_items (
    schedule_item_id, post_id, schedule_type, schedule_date,
    title, start_time, end_time, location, notes
) values (
    'HIST004_SCH001', 'HISTORY_POST_004', 'PHYSICAL', date '2026-08-09',
    'Pantry preparation and community collection support',
    time '09:00', time '13:00', 'Pusat Komuniti Seberang Jaya',
    'Check in at the registration desk before joining the assigned supply station.'
)
on conflict (schedule_item_id) do update set
    schedule_date = excluded.schedule_date,
    title = excluded.title,
    start_time = excluded.start_time,
    end_time = excluded.end_time,
    location = excluded.location,
    notes = excluded.notes;

insert into v1_erd_test.schedule_item_roles (
    schedule_item_id, post_id, role_template_id
) values ('HIST004_SCH001', 'HISTORY_POST_004', 'ROLE007')
on conflict do nothing;

insert into v1_erd_test.role_participations (
    post_id, role_template_id, user_id,
    application_status, completion_status, auto_completed,
    joined_at, completed_at, created_at, decision_note, is_shortlisted
) values (
    'HISTORY_POST_004', 'ROLE007', 'USER005',
    'ACCEPTED', 'NOT_COMPLETED', false,
    timestamptz '2026-08-01 03:00:00+00',
    timestamptz '2026-08-09 06:30:00+00',
    timestamptz '2026-07-29 03:00:00+00',
    'Attendance was not recorded for the scheduled activity, so the organisation could not verify completion.',
    false
)
on conflict (post_id, role_template_id, user_id) do update set
    application_status = excluded.application_status,
    completion_status = excluded.completion_status,
    auto_completed = excluded.auto_completed,
    joined_at = excluded.joined_at,
    completed_at = excluded.completed_at,
    created_at = excluded.created_at,
    cancelled_at = null,
    decision_note = excluded.decision_note,
    is_shortlisted = excluded.is_shortlisted;

insert into v1_erd_test.volunteer_evaluations (
    post_id, role_template_id, user_id, organisation_id,
    rating, feedback, completion_reason, verified_minutes, created_at
) values (
    'HISTORY_POST_004', 'ROLE007', 'USER005', 'ORG0002',
    null, null,
    'Attendance was not recorded for the scheduled activity, so the organisation could not verify completion.',
    0, timestamptz '2026-08-09 06:30:00+00'
)
on conflict (post_id, role_template_id, user_id) do update set
    organisation_id = excluded.organisation_id,
    rating = excluded.rating,
    feedback = excluded.feedback,
    completion_reason = excluded.completion_reason,
    verified_minutes = excluded.verified_minutes,
    created_at = excluded.created_at;

-- Enforce the assessment rule for this one fixture only: NOT_COMPLETED earns
-- neither a certificate nor verified skill experience.
delete from v1_erd_test.volunteer_certificates
where post_id = 'HISTORY_POST_004'
  and role_template_id = 'ROLE007'
  and user_id = 'USER005';

delete from v1_erd_test.volunteer_skill_experiences
where post_id = 'HISTORY_POST_004'
  and role_template_id = 'ROLE007'
  and user_id = 'USER005';

commit;

-- Expected: one NOT_COMPLETED row, 0 verified minutes and no reward records.
select participation.post_id, participation.application_status,
       participation.completion_status, participation.decision_note,
       evaluation.verified_minutes,
       exists (
           select 1 from v1_erd_test.volunteer_certificates certificate
           where certificate.post_id = participation.post_id
             and certificate.role_template_id = participation.role_template_id
             and certificate.user_id = participation.user_id
       ) as has_certificate,
       exists (
           select 1 from v1_erd_test.volunteer_skill_experiences experience
           where experience.post_id = participation.post_id
             and experience.role_template_id = participation.role_template_id
             and experience.user_id = participation.user_id
       ) as has_skill_experience
from v1_erd_test.role_participations participation
left join v1_erd_test.volunteer_evaluations evaluation
  on evaluation.post_id = participation.post_id
 and evaluation.role_template_id = participation.role_template_id
 and evaluation.user_id = participation.user_id
where participation.post_id = 'HISTORY_POST_004'
  and participation.user_id = 'USER005';
