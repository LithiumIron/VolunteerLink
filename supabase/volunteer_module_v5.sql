-- VolunteerLink volunteer opportunity + Skill Path demo integration (v5)
-- Safe to run more than once in Supabase SQL Editor.
-- This script does not delete team data. It upserts only the explicit demo IDs
-- POST904..POST910, PROLE905..PROLE916 and their matching demo records.

begin;

-- Columns used by the volunteer UI and application outcome details.
alter table public.volunteer_posts
    add column if not exists category text,
    add column if not exists cause_name text,
    add column if not exists is_government_approved boolean not null default false;

alter table public.post_roles
    add column if not exists experience_requirement text,
    add column if not exists specific_assignment text,
    add column if not exists training_details text,
    add column if not exists minimum_skill_path_level integer not null default 1;

alter table public.role_participations
    add column if not exists decision_note text;

do $$
declare
    green_org_id text;
    community_org_id text;
    digital_org_id text;
    fallback_org_id text;
    demo_user_id text;
begin
    select o.organisation_id
    into fallback_org_id
    from public.organisations o
    order by o.organisation_id
    limit 1;

    if fallback_org_id is null then
        raise exception 'No organisation exists. Keep the team organisation seed and run this v5 script again.';
    end if;

    select coalesce(
        (
            select o.organisation_id
            from public.organisations o
            where o.organisation_name = 'Green Earth Society'
            limit 1
        ),
        fallback_org_id
    ) into green_org_id;

    select coalesce(
        (
            select o.organisation_id
            from public.organisations o
            where o.organisation_name = 'Community Food Support'
            limit 1
        ),
        fallback_org_id
    ) into community_org_id;

    select coalesce(
        (
            select o.organisation_id
            from public.organisations o
            where o.organisation_name = 'DigitalCare Foundation'
            limit 1
        ),
        fallback_org_id
    ) into digital_org_id;

    select up.user_id
    into demo_user_id
    from public.user_profiles up
    join auth.users au
      on au.id = up.auth_user_id
    where lower(au.email) = 'volunteer.login.2026@example.com'
      and up.account_type = 'VOLUNTEER'
    limit 1;

    if demo_user_id is null then
        select up.user_id
        into demo_user_id
        from public.user_profiles up
        where up.account_type = 'VOLUNTEER'
        order by up.user_id
        limit 1;
    end if;

    if demo_user_id is null then
        raise exception 'No volunteer profile exists. Sign in once with the demo volunteer, link its auth UID, then rerun this script.';
    end if;

    -- Two historical events exist only in application history/certificates.
    insert into public.volunteer_posts (
        post_id, organisation_id, title, description, mode, status,
        category, cause_name, is_government_approved, published_at
    ) values
        (
            'POST904', green_org_id,
            'Community Library Sorting Day',
            'Volunteers organised donated books, prepared reading corners and labelled resources for neighbourhood families.',
            'PHYSICAL', 'COMPLETED', 'EDUCATION',
            'Community Learning and Literacy', true,
            '2026-07-20 09:00:00+08'
        ),
        (
            'POST910', community_org_id,
            'Neighbourhood Welcome Desk',
            'A completed community welcome programme where volunteers guided visitors and connected families with local support services.',
            'PHYSICAL', 'COMPLETED', 'COMMUNITY',
            'Inclusive Community Support', false,
            '2026-07-12 09:00:00+08'
        )
    on conflict (post_id) do update set
        organisation_id = excluded.organisation_id,
        title = excluded.title,
        description = excluded.description,
        mode = excluded.mode,
        status = excluded.status,
        category = excluded.category,
        cause_name = excluded.cause_name,
        is_government_approved = excluded.is_government_approved;

    -- Five additional open opportunities. Together with the existing three,
    -- the volunteer dashboard has eight varied, connected opportunities.
    insert into public.volunteer_posts (
        post_id, organisation_id, title, description, mode, status,
        category, cause_name, is_government_approved, published_at
    ) values
        (
            'POST905', green_org_id,
            'Coastal Cleanup & Recycling Drive',
            'Protect Penang''s coastline by collecting litter, sorting recyclable material and helping visitors use safe cleanup zones.',
            'PHYSICAL', 'PUBLISHED', 'ENVIRONMENT',
            'Coastal Conservation and Waste Reduction', true,
            now()
        ),
        (
            'POST906', community_org_id,
            'Community Health Screening Day',
            'Support a free community screening programme by welcoming visitors, managing safe movement and directing families to service counters.',
            'PHYSICAL', 'PUBLISHED', 'HEALTH',
            'Preventive Health and Wellbeing', true,
            now()
        ),
        (
            'POST907', community_org_id,
            'Youth Reading Buddy Weekend',
            'Encourage children to enjoy reading through guided activities, story stations and friendly one-to-one learning support.',
            'PHYSICAL', 'PUBLISHED', 'EDUCATION',
            'Youth Learning and Confidence', false,
            now()
        ),
        (
            'POST908', digital_org_id,
            'Accessible Website Content Review',
            'Review charity website pages, online forms and written guides so that community information is clearer and easier to access.',
            'REMOTE', 'PUBLISHED', 'EDUCATION',
            'Digital Inclusion and Accessibility', false,
            now()
        ),
        (
            'POST909', green_org_id,
            'Animal Shelter Adoption Day',
            'Help welcome prospective adopters, document the event and guide families through a calm, organised adoption-day experience.',
            'PHYSICAL', 'PUBLISHED', 'ANIMALS',
            'Animal Welfare and Responsible Adoption', false,
            now()
        )
    on conflict (post_id) do update set
        organisation_id = excluded.organisation_id,
        title = excluded.title,
        description = excluded.description,
        mode = excluded.mode,
        status = excluded.status,
        category = excluded.category,
        cause_name = excluded.cause_name,
        is_government_approved = excluded.is_government_approved;

    insert into public.physical_details (
        physical_detail_id, post_id, start_date, end_date,
        start_time, end_time, location_name, location_address,
        state_region, country, latitude, longitude, meeting_point,
        volunteer_capacity
    ) values
        (
            'PHY904', 'POST904', '2026-08-02', '2026-08-02',
            '09:00', '14:00', 'Butterworth Community Library',
            'Jalan Bagan Luar, 12000 Butterworth, Penang',
            'Penang', 'Malaysia', 5.3996, 100.3651,
            'Library main entrance', 10
        ),
        (
            'PHY910', 'POST910', '2026-07-26', '2026-07-26',
            '08:30', '13:00', 'Seberang Perai Community Hall',
            'Jalan Perai Jaya, 13600 Perai, Penang',
            'Penang', 'Malaysia', 5.3841, 100.3972,
            'Welcome counter', 8
        ),
        (
            'PHY905', 'POST905', '2026-08-16', '2026-08-16',
            '07:30', '12:30', 'Pantai Bersih',
            'Pantai Bersih, 13000 Butterworth, Penang',
            'Penang', 'Malaysia', 5.4380, 100.3770,
            'Volunteer tent beside the main car park', 16
        ),
        (
            'PHY906', 'POST906', '2026-08-23', '2026-08-23',
            '08:00', '14:00', 'Dewan Masyarakat Butterworth',
            'Jalan Raja Uda, 12300 Butterworth, Penang',
            'Penang', 'Malaysia', 5.4210, 100.3780,
            'Registration lobby', 8
        ),
        (
            'PHY907', 'POST907', '2026-08-30', '2026-08-30',
            '09:00', '15:00', 'Taman Inderawasih Learning Centre',
            'Taman Inderawasih, 13600 Perai, Penang',
            'Penang', 'Malaysia', 5.3850, 100.4010,
            'Ground-floor reading room', 12
        ),
        (
            'PHY909', 'POST909', '2026-09-06', '2026-09-06',
            '09:00', '16:00', 'Penang Animal Welfare Centre',
            'Jalan Sungai Dua, 11700 Gelugor, Penang',
            'Penang', 'Malaysia', 5.3570, 100.3020,
            'Adoption registration tent', 8
        )
    on conflict (post_id) do update set
        start_date = excluded.start_date,
        end_date = excluded.end_date,
        start_time = excluded.start_time,
        end_time = excluded.end_time,
        location_name = excluded.location_name,
        location_address = excluded.location_address,
        state_region = excluded.state_region,
        country = excluded.country,
        latitude = excluded.latitude,
        longitude = excluded.longitude,
        meeting_point = excluded.meeting_point,
        volunteer_capacity = excluded.volunteer_capacity;

    insert into public.remote_details (
        remote_detail_id, post_id, start_date, end_date,
        volunteer_capacity, submission_mode, shared_deliverable
    ) values (
        'REM908', 'POST908', '2026-08-18', '2026-09-05',
        10, 'INDIVIDUAL',
        'Reviewed website pages with accessibility notes and corrected content.'
    )
    on conflict (post_id) do update set
        start_date = excluded.start_date,
        end_date = excluded.end_date,
        volunteer_capacity = excluded.volunteer_capacity,
        submission_mode = excluded.submission_mode,
        shared_deliverable = excluded.shared_deliverable;

    insert into public.post_roles (
        post_role_id, post_id, role_template_id, role_mode, level,
        capacity, application_method, responsibilities,
        practised_skills, required_skill_requirements,
        screening_questions, role_notes,
        experience_requirement, specific_assignment,
        training_details, minimum_skill_path_level
    ) values
        (
            'PROLE905', 'POST904', 'ROLE007', 'PHYSICAL', 'BEGINNER',
            10, 'INSTANT_JOIN',
            '["Sort donated books", "Label shelves", "Prepare reading areas"]'::jsonb,
            '["SKILL011", "SKILL012", "SKILL013"]'::jsonb,
            '[]'::jsonb, '[]'::jsonb,
            'Completed demo role used as verified Skill Path evidence.',
            'No previous experience was required.',
            'Organise donated books, learning materials and reading-area supplies.',
            'A library safety and sorting briefing was provided.', 1
        ),
        (
            'PROLE916', 'POST910', 'ROLE004', 'PHYSICAL', 'BEGINNER',
            8, 'INSTANT_JOIN',
            '["Welcome visitors", "Guide families", "Support check-in"]'::jsonb,
            '["SKILL001", "SKILL003", "SKILL004"]'::jsonb,
            '[]'::jsonb, '[]'::jsonb,
            'Completed demo role used as communication-path evidence.',
            'No previous experience was required.',
            'Welcome community visitors and direct them to the right support counter.',
            'A guest-service briefing was provided.', 1
        ),
        (
            'PROLE906', 'POST905', 'ROLE002', 'PHYSICAL', 'BEGINNER',
            10, 'INSTANT_JOIN',
            '["Prepare cleanup zones", "Sort collected material", "Restore equipment"]'::jsonb,
            '["SKILL011", "SKILL012", "SKILL006"]'::jsonb,
            '[]'::jsonb, '[]'::jsonb, null,
            'No experience is required. Volunteers should be comfortable working outdoors.',
            'Set up cleanup stations and sort collected waste into safe recycling categories.',
            'Gloves, tools and a 20-minute safety briefing are provided.', 1
        ),
        (
            'PROLE907', 'POST905', 'ROLE010', 'PHYSICAL', 'BEGINNER',
            6, 'REVIEW_APPLICANTS',
            '["Guide volunteer teams", "Monitor safe routes", "Report hazards"]'::jsonb,
            '["SKILL015", "SKILL014", "SKILL004"]'::jsonb,
            '[]'::jsonb,
            '["Why are you interested in coastal conservation?"]'::jsonb,
            null, 'No previous marshalling experience is required.',
            'Guide volunteers between the registration, cleanup and recycling zones.',
            'Route and emergency-contact briefing included.', 1
        ),
        (
            'PROLE908', 'POST906', 'ROLE004', 'PHYSICAL', 'BEGINNER',
            5, 'INSTANT_JOIN',
            '["Welcome visitors", "Support registration", "Give directions"]'::jsonb,
            '["SKILL001", "SKILL003", "SKILL004"]'::jsonb,
            '[]'::jsonb, '[]'::jsonb, null,
            'Friendly communication and respect for visitor privacy are important.',
            'Welcome visitors and guide them through registration and screening stations.',
            'Privacy and guest-support briefing provided.', 1
        ),
        (
            'PROLE909', 'POST906', 'ROLE012', 'PHYSICAL', 'INTERMEDIATE',
            3, 'REVIEW_APPLICANTS',
            '["Monitor safe flow", "Coordinate basic safety response", "Support team leads"]'::jsonb,
            '["SKILL015", "SKILL014", "SKILL010"]'::jsonb,
            '["Prior event support experience", "Confident handling visitor concerns"]'::jsonb,
            '["Describe one situation where you helped keep an activity safe."]'::jsonb,
            null, 'Previous event or community programme experience is recommended.',
            'Coordinate safe visitor movement and report operational concerns.',
            'Organisation-led safety briefing required.', 2
        ),
        (
            'PROLE910', 'POST907', 'ROLE014', 'PHYSICAL', 'BEGINNER',
            8, 'INSTANT_JOIN',
            '["Run reading activities", "Encourage participation", "Support children"]'::jsonb,
            '["SKILL008", "SKILL006", "SKILL002"]'::jsonb,
            '[]'::jsonb, '[]'::jsonb, null,
            'No teaching experience is required; patience and clear communication are important.',
            'Support short reading games, story stations and activity transitions.',
            'Reading-activity guide and safeguarding briefing provided.', 1
        ),
        (
            'PROLE911', 'POST907', 'ROLE004', 'PHYSICAL', 'BEGINNER',
            4, 'INSTANT_JOIN',
            '["Welcome families", "Check in participants", "Guide visitors"]'::jsonb,
            '["SKILL001", "SKILL003", "SKILL004"]'::jsonb,
            '[]'::jsonb, '[]'::jsonb, null,
            'No experience is required.',
            'Welcome families and guide children to the correct reading station.',
            'A short check-in briefing is provided.', 1
        ),
        (
            'PROLE912', 'POST908', 'ROLE034', 'REMOTE', 'BEGINNER',
            6, 'REVIEW_APPLICANTS',
            '["Review web pages", "Test online forms", "Document accessibility issues"]'::jsonb,
            '["SKILL036", "SKILL037", "SKILL038"]'::jsonb,
            '[]'::jsonb,
            '["How many hours can you contribute each week?"]'::jsonb,
            null, 'Basic confidence using a web browser and online forms is sufficient.',
            'Test assigned website pages and record clear accessibility observations.',
            'Online checklist, sample report and mentor review are provided.', 1
        ),
        (
            'PROLE913', 'POST908', 'ROLE023', 'REMOTE', 'INTERMEDIATE',
            4, 'REVIEW_APPLICANTS',
            '["Edit public guides", "Improve clarity", "Check inclusive language"]'::jsonb,
            '["SKILL028", "SKILL026", "SKILL035"]'::jsonb,
            '["Previous writing or proofreading sample"]'::jsonb,
            '["Describe your proofreading experience."]'::jsonb,
            null, 'Previous writing, editing or content-review experience is recommended.',
            'Edit website guides so instructions are concise, inclusive and easy to follow.',
            'Style guide and editor feedback are provided.', 2
        ),
        (
            'PROLE914', 'POST909', 'ROLE013', 'PHYSICAL', 'BEGINNER',
            5, 'INSTANT_JOIN',
            '["Welcome adopters", "Share event information", "Support visitor flow"]'::jsonb,
            '["SKILL005", "SKILL002", "SKILL001"]'::jsonb,
            '[]'::jsonb, '[]'::jsonb, null,
            'No animal-handling experience is required because this role supports visitors.',
            'Welcome prospective adopters and guide them to registration and consultation areas.',
            'Visitor support and animal-welfare briefing provided.', 1
        ),
        (
            'PROLE915', 'POST909', 'ROLE016', 'PHYSICAL', 'INTERMEDIATE',
            3, 'REVIEW_APPLICANTS',
            '["Photograph activities", "Organise image files", "Respect consent rules"]'::jsonb,
            '["SKILL016", "SKILL019", "SKILL020"]'::jsonb,
            '["Comfortable using a camera", "Understands photo consent"]'::jsonb,
            '["Share a link to a small photography sample."]'::jsonb,
            null, 'Basic event photography experience and access to a camera are required.',
            'Capture adoption-day moments while following consent and animal-welfare guidance.',
            'Shot list, consent rules and file-delivery guide provided.', 2
        )
    on conflict (post_role_id) do update set
        post_id = excluded.post_id,
        role_template_id = excluded.role_template_id,
        role_mode = excluded.role_mode,
        level = excluded.level,
        capacity = excluded.capacity,
        application_method = excluded.application_method,
        responsibilities = excluded.responsibilities,
        practised_skills = excluded.practised_skills,
        required_skill_requirements = excluded.required_skill_requirements,
        screening_questions = excluded.screening_questions,
        role_notes = excluded.role_notes,
        experience_requirement = excluded.experience_requirement,
        specific_assignment = excluded.specific_assignment,
        training_details = excluded.training_details,
        minimum_skill_path_level = excluded.minimum_skill_path_level;

    insert into public.schedule_items (
        schedule_item_id, post_id, scope, schedule_date, title,
        start_time, end_time, location, target_role_ids, notes
    ) values
        ('SCH920', 'POST905', 'PHYSICAL', '2026-08-16', 'Registration and safety briefing', '07:30', '08:00', 'Volunteer tent', '["PROLE906", "PROLE907"]'::jsonb, 'Bring water and sun protection.'),
        ('SCH921', 'POST905', 'PHYSICAL', '2026-08-16', 'Cleanup and recycling deployment', '08:00', '12:00', 'Pantai Bersih zones', '["PROLE906", "PROLE907"]'::jsonb, null),
        ('SCH922', 'POST906', 'PHYSICAL', '2026-08-23', 'Volunteer briefing', '08:00', '08:30', 'Registration lobby', '["PROLE908", "PROLE909"]'::jsonb, null),
        ('SCH923', 'POST906', 'PHYSICAL', '2026-08-23', 'Community screening support', '08:30', '13:30', 'Screening stations', '["PROLE908", "PROLE909"]'::jsonb, null),
        ('SCH924', 'POST907', 'PHYSICAL', '2026-08-30', 'Safeguarding and activity briefing', '09:00', '09:30', 'Reading room', '["PROLE910", "PROLE911"]'::jsonb, null),
        ('SCH925', 'POST907', 'PHYSICAL', '2026-08-30', 'Reading buddy activities', '09:30', '14:30', 'Learning centre', '["PROLE910", "PROLE911"]'::jsonb, null),
        ('SCH926', 'POST908', 'REMOTE', '2026-08-18', 'Online onboarding and task selection', null, null, 'Online', '["PROLE912", "PROLE913"]'::jsonb, null),
        ('SCH927', 'POST908', 'REMOTE', '2026-09-05', 'Final review and submission', null, null, 'Online', '["PROLE912", "PROLE913"]'::jsonb, null),
        ('SCH928', 'POST909', 'PHYSICAL', '2026-09-06', 'Animal welfare and consent briefing', '09:00', '09:30', 'Adoption tent', '["PROLE914", "PROLE915"]'::jsonb, null),
        ('SCH929', 'POST909', 'PHYSICAL', '2026-09-06', 'Adoption day support', '09:30', '15:30', 'Welfare centre', '["PROLE914", "PROLE915"]'::jsonb, null),
        ('SCH930', 'POST904', 'PHYSICAL', '2026-08-02', 'Library sorting briefing', '09:00', '09:20', 'Library entrance', '["PROLE905"]'::jsonb, null),
        ('SCH931', 'POST910', 'PHYSICAL', '2026-07-26', 'Guest service briefing', '08:30', '09:00', 'Welcome counter', '["PROLE916"]'::jsonb, null)
    on conflict (schedule_item_id) do update set
        post_id = excluded.post_id,
        scope = excluded.scope,
        schedule_date = excluded.schedule_date,
        title = excluded.title,
        start_time = excluded.start_time,
        end_time = excluded.end_time,
        location = excluded.location,
        target_role_ids = excluded.target_role_ids,
        notes = excluded.notes;

    -- Demo outcomes: two completed records and one rejected record. Existing
    -- team-created Pending/Accepted records are intentionally kept unchanged.
    insert into public.role_participations (
        participation_id, post_role_id, user_id,
        application_status, completion_status, screening_answers,
        auto_completed, joined_at, completed_at, created_at, decision_note
    ) values
        (
            'PART905', 'PROLE905', demo_user_id,
            'ACCEPTED', 'COMPLETED', '[]'::jsonb,
            false, '2026-08-02 09:00:00+08',
            '2026-08-02 14:00:00+08',
            '2026-07-25 10:15:00+08', null
        ),
        (
            'PART906', 'PROLE909', demo_user_id,
            'DECLINED', 'NOT_COMPLETED',
            '[{"question":"Describe one situation where you helped keep an activity safe.","answer":"I supported crowd movement during a school event."}]'::jsonb,
            false, null, null,
            '2026-08-11 18:30:00+08',
            'This intermediate safety role requires more verified event-safety experience. Try the Greeter role in the same event to build evidence first.'
        ),
        (
            'PART907', 'PROLE916', demo_user_id,
            'ACCEPTED', 'COMPLETED', '[]'::jsonb,
            false, '2026-07-26 08:30:00+08',
            '2026-07-26 13:00:00+08',
            '2026-07-18 11:00:00+08', null
        )
    on conflict (participation_id) do update set
        post_role_id = excluded.post_role_id,
        user_id = excluded.user_id,
        application_status = excluded.application_status,
        completion_status = excluded.completion_status,
        screening_answers = excluded.screening_answers,
        auto_completed = excluded.auto_completed,
        joined_at = excluded.joined_at,
        completed_at = excluded.completed_at,
        decision_note = excluded.decision_note;

    insert into public.attendance_days (
        attendance_day_id, post_id, event_date,
        pin_code, expected_minutes, is_active
    ) values
        ('ADAY905', 'POST904', '2026-08-02', '8204', 300, false),
        ('ADAY907', 'POST910', '2026-07-26', '7264', 270, false)
    on conflict (attendance_day_id) do update set
        post_id = excluded.post_id,
        event_date = excluded.event_date,
        expected_minutes = excluded.expected_minutes,
        is_active = excluded.is_active;

    insert into public.attendance_records (
        attendance_record_id, attendance_day_id,
        participation_id, checked_in_at, verified_minutes
    ) values
        ('ATT905', 'ADAY905', 'PART905', '2026-08-02 09:00:00+08', 300),
        ('ATT907', 'ADAY907', 'PART907', '2026-07-26 08:30:00+08', 270)
    on conflict (attendance_record_id) do update set
        attendance_day_id = excluded.attendance_day_id,
        participation_id = excluded.participation_id,
        checked_in_at = excluded.checked_in_at,
        verified_minutes = excluded.verified_minutes;

    insert into public.volunteer_evaluations (
        evaluation_id, participation_id, organisation_id,
        rating, feedback
    ) values
        (
            'EVAL905', 'PART905', green_org_id, 5,
            'Reliable and organised. The volunteer completed every sorting task and supported other team members.'
        ),
        (
            'EVAL907', 'PART907', community_org_id, 5,
            'Warm, clear and respectful when guiding visitors. A strong contribution to the welcome team.'
        )
    on conflict (participation_id) do update set
        organisation_id = excluded.organisation_id,
        rating = excluded.rating,
        feedback = excluded.feedback;

    insert into public.volunteer_skill_experiences (
        skill_experience_id, participation_id, skill_id, verified_at
    ) values
        ('EXP950', 'PART905', 'SKILL011', '2026-08-02 14:00:00+08'),
        ('EXP951', 'PART905', 'SKILL012', '2026-08-02 14:00:00+08'),
        ('EXP952', 'PART905', 'SKILL013', '2026-08-02 14:00:00+08'),
        ('EXP953', 'PART907', 'SKILL001', '2026-07-26 13:00:00+08'),
        ('EXP954', 'PART907', 'SKILL003', '2026-07-26 13:00:00+08'),
        ('EXP955', 'PART907', 'SKILL004', '2026-07-26 13:00:00+08')
    on conflict (skill_experience_id) do update set
        participation_id = excluded.participation_id,
        skill_id = excluded.skill_id,
        verified_at = excluded.verified_at;
end;
$$;

-- Reconcile every event-level capacity with the sum of its role capacities.
-- The Android app additionally shows remaining role spots, so the total shown
-- at event level always equals the role values visible below it.
update public.physical_details details
set volunteer_capacity = totals.total_capacity
from (
    select pr.post_id, sum(pr.capacity)::integer as total_capacity
    from public.post_roles pr
    group by pr.post_id
) totals
where details.post_id = totals.post_id
  and details.volunteer_capacity <> totals.total_capacity;

update public.remote_details details
set volunteer_capacity = totals.total_capacity
from (
    select pr.post_id, sum(pr.capacity)::integer as total_capacity
    from public.post_roles pr
    group by pr.post_id
) totals
where details.post_id = totals.post_id
  and details.volunteer_capacity <> totals.total_capacity;

-- Privacy-safe published role metrics. No user IDs or screening answers leak.
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
        pr.post_role_id,
        count(rp.participation_id) filter (
            where rp.application_status not in ('DECLINED', 'CANCELLED')
        )::integer as application_count,
        count(rp.participation_id) filter (
            where rp.application_status = 'ACCEPTED'
        )::integer as accepted_count,
        greatest(
            pr.capacity -
                count(rp.participation_id) filter (
                    where rp.application_status = 'ACCEPTED'
                ),
            0
        )::integer as available_spots
    from public.post_roles pr
    join public.volunteer_posts vp
      on vp.post_id = pr.post_id
    left join public.role_participations rp
      on rp.post_role_id = pr.post_role_id
    where vp.status = 'PUBLISHED'
    group by pr.post_role_id, pr.capacity
    order by pr.post_role_id;
$$;

revoke all on function public.get_published_role_metrics() from public;
grant execute on function public.get_published_role_metrics()
    to anon, authenticated;

-- Full application history for only the authenticated volunteer. This secure
-- RPC also supplies archived event details needed by Completed certificates.
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
        vp.post_id,
        pr.post_role_id,
        vp.title as event_title,
        o.organisation_name,
        rt.role_name as role_title,
        up.full_name as volunteer_name,
        rp.application_status,
        rp.completion_status,
        rp.decision_note,
        case
            when rp.completion_status = 'COMPLETED'
                then coalesce(att.verified_minutes, 0)::integer
            else null
        end as verified_minutes,
        rp.completed_at,
        ve.feedback,
        sp.name as primary_skill_path,
        coalesce(skill_data.skill_names, '[]'::jsonb)
            as practised_skill_names,
        coalesce(pd.start_date, rd.start_date)::text as event_date,
        case
            when pd.post_id is not null then
                to_char(pd.start_time, 'FMHH12:MI AM') ||
                ' - ' ||
                to_char(pd.end_time, 'FMHH12:MI AM')
            else 'Flexible'
        end as event_time,
        case
            when pd.post_id is not null then
                coalesce(nullif(pd.location_address, ''), pd.location_name)
            else 'Online'
        end as event_location,
        rp.created_at
    from public.role_participations rp
    join public.user_profiles up
      on up.user_id = rp.user_id
    join public.post_roles pr
      on pr.post_role_id = rp.post_role_id
    join public.volunteer_posts vp
      on vp.post_id = pr.post_id
    join public.organisations o
      on o.organisation_id = vp.organisation_id
    join public.role_templates rt
      on rt.role_template_id = pr.role_template_id
    join public.skill_paths sp
      on sp.skill_path_id = rt.skill_path_id
    left join public.physical_details pd
      on pd.post_id = vp.post_id
    left join public.remote_details rd
      on rd.post_id = vp.post_id
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
            case
                when jsonb_array_length(pr.practised_skills) > 0
                    then pr.practised_skills
                else rt.skills_practised
            end
        ) ids(skill_id)
        join public.skills s
          on s.skill_id = ids.skill_id
    ) skill_data on true
    where up.auth_user_id = auth.uid()
      and up.account_type = 'VOLUNTEER'
    order by rp.created_at desc;
$$;

revoke all on function public.get_my_volunteer_applications() from public;
grant execute on function public.get_my_volunteer_applications()
    to authenticated;

-- Smaller achievement RPC kept for compatibility with the repository fallback.
create or replace function public.get_my_volunteer_achievement_records()
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
set search_path = public
as $$
    select
        rp.participation_id,
        coalesce(sum(ar.verified_minutes), 0)::integer,
        rp.completed_at,
        ve.rating,
        ve.feedback,
        up.full_name
    from public.role_participations rp
    join public.user_profiles up
      on up.user_id = rp.user_id
    left join public.attendance_records ar
      on ar.participation_id = rp.participation_id
    left join public.volunteer_evaluations ve
      on ve.participation_id = rp.participation_id
    where up.auth_user_id = auth.uid()
      and rp.application_status = 'ACCEPTED'
      and rp.completion_status = 'COMPLETED'
    group by
        rp.participation_id,
        rp.completed_at,
        ve.rating,
        ve.feedback,
        up.full_name
    order by rp.completed_at desc;
$$;

revoke all on function
    public.get_my_volunteer_achievement_records()
    from public;
grant execute on function
    public.get_my_volunteer_achievement_records()
    to authenticated;

-- Recalculate all eight Skill Paths from accepted AND completed evidence only.
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
        select sp.skill_path_id, sp.progression_type
        from public.skill_paths sp
        order by sp.skill_path_id
    loop
        select
            count(distinct rp.participation_id)::integer,
            coalesce(sum(ar.verified_minutes), 0)::integer
        into completed_assignments, completed_minutes
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
                user_id, skill_path_id, current_level,
                verified_assignments, verified_minutes, updated_at
            ) values (
                current_volunteer_user_id,
                path_record.skill_path_id,
                calculated_level,
                completed_assignments,
                case
                    when path_record.progression_type = 'ASSIGNMENTS_ONLY'
                        then null
                    else completed_minutes
                end,
                now()
            );
        end if;
    end loop;
end;
$$;

revoke all on function public.refresh_my_skill_path_progress() from public;
grant execute on function public.refresh_my_skill_path_progress()
    to authenticated;

commit;

-- One compact verification result after the migration.
select
    (select count(*) from public.volunteer_posts where status = 'PUBLISHED')
        as published_events,
    (select count(*) from public.post_roles pr
        join public.volunteer_posts vp on vp.post_id = pr.post_id
        where vp.status = 'PUBLISHED')
        as published_roles,
    (select count(*) from public.role_participations
        where completion_status = 'COMPLETED')
        as completed_participations,
    (select count(*) from public.role_participations
        where application_status = 'DECLINED')
        as rejected_applications;
