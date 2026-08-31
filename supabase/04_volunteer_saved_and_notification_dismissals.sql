-- VolunteerLink V16.1: Volunteer-owned bookmarks and notification dismissals.
-- Additive only. Does not replace Organisation, attendance, review or post RPCs.

begin;

create table if not exists v1_erd_test.volunteer_saved_opportunities (
    user_id text not null references v1_erd_test.user_profiles(user_id) on delete cascade,
    post_id text not null references v1_erd_test.volunteer_posts(post_id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (user_id, post_id)
);

create table if not exists v1_erd_test.volunteer_notification_dismissals (
    user_id text not null references v1_erd_test.user_profiles(user_id) on delete cascade,
    notification_key text not null,
    dismissed_at timestamptz not null default now(),
    primary key (user_id, notification_key),
    check (length(btrim(notification_key)) between 1 and 300)
);

alter table v1_erd_test.volunteer_saved_opportunities enable row level security;
alter table v1_erd_test.volunteer_notification_dismissals enable row level security;

drop policy if exists volunteer_owns_saved_opportunities
on v1_erd_test.volunteer_saved_opportunities;
create policy volunteer_owns_saved_opportunities
on v1_erd_test.volunteer_saved_opportunities
for all to authenticated
using (
    user_id = (
        select profile.user_id from v1_erd_test.user_profiles profile
        where profile.auth_user_id = auth.uid()
          and profile.account_type = 'VOLUNTEER'
        limit 1
    )
)
with check (
    user_id = (
        select profile.user_id from v1_erd_test.user_profiles profile
        where profile.auth_user_id = auth.uid()
          and profile.account_type = 'VOLUNTEER'
        limit 1
    )
);

drop policy if exists volunteer_owns_notification_dismissals
on v1_erd_test.volunteer_notification_dismissals;
create policy volunteer_owns_notification_dismissals
on v1_erd_test.volunteer_notification_dismissals
for all to authenticated
using (
    user_id = (
        select profile.user_id from v1_erd_test.user_profiles profile
        where profile.auth_user_id = auth.uid()
          and profile.account_type = 'VOLUNTEER'
        limit 1
    )
)
with check (
    user_id = (
        select profile.user_id from v1_erd_test.user_profiles profile
        where profile.auth_user_id = auth.uid()
          and profile.account_type = 'VOLUNTEER'
        limit 1
    )
);

create or replace function v1_erd_test.get_my_saved_opportunity_ids()
returns table(post_id text)
language sql stable security definer
set search_path = v1_erd_test, pg_temp
as $$
    select saved.post_id
    from v1_erd_test.volunteer_saved_opportunities saved
    join v1_erd_test.user_profiles profile on profile.user_id = saved.user_id
    where profile.auth_user_id = auth.uid()
      and profile.account_type = 'VOLUNTEER'
    order by saved.created_at desc;
$$;

create or replace function v1_erd_test.set_my_saved_opportunity(
    target_post_id text,
    should_save boolean
) returns jsonb
language plpgsql security definer
set search_path = v1_erd_test, pg_temp
as $$
declare volunteer_id text;
begin
    if auth.uid() is null then
        raise exception 'Authentication is required.' using errcode = '28000';
    end if;
    select profile.user_id into volunteer_id
    from v1_erd_test.user_profiles profile
    where profile.auth_user_id = auth.uid()
      and profile.account_type = 'VOLUNTEER'
    limit 1;
    if volunteer_id is null then raise exception 'Volunteer profile was not found.'; end if;
    if not exists (select 1 from v1_erd_test.volunteer_posts post where post.post_id = target_post_id) then
        raise exception 'Opportunity was not found.';
    end if;
    if should_save then
        insert into v1_erd_test.volunteer_saved_opportunities(user_id, post_id)
        values (volunteer_id, target_post_id)
        on conflict (user_id, post_id) do nothing;
    else
        delete from v1_erd_test.volunteer_saved_opportunities
        where user_id = volunteer_id and post_id = target_post_id;
    end if;
    return jsonb_build_object('post_id', target_post_id, 'saved', should_save);
end;
$$;

create or replace function v1_erd_test.get_my_dismissed_notification_keys()
returns table(notification_key text)
language sql stable security definer
set search_path = v1_erd_test, pg_temp
as $$
    select dismissal.notification_key
    from v1_erd_test.volunteer_notification_dismissals dismissal
    join v1_erd_test.user_profiles profile on profile.user_id = dismissal.user_id
    where profile.auth_user_id = auth.uid()
      and profile.account_type = 'VOLUNTEER';
$$;

create or replace function v1_erd_test.dismiss_my_volunteer_notification(
    target_notification_key text
) returns void
language plpgsql security definer
set search_path = v1_erd_test, pg_temp
as $$
declare volunteer_id text;
begin
    select profile.user_id into volunteer_id
    from v1_erd_test.user_profiles profile
    where profile.auth_user_id = auth.uid()
      and profile.account_type = 'VOLUNTEER'
    limit 1;
    if volunteer_id is null then raise exception 'Volunteer profile was not found.'; end if;
    insert into v1_erd_test.volunteer_notification_dismissals(user_id, notification_key)
    values (volunteer_id, btrim(target_notification_key))
    on conflict (user_id, notification_key)
    do update set dismissed_at = now();
end;
$$;

create or replace function v1_erd_test.dismiss_my_volunteer_notifications(
    target_notification_keys text[]
) returns void
language plpgsql security definer
set search_path = v1_erd_test, pg_temp
as $$
declare volunteer_id text;
begin
    select profile.user_id into volunteer_id
    from v1_erd_test.user_profiles profile
    where profile.auth_user_id = auth.uid()
      and profile.account_type = 'VOLUNTEER'
    limit 1;
    if volunteer_id is null then raise exception 'Volunteer profile was not found.'; end if;
    insert into v1_erd_test.volunteer_notification_dismissals(user_id, notification_key)
    select volunteer_id, btrim(key)
    from unnest(target_notification_keys) key
    where btrim(key) <> ''
    on conflict (user_id, notification_key)
    do update set dismissed_at = now();
end;
$$;

grant usage on schema v1_erd_test to authenticated;
grant select, insert, delete on v1_erd_test.volunteer_saved_opportunities to authenticated;
grant select, insert, update on v1_erd_test.volunteer_notification_dismissals to authenticated;
grant execute on function v1_erd_test.get_my_saved_opportunity_ids() to authenticated;
grant execute on function v1_erd_test.set_my_saved_opportunity(text, boolean) to authenticated;
grant execute on function v1_erd_test.get_my_dismissed_notification_keys() to authenticated;
grant execute on function v1_erd_test.dismiss_my_volunteer_notification(text) to authenticated;
grant execute on function v1_erd_test.dismiss_my_volunteer_notifications(text[]) to authenticated;

commit;

select table_name
from information_schema.tables
where table_schema = 'v1_erd_test'
  and table_name in ('volunteer_saved_opportunities', 'volunteer_notification_dismissals')
order by table_name;
