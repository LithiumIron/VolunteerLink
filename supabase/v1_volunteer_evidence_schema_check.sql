-- Run this read-only query once before adding completed-service demo evidence.
-- It prevents us from duplicating or contradicting the team's normalized ERD.

select
    table_name,
    ordinal_position,
    column_name,
    data_type,
    is_nullable,
    column_default
from information_schema.columns
where table_schema = 'v1_erd_test'
  and (
      table_name ~* '(particip|attend|evaluat|certificat|evidence|badge|progress)'
      or table_name in (
          'post_role_skills',
          'post_role_responsibilities',
          'post_role_screening_questions',
          'schedule_item_roles',
          'role_template_skills'
      )
  )
order by table_name, ordinal_position;
