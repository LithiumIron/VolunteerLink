-- VolunteerLink v1_erd_test: allow authenticated volunteers to read the
-- normalized details written by Organisation Create.
-- This migration does not delete or replace organisation-created data.

grant usage on schema v1_erd_test to authenticated;

grant select on table
    v1_erd_test.role_template_skills,
    v1_erd_test.post_role_skills,
    v1_erd_test.post_role_responsibilities,
    v1_erd_test.post_role_screening_questions,
    v1_erd_test.schedule_item_roles
to authenticated;

alter table v1_erd_test.role_template_skills enable row level security;
alter table v1_erd_test.post_role_skills enable row level security;
alter table v1_erd_test.post_role_responsibilities enable row level security;
alter table v1_erd_test.post_role_screening_questions enable row level security;
alter table v1_erd_test.schedule_item_roles enable row level security;

drop policy if exists volunteer_read_role_template_skills
on v1_erd_test.role_template_skills;

create policy volunteer_read_role_template_skills
on v1_erd_test.role_template_skills
for select
to authenticated
using (true);

drop policy if exists volunteer_read_published_post_role_skills
on v1_erd_test.post_role_skills;

create policy volunteer_read_published_post_role_skills
on v1_erd_test.post_role_skills
for select
to authenticated
using (
    exists (
        select 1
        from v1_erd_test.volunteer_posts post
        where post.post_id = post_role_skills.post_id
          and post.status in ('PUBLISHED', 'COMPLETED', 'CLOSED')
    )
);

drop policy if exists volunteer_read_published_role_responsibilities
on v1_erd_test.post_role_responsibilities;

create policy volunteer_read_published_role_responsibilities
on v1_erd_test.post_role_responsibilities
for select
to authenticated
using (
    exists (
        select 1
        from v1_erd_test.volunteer_posts post
        where post.post_id = post_role_responsibilities.post_id
          and post.status in ('PUBLISHED', 'COMPLETED', 'CLOSED')
    )
);

drop policy if exists volunteer_read_published_screening_questions
on v1_erd_test.post_role_screening_questions;

create policy volunteer_read_published_screening_questions
on v1_erd_test.post_role_screening_questions
for select
to authenticated
using (
    exists (
        select 1
        from v1_erd_test.volunteer_posts post
        where post.post_id = post_role_screening_questions.post_id
          and post.status in ('PUBLISHED', 'COMPLETED', 'CLOSED')
    )
);

drop policy if exists volunteer_read_published_schedule_item_roles
on v1_erd_test.schedule_item_roles;

create policy volunteer_read_published_schedule_item_roles
on v1_erd_test.schedule_item_roles
for select
to authenticated
using (
    exists (
        select 1
        from v1_erd_test.volunteer_posts post
        where post.post_id = schedule_item_roles.post_id
          and post.status in ('PUBLISHED', 'COMPLETED', 'CLOSED')
    )
);
