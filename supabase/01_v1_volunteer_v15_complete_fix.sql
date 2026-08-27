begin;

-- VolunteerLink must use the real database/device time for production demos.
update v1_erd_test.app_test_clock
set use_test_time = false
where clock_name = 'APP';

-- Replace internal fixture copy with user-facing, realistic opportunity copy.
update v1_erd_test.volunteer_posts
set description = case post_id
    when 'POST001' then 'A community support programme where volunteers welcome participants, coordinate activity stations and assist the organising team with safe event operations.'
    when 'POST002' then 'Support a two-day community volunteer programme through participant registration, team coordination and on-site assistance.'
    when 'POST003' then 'Join a community service weekend focused on practical neighbourhood support, accessible participation and responsible event operations.'
    when 'POST004' then 'Create accessible visual materials that help community organisations communicate services clearly to people with different abilities.'
    when 'POST005' then 'Develop social media visuals for a public-interest campaign, respond to coordinator feedback and prepare final publication-ready assets.'
    when 'POST006' then 'Work with an on-site and remote media team to document community activities and transform verified stories into useful campaign materials.'
    when 'POST007' then 'Support participant registration and activity coordination for a scheduled community programme whose volunteer recruitment has now closed.'
    when 'POST008' then 'A venue preparation programme that was cancelled after an operational change. Existing applicants can still review the event record.'
    when 'POST009' then 'Design a set of accessible community campaign materials, complete two review checkpoints and submit final assets for organisation verification.'
    else description
end,
updated_at = least(coalesce(updated_at, timestamptz '2026-08-27 10:00:00+00'), timestamptz '2026-08-27 10:00:00+00')
where post_id between 'POST001' and 'POST009';

update v1_erd_test.post_roles
set role_notes = case post_id || '|' || role_template_id
    when 'POST001|ROLE001' then 'Support general event operations and assist wherever the coordinator assigns help.'
    when 'POST001|ROLE003' then 'Coordinate support volunteers, confirm task coverage and resolve operational issues.'
    when 'POST001|ROLE004' then 'Welcome participants, support registration and guide guests to the correct area.'
    when 'POST002|ROLE003' then 'Coordinate the support team and maintain clear communication with the event lead.'
    when 'POST002|ROLE004' then 'Support participant check-in and provide accurate directions at the information point.'
    when 'POST002|ROLE006' then 'Supervise registration flow and help the team resolve participant record issues.'
    when 'POST003|ROLE001' then 'Assist with scheduled duties, attendance check-in and community activity support.'
    when 'POST004|ROLE019' then 'Adapt supplied content into accessible visual layouts for the community project.'
    when 'POST005|ROLE020' then 'Create two campaign visuals and revise them using documented coordinator feedback.'
    when 'POST006|ROLE002' then 'Prepare the physical activity space and restore equipment after the session.'
    when 'POST006|ROLE019' then 'Prepare the assigned visual assets and submit editable files before the due date.'
    when 'POST007|ROLE004' then 'Welcome registered volunteers and support orderly participant check-in.'
    when 'POST008|ROLE002' then 'Prepare venue resources and assist the team with setup and restoration duties.'
    when 'POST009|ROLE020' then 'Create two accessible information visuals and complete the final review checklist.'
    else role_notes
end
where post_id between 'POST001' and 'POST009';

update v1_erd_test.schedule_items
set notes = case
    when schedule_type = 'PHYSICAL' then
        'Arrive before the listed start time and check in with the volunteer coordinator.'
    when schedule_type = 'REMOTE' then
        'Complete the assigned deliverable before this checkpoint and follow the coordinator submission instructions.'
    when schedule_type = 'TRAINING' then
        'Accepted volunteers will receive the briefing access details from the organisation.'
    else notes
end
where post_id between 'POST001' and 'POST009'
  and (
      notes ilike '%fixture%'
      or notes ilike '%test%'
      or notes is null
  );

-- Correct misleading or inconsistent map locations.
update v1_erd_test.physical_details
set latitude = 5.4377,
    longitude = 100.2905
where post_id = 'POST007';

update v1_erd_test.physical_details
set location_name = 'TAR UMT Penang Branch',
    location_address = '77, Lorong Lembah Permai 3, 11200 Tanjung Bungah, Penang',
    latitude = 5.45235,
    longitude = 100.28508
where post_id in ('POST001', 'DEMO_POST_020');

update v1_erd_test.physical_details
set location_name = 'Queensbay Mall',
    location_address = '100, Persiaran Bayan Indah, 11900 Bayan Lepas, Penang',
    latitude = 5.3336,
    longitude = 100.3065
where post_id = 'DEMO_POST_005';

update v1_erd_test.schedule_items
set location = 'Queensbay Mall'
where post_id = 'DEMO_POST_005'
  and schedule_type = 'PHYSICAL';

-- Five coherent USER005 completions for the assessed demonstration.
-- The three physical PATH002 roles total 900 verified minutes and unlock
-- Level 2. The two Remote PATH005 assignments count as completed roles but
-- intentionally do not contribute service hours.
insert into v1_erd_test.volunteer_posts (
    post_id, organisation_id, title, description, mode, status,
    created_at, published_at, updated_at, category
) values
(
    'HISTORY_POST_001', 'ORG0002', 'Community Food Packing Day',
    'Volunteers organised household food packs, coordinated packing stations and checked each allocation before community collection.',
    'PHYSICAL', 'COMPLETED',
    timestamptz '2026-04-27 02:00:00+00',
    timestamptz '2026-04-28 02:00:00+00',
    timestamptz '2026-05-10 07:00:00+00', 'COMMUNITY'
),
(
    'HISTORY_POST_002', 'ORG0001', 'Inclusive Community Activity Day',
    'Volunteers facilitated accessible small-group activities and helped participants take part safely and confidently.',
    'PHYSICAL', 'COMPLETED',
    timestamptz '2026-05-25 02:00:00+00',
    timestamptz '2026-05-26 02:00:00+00',
    timestamptz '2026-06-14 08:00:00+00', 'COMMUNITY'
),
(
    'HISTORY_POST_003', 'ORG0001', 'Neighbourhood Digital Help Session',
    'Volunteers guided residents through supervised digital-service exercises and recorded questions requiring follow-up support.',
    'PHYSICAL', 'COMPLETED',
    timestamptz '2026-06-27 02:00:00+00',
    timestamptz '2026-06-28 02:00:00+00',
    timestamptz '2026-07-18 08:00:00+00', 'EDUCATION'
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
    'HISTORY_POST_001', date '2026-05-10', date '2026-05-10',
    time '09:00', time '13:00',
    'Dewan Orang Ramai Butterworth',
    'Jalan Raja Uda, 12300 Butterworth, Penang',
    'Penang', 'Malaysia', 5.4147, 100.3742,
    'Main hall registration counter', 18, 'Asia/Kuala_Lumpur'
),
(
    'HISTORY_POST_002', date '2026-06-14', date '2026-06-14',
    time '09:00', time '14:00',
    'Pusat Komuniti Seberang Jaya',
    '13700 Perai, Penang',
    'Penang', 'Malaysia', 5.3979, 100.4025,
    'Activity room entrance', 16, 'Asia/Kuala_Lumpur'
),
(
    'HISTORY_POST_003', date '2026-07-18', date '2026-07-18',
    time '09:00', time '15:00',
    'George Town Community Learning Hub',
    'Lebuh Acheh, 10200 George Town, Penang',
    'Penang', 'Malaysia', 5.4144, 100.3296,
    'Ground-floor help desk', 15, 'Asia/Kuala_Lumpur'
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
) values
(
    'HISTORY_POST_001', 'ROLE014', 8, 'REVIEW_APPLICANTS',
    'Coordinate packing groups and complete the allocation checklist.', null
),
(
    'HISTORY_POST_002', 'ROLE014', 8, 'REVIEW_APPLICANTS',
    'Facilitate an assigned activity group and record participation notes.', null
),
(
    'HISTORY_POST_003', 'ROLE014', 6, 'REVIEW_APPLICANTS',
    'Guide supervised device practice and escalate unresolved questions.', null
)
on conflict (post_id, role_template_id) do update set
    capacity = excluded.capacity,
    application_method = excluded.application_method,
    role_notes = excluded.role_notes;

update v1_erd_test.volunteer_posts
set title = case post_id
        when 'POST005' then 'Community Recycling Visual Campaign'
        when 'POST009' then 'Accessible Community Information Design'
        else title
    end,
    description = case post_id
        when 'POST005' then 'Create clear digital visuals that explain neighbourhood recycling steps and revise the final assets using coordinator feedback.'
        when 'POST009' then 'Design accessible public-information materials, complete documented review checkpoints and submit publication-ready assets for approval.'
        else description
    end,
    status = case when post_id in ('POST005', 'POST009') then 'COMPLETED' else status end,
    updated_at = case
        when post_id = 'POST005' then timestamptz '2026-08-16 10:00:00+00'
        when post_id = 'POST009' then timestamptz '2026-08-25 10:00:00+00'
        else updated_at
    end
where post_id in ('POST005', 'POST009');

update v1_erd_test.remote_details
set start_date = case
        when post_id = 'POST005' then date '2026-08-05'
        when post_id = 'POST009' then date '2026-08-20'
        else start_date
    end,
    end_date = case
        when post_id = 'POST005' then date '2026-08-16'
        when post_id = 'POST009' then date '2026-08-25'
        else end_date
    end
where post_id in ('POST005', 'POST009');

insert into v1_erd_test.role_participations (
    post_id, role_template_id, user_id,
    application_status, completion_status, auto_completed,
    joined_at, completed_at, created_at, decision_note, is_shortlisted
) values
(
    'HISTORY_POST_001', 'ROLE014', 'USER005',
    'ACCEPTED', 'COMPLETED', false,
    timestamptz '2026-05-05 03:00:00+00',
    timestamptz '2026-05-10 06:30:00+00',
    timestamptz '2026-05-03 03:00:00+00',
    'Attendance and assigned activity records were verified by the coordinator.',
    false
),
(
    'HISTORY_POST_002', 'ROLE014', 'USER005',
    'ACCEPTED', 'COMPLETED', false,
    timestamptz '2026-06-05 04:00:00+00',
    timestamptz '2026-06-14 07:30:00+00',
    timestamptz '2026-06-02 04:00:00+00',
    'The coordinator verified the completed facilitation role and service time.',
    false
),
(
    'HISTORY_POST_003', 'ROLE014', 'USER005',
    'ACCEPTED', 'COMPLETED', false,
    timestamptz '2026-07-09 02:30:00+00',
    timestamptz '2026-07-18 07:30:00+00',
    timestamptz '2026-07-06 02:30:00+00',
    'The completed participant-support duties were reviewed and verified.',
    false
)
on conflict (post_id, role_template_id, user_id) do update set
    application_status = excluded.application_status,
    completion_status = excluded.completion_status,
    joined_at = excluded.joined_at,
    completed_at = excluded.completed_at,
    created_at = excluded.created_at,
    cancelled_at = null,
    decision_note = excluded.decision_note;

update v1_erd_test.role_participations
set application_status = 'ACCEPTED',
    completion_status = 'COMPLETED',
    joined_at = case
        when post_id = 'POST005' then timestamptz '2026-08-04 02:00:00+00'
        when post_id = 'POST009' then timestamptz '2026-08-20 02:00:00+00'
    end,
    completed_at = case
        when post_id = 'POST005' then timestamptz '2026-08-16 09:30:00+00'
        when post_id = 'POST009' then timestamptz '2026-08-25 09:30:00+00'
    end,
    created_at = case
        when post_id = 'POST005' then timestamptz '2026-08-02 03:25:00+00'
        when post_id = 'POST009' then timestamptz '2026-08-18 02:00:00+00'
    end,
    cancelled_at = null,
    decision_note = 'The organisation reviewed the submitted assignment and verified this role as completed.'
where user_id = 'USER005'
  and (
      (post_id = 'POST005' and role_template_id = 'ROLE020')
      or (post_id = 'POST009' and role_template_id = 'ROLE020')
  );

insert into v1_erd_test.volunteer_evaluations (
    post_id, role_template_id, user_id, organisation_id,
    rating, feedback, created_at, verified_minutes
) values
(
    'HISTORY_POST_001', 'ROLE014', 'USER005', 'ORG0002', 5,
    'Worked cooperatively, kept the packing station organised and completed the allocation checks.',
    timestamptz '2026-05-10 06:30:00+00', 240
),
(
    'HISTORY_POST_002', 'ROLE014', 'USER005', 'ORG0001', 5,
    'Facilitated the assigned group clearly and supported inclusive participation throughout the session.',
    timestamptz '2026-06-14 07:30:00+00', 300
),
(
    'HISTORY_POST_003', 'ROLE014', 'USER005', 'ORG0001', 4,
    'Guided participants patiently and documented questions that required follow-up.',
    timestamptz '2026-07-18 07:30:00+00', 360
),
(
    'POST005', 'ROLE020', 'USER005', 'ORG0001', 5,
    'Produced clear campaign visuals and responded carefully to the coordinator review.',
    timestamptz '2026-08-16 09:30:00+00', 0
),
(
    'POST009', 'ROLE020', 'USER005', 'ORG0001', 5,
    'Completed the assigned design work reliably and communicated clearly.',
    timestamptz '2026-08-25 09:30:00+00', 0
)
on conflict (post_id, role_template_id, user_id) do update set
    organisation_id = excluded.organisation_id,
    rating = excluded.rating,
    feedback = excluded.feedback,
    created_at = excluded.created_at,
    verified_minutes = excluded.verified_minutes;

insert into v1_erd_test.volunteer_certificates (
    post_id, role_template_id, user_id, certificate_path, issued_at
) values
('HISTORY_POST_001', 'ROLE014', 'USER005', null, timestamptz '2026-05-10 06:30:00+00'),
('HISTORY_POST_002', 'ROLE014', 'USER005', null, timestamptz '2026-06-14 07:30:00+00'),
('HISTORY_POST_003', 'ROLE014', 'USER005', null, timestamptz '2026-07-18 07:30:00+00'),
('POST005', 'ROLE020', 'USER005', null, timestamptz '2026-08-16 09:30:00+00'),
('POST009', 'ROLE020', 'USER005', null, timestamptz '2026-08-25 09:30:00+00')
on conflict (post_id, role_template_id, user_id) do update set
    issued_at = excluded.issued_at;

insert into v1_erd_test.volunteer_skill_path_progress (
    user_id, skill_path_id, current_level,
    verified_assignments, verified_minutes, updated_at
) values
('USER005', 'PATH002', 2, 3, 900, timestamptz '2026-08-27 10:00:00+00'),
('USER005', 'PATH005', 1, 2, null, timestamptz '2026-08-27 10:00:00+00')
on conflict (user_id, skill_path_id) do update set
    current_level = excluded.current_level,
    verified_assignments = excluded.verified_assignments,
    verified_minutes = excluded.verified_minutes,
    updated_at = excluded.updated_at;

-- A completed Remote project cannot have a future final review/completion.
update v1_erd_test.schedule_items
set schedule_date = date '2026-08-25',
    title = 'Final asset submission and coordinator review',
    notes = 'Submit the completed visual assets for organisation review and final approval.'
where schedule_item_id = 'SCH025';

update v1_erd_test.role_participations
set created_at = case
        when post_id = 'DEMO_POST_003' then timestamptz '2026-08-06 02:15:00+00'
        when post_id = 'DEMO_POST_005' then timestamptz '2026-08-10 05:20:00+00'
        when post_id = 'DEMO_POST_008' then timestamptz '2026-08-14 03:10:00+00'
        when post_id = 'DEMO_POST_018' then timestamptz '2026-08-17 06:35:00+00'
        when post_id = 'DEMO_POST_019' then timestamptz '2026-08-21 04:45:00+00'
        when post_id = 'POST002' then timestamptz '2026-08-11 01:30:00+00'
        when post_id = 'POST003' then timestamptz '2026-08-16 02:00:00+00'
        when post_id = 'POST004' then timestamptz '2026-08-18 07:10:00+00'
        when post_id = 'POST005' then timestamptz '2026-08-02 03:25:00+00'
        when post_id = 'POST006' then timestamptz '2026-08-22 06:00:00+00'
        when post_id = 'POST009' then timestamptz '2026-08-18 02:00:00+00'
        else created_at
    end,
    joined_at = case
        when application_status = 'ACCEPTED' and post_id = 'DEMO_POST_003' then timestamptz '2026-08-06 02:15:00+00'
        when application_status = 'ACCEPTED' and post_id = 'DEMO_POST_008' then timestamptz '2026-08-14 03:10:00+00'
        when application_status = 'ACCEPTED' and post_id = 'DEMO_POST_018' then timestamptz '2026-08-17 06:35:00+00'
        when application_status = 'ACCEPTED' and post_id = 'POST003' then timestamptz '2026-08-16 02:00:00+00'
        when application_status = 'ACCEPTED' and post_id = 'POST004' then timestamptz '2026-08-18 07:10:00+00'
        when application_status = 'ACCEPTED' and post_id = 'POST005' then timestamptz '2026-08-04 02:00:00+00'
        when application_status = 'ACCEPTED' and post_id = 'POST006' then timestamptz '2026-08-22 06:00:00+00'
        when post_id = 'POST009' then timestamptz '2026-08-20 02:00:00+00'
        else joined_at
    end,
    completed_at = case
        when post_id = 'POST009' then timestamptz '2026-08-25 09:30:00+00'
        else completed_at
    end,
    cancelled_at = case
        when application_status = 'CANCELLED' and cancelled_at > timestamptz '2026-08-27 15:59:59+00'
            then created_at + interval '2 days'
        else cancelled_at
    end,
    decision_note = case
        when application_status = 'DECLINED' then
            'The organisation selected applicants whose current experience more closely matched this role.'
        when completion_status = 'COMPLETED' then
            'The organisation reviewed the submitted work and verified this role as completed.'
        when decision_note ilike '%fixture%' or decision_note ilike '%test%' then null
        else decision_note
    end
where user_id = 'USER005';

-- Keep the progress refresh timestamp truthful even before the next app refresh.
update v1_erd_test.volunteer_skill_path_progress
set updated_at = timestamptz '2026-08-27 10:00:00+00'
where user_id = 'USER005'
  and updated_at > timestamptz '2026-08-27 15:59:59+00';

-- Provide a real, currently testable Remote review application question.
insert into v1_erd_test.post_role_screening_questions (
    post_id, role_template_id, question_no, question_text
) values (
    'DEMO_POST_014',
    'ROLE026',
    1,
    'Describe one idea you would use to help students recognise an online scam.'
)
on conflict (post_id, role_template_id, question_no)
do update set question_text = excluded.question_text;

-- Remote ASSIGNMENTS_ONLY paths create verified completion evidence and
-- certificates, but they do not add to service-hour totals.
create or replace function v1_erd_test.get_my_volunteer_applications()
returns table(
    participation_id text, post_id text, post_role_id text,
    event_title text, organisation_name text, role_title text,
    volunteer_name text, application_status text, completion_status text,
    decision_note text, verified_minutes integer,
    completed_at timestamptz, feedback text, primary_skill_path text,
    practised_skill_names jsonb, event_date text, event_time text,
    event_location text, created_at timestamptz
)
language sql stable security definer
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
             and path.progression_type <> 'ASSIGNMENTS_ONLY'
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
    join v1_erd_test.user_profiles profile on profile.user_id = participation.user_id
    join v1_erd_test.volunteer_posts post on post.post_id = participation.post_id
    join v1_erd_test.organisations organisation on organisation.organisation_id = post.organisation_id
    join v1_erd_test.role_templates template on template.role_template_id = participation.role_template_id
    join v1_erd_test.skill_paths path on path.skill_path_id = template.skill_path_id
    left join v1_erd_test.volunteer_evaluations evaluation
      on evaluation.post_id = participation.post_id
     and evaluation.role_template_id = participation.role_template_id
     and evaluation.user_id = participation.user_id
    left join lateral (
        select min(source.start_date) as start_date,
               max(source.event_time) filter (where source.event_time <> 'Flexible') as event_time,
               max(source.event_location) filter (where source.event_location <> 'Online') as event_location
        from (
            select physical.start_date,
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
                  select 1 from v1_erd_test.post_role_skills configured
                  where configured.post_id = participation.post_id
                    and configured.role_template_id = participation.role_template_id
              )
        ) selected_skill
        join v1_erd_test.skills skill on skill.skill_id = selected_skill.skill_id
    ) skill_data on true
    where profile.auth_user_id = auth.uid()
      and profile.account_type = 'VOLUNTEER'
    order by participation.created_at desc;
$$;

commit;

select
    now() at time zone 'Asia/Kuala_Lumpur' as malaysia_real_time,
    p.post_id,
    p.application_status,
    p.completion_status,
    p.created_at at time zone 'Asia/Kuala_Lumpur' as submitted_malaysia,
    p.joined_at at time zone 'Asia/Kuala_Lumpur' as joined_malaysia,
    p.completed_at at time zone 'Asia/Kuala_Lumpur' as completed_malaysia
from v1_erd_test.role_participations p
where p.user_id = 'USER005'
order by p.created_at;
