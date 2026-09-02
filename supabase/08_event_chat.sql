begin;

create extension if not exists pgcrypto;

-- One chat group for each VolunteerLink event/post.
create table if not exists v1_erd_test.event_chat_groups (
    chat_id uuid primary key default gen_random_uuid(),
    post_id text not null unique
        references v1_erd_test.volunteer_posts(post_id)
        on delete cascade,
    title text not null,
    description text not null default '',
    created_by_user_id text not null
        references v1_erd_test.user_profiles(user_id),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists v1_erd_test.event_chat_members (
    chat_id uuid not null
        references v1_erd_test.event_chat_groups(chat_id)
        on delete cascade,
    user_id text not null
        references v1_erd_test.user_profiles(user_id)
        on delete cascade,
    member_role text not null
        check (member_role in ('ORGANISATION', 'VOLUNTEER')),
    joined_at timestamptz not null default now(),
    primary key (chat_id, user_id)
);

-- Organisation sends an invitation; a volunteer chooses whether to accept.
create table if not exists v1_erd_test.event_chat_invitations (
    invitation_id uuid primary key default gen_random_uuid(),
    chat_id uuid not null
        references v1_erd_test.event_chat_groups(chat_id)
        on delete cascade,
    user_id text not null
        references v1_erd_test.user_profiles(user_id)
        on delete cascade,
    invited_by_user_id text not null
        references v1_erd_test.user_profiles(user_id),
    status text not null default 'PENDING'
        check (status in ('PENDING', 'ACCEPTED', 'DECLINED')),
    created_at timestamptz not null default now(),
    responded_at timestamptz,
    unique (chat_id, user_id)
);

create table if not exists v1_erd_test.event_chat_messages (
    message_id uuid primary key default gen_random_uuid(),
    chat_id uuid not null
        references v1_erd_test.event_chat_groups(chat_id)
        on delete cascade,
    sender_user_id text not null
        references v1_erd_test.user_profiles(user_id),
    body text not null default '',
    message_type text not null default 'TEXT'
        check (message_type in ('TEXT', 'IMAGE', 'VIDEO', 'AUDIO', 'FILE')),
    attachment_path text,
    attachment_name text,
    attachment_mime_type text,
    reply_to_message_id uuid
        references v1_erd_test.event_chat_messages(message_id)
        on delete set null,
    sent_at timestamptz not null default v1_erd_test.volunteer_app_now(),
    edited_at timestamptz
);

create index if not exists event_chat_members_user_id_index
    on v1_erd_test.event_chat_members(user_id);

create index if not exists event_chat_messages_chat_sent_index
    on v1_erd_test.event_chat_messages(chat_id, sent_at);

-- Finds the VolunteerLink user_profiles.user_id for the authenticated user.
create or replace function v1_erd_test.current_chat_user_id()
returns text
language plpgsql
stable
security definer
set search_path = v1_erd_test, pg_temp
as $$
declare
    current_user_id text;
begin
    select user_id
    into current_user_id
    from v1_erd_test.user_profiles
    where auth_user_id = auth.uid()
    limit 1;

    if current_user_id is null then
        raise exception 'A VolunteerLink user profile is required.';
    end if;

    return current_user_id;
end;
$$;

-- Creates the event chat if needed and makes the event owner an organisation member.
create or replace function v1_erd_test.ensure_event_chat_group(
    p_post_id text
)
returns uuid
language plpgsql
security definer
set search_path = v1_erd_test, pg_temp
as $$
declare
    v_chat_id uuid;
    v_organisation_user_id text;
    v_title text;
begin
    select
        organisation.user_id,
        post.title
    into
        v_organisation_user_id,
        v_title
    from v1_erd_test.volunteer_posts post
    join v1_erd_test.organisations organisation
        on organisation.organisation_id = post.organisation_id
    where post.post_id = p_post_id;

    if v_organisation_user_id is null then
        raise exception 'Event or event organisation was not found.';
    end if;

    select chat_id
    into v_chat_id
    from v1_erd_test.event_chat_groups
    where post_id = p_post_id;

    if v_chat_id is null then
        insert into v1_erd_test.event_chat_groups (
            post_id,
            title,
            created_by_user_id
        )
        values (
            p_post_id,
            v_title,
            v_organisation_user_id
        )
        returning chat_id into v_chat_id;
    end if;

    insert into v1_erd_test.event_chat_members (
        chat_id,
        user_id,
        member_role
    )
    values (
        v_chat_id,
        v_organisation_user_id,
        'ORGANISATION'
    )
    on conflict (chat_id, user_id) do nothing;

    return v_chat_id;
end;
$$;

-- Automatically creates a chat group whenever an organisation creates an event/post.
create or replace function v1_erd_test.create_event_chat_after_post_insert()
returns trigger
language plpgsql
security definer
set search_path = v1_erd_test, pg_temp
as $$
begin
    perform v1_erd_test.ensure_event_chat_group(new.post_id);
    return new;
end;
$$;

drop trigger if exists volunteer_post_create_event_chat
on v1_erd_test.volunteer_posts;

create trigger volunteer_post_create_event_chat
after insert on v1_erd_test.volunteer_posts
for each row
execute function v1_erd_test.create_event_chat_after_post_insert();

-- Backfills groups for all existing dummy events/posts.
select v1_erd_test.ensure_event_chat_group(post_id)
from v1_erd_test.volunteer_posts;

-- Instant Join: only an accepted volunteer can add themselves.
create or replace function v1_erd_test.join_my_accepted_event_chat(
    p_post_id text
)
returns uuid
language plpgsql
security definer
set search_path = v1_erd_test, pg_temp
as $$
declare
    v_user_id text;
    v_chat_id uuid;
begin
    v_user_id := v1_erd_test.current_chat_user_id();

    if not exists (
        select 1
        from v1_erd_test.role_participations participation
        where participation.post_id = p_post_id
          and participation.user_id = v_user_id
          and participation.application_status = 'ACCEPTED'
    ) then
        raise exception 'Only accepted volunteers may join this event chat.';
    end if;

    v_chat_id := v1_erd_test.ensure_event_chat_group(p_post_id);

    insert into v1_erd_test.event_chat_members (
        chat_id,
        user_id,
        member_role
    )
    values (
        v_chat_id,
        v_user_id,
        'VOLUNTEER'
    )
    on conflict (chat_id, user_id) do nothing;

    return v_chat_id;
end;
$$;

-- Organisation sends a group-chat invitation to an accepted volunteer.
create or replace function v1_erd_test.invite_accepted_volunteer_to_event_chat(
    p_post_id text,
    p_volunteer_user_id text
)
returns uuid
language plpgsql
security definer
set search_path = v1_erd_test, pg_temp
as $$
declare
    v_organisation_user_id text;
    v_chat_id uuid;
    v_invitation_id uuid;
begin
    v_organisation_user_id := v1_erd_test.current_chat_user_id();

    if not exists (
        select 1
        from v1_erd_test.volunteer_posts post
        join v1_erd_test.organisations organisation
            on organisation.organisation_id = post.organisation_id
        where post.post_id = p_post_id
          and organisation.user_id = v_organisation_user_id
    ) then
        raise exception 'Only the event organisation may invite volunteers.';
    end if;

    if not exists (
        select 1
        from v1_erd_test.role_participations participation
        where participation.post_id = p_post_id
          and participation.user_id = p_volunteer_user_id
          and participation.application_status = 'ACCEPTED'
    ) then
        raise exception 'Only accepted volunteers may be invited.';
    end if;

    v_chat_id := v1_erd_test.ensure_event_chat_group(p_post_id);

    insert into v1_erd_test.event_chat_invitations (
        chat_id,
        user_id,
        invited_by_user_id,
        status,
        created_at,
        responded_at
    )
    values (
        v_chat_id,
        p_volunteer_user_id,
        v_organisation_user_id,
        'PENDING',
        v1_erd_test.volunteer_app_now(),
        null
    )
    on conflict (chat_id, user_id)
    do update set
        invited_by_user_id = excluded.invited_by_user_id,
        status = 'PENDING',
        created_at = excluded.created_at,
        responded_at = null
    returning invitation_id into v_invitation_id;

    return v_invitation_id;
end;
$$;

-- Volunteer accepts or declines an organisation chat invitation.
create or replace function v1_erd_test.respond_to_event_chat_invitation(
    p_invitation_id uuid,
    p_accept boolean
)
returns uuid
language plpgsql
security definer
set search_path = v1_erd_test, pg_temp
as $$
declare
    v_user_id text;
    v_chat_id uuid;
begin
    v_user_id := v1_erd_test.current_chat_user_id();

    select chat_id
    into v_chat_id
    from v1_erd_test.event_chat_invitations
    where invitation_id = p_invitation_id
      and user_id = v_user_id
      and status = 'PENDING';

    if v_chat_id is null then
        raise exception 'This chat invitation is unavailable.';
    end if;

    update v1_erd_test.event_chat_invitations
    set
        status = case when p_accept then 'ACCEPTED' else 'DECLINED' end,
        responded_at = v1_erd_test.volunteer_app_now()
    where invitation_id = p_invitation_id;

    if p_accept then
        insert into v1_erd_test.event_chat_members (
            chat_id,
            user_id,
            member_role
        )
        values (
            v_chat_id,
            v_user_id,
            'VOLUNTEER'
        )
        on conflict (chat_id, user_id) do nothing;
    end if;

    return v_chat_id;
end;
$$;

-- A member can send a message only to their own event chat.
create or replace function v1_erd_test.send_event_chat_message(
    p_chat_id uuid,
    p_body text default '',
    p_message_type text default 'TEXT',
    p_attachment_path text default null,
    p_attachment_name text default null,
    p_attachment_mime_type text default null,
    p_reply_to_message_id uuid default null
)
returns uuid
language plpgsql
security definer
set search_path = v1_erd_test, pg_temp
as $$
declare
    v_user_id text;
    v_message_id uuid;
begin
    v_user_id := v1_erd_test.current_chat_user_id();

    if not exists (
        select 1
        from v1_erd_test.event_chat_members member
        where member.chat_id = p_chat_id
          and member.user_id = v_user_id
    ) then
        raise exception 'You are not a member of this event chat.';
    end if;

    if btrim(coalesce(p_body, '')) = ''
       and p_attachment_path is null then
        raise exception 'A message needs text or an attachment.';
    end if;

    insert into v1_erd_test.event_chat_messages (
        chat_id,
        sender_user_id,
        body,
        message_type,
        attachment_path,
        attachment_name,
        attachment_mime_type,
        reply_to_message_id
    )
    values (
        p_chat_id,
        v_user_id,
        coalesce(p_body, ''),
        p_message_type,
        p_attachment_path,
        p_attachment_name,
        p_attachment_mime_type,
        p_reply_to_message_id
    )
    returning message_id into v_message_id;

    return v_message_id;
end;
$$;

-- Only the event organisation can edit the group description.
create or replace function v1_erd_test.update_event_chat_description(
    p_chat_id uuid,
    p_description text
)
returns void
language plpgsql
security definer
set search_path = v1_erd_test, pg_temp
as $$
declare
    v_user_id text;
begin
    v_user_id := v1_erd_test.current_chat_user_id();

    if not exists (
        select 1
        from v1_erd_test.event_chat_groups chat
        join v1_erd_test.volunteer_posts post
            on post.post_id = chat.post_id
        join v1_erd_test.organisations organisation
            on organisation.organisation_id = post.organisation_id
        where chat.chat_id = p_chat_id
          and organisation.user_id = v_user_id
    ) then
        raise exception 'Only the event organisation may edit this description.';
    end if;

    update v1_erd_test.event_chat_groups
    set
        description = coalesce(p_description, ''),
        updated_at = v1_erd_test.volunteer_app_now()
    where chat_id = p_chat_id;
end;
$$;

-- Row-level security: users can only read chats they belong to.
create or replace function v1_erd_test.is_event_chat_member(
    p_chat_id uuid
)
returns boolean
language sql
stable
security definer
set search_path = v1_erd_test, pg_temp
as $$
    select exists (
        select 1
        from v1_erd_test.event_chat_members member
        join v1_erd_test.user_profiles profile
            on profile.user_id = member.user_id
        where member.chat_id = p_chat_id
          and profile.auth_user_id = auth.uid()
    );
$$;

alter table v1_erd_test.event_chat_groups enable row level security;
alter table v1_erd_test.event_chat_members enable row level security;
alter table v1_erd_test.event_chat_invitations enable row level security;
alter table v1_erd_test.event_chat_messages enable row level security;

drop policy if exists event_chat_group_members_can_read
on v1_erd_test.event_chat_groups;

create policy event_chat_group_members_can_read
on v1_erd_test.event_chat_groups
for select to authenticated
using (v1_erd_test.is_event_chat_member(chat_id));

drop policy if exists event_chat_members_can_read
on v1_erd_test.event_chat_members;

create policy event_chat_members_can_read
on v1_erd_test.event_chat_members
for select to authenticated
using (v1_erd_test.is_event_chat_member(chat_id));

drop policy if exists event_chat_messages_can_read
on v1_erd_test.event_chat_messages;

create policy event_chat_messages_can_read
on v1_erd_test.event_chat_messages
for select to authenticated
using (v1_erd_test.is_event_chat_member(chat_id));

drop policy if exists event_chat_invitation_owner_can_read
on v1_erd_test.event_chat_invitations;

create policy event_chat_invitation_owner_can_read
on v1_erd_test.event_chat_invitations
for select to authenticated
using (user_id = v1_erd_test.current_chat_user_id());

grant usage on schema v1_erd_test to authenticated;

grant select on
    v1_erd_test.event_chat_groups,
    v1_erd_test.event_chat_members,
    v1_erd_test.event_chat_invitations,
    v1_erd_test.event_chat_messages
to authenticated;

grant execute on function
    v1_erd_test.join_my_accepted_event_chat(text),
    v1_erd_test.invite_accepted_volunteer_to_event_chat(text, text),
    v1_erd_test.respond_to_event_chat_invitation(uuid, boolean),
    v1_erd_test.send_event_chat_message(uuid, text, text, text, text, text, uuid),
    v1_erd_test.update_event_chat_description(uuid, text)
to authenticated;

commit;

-- Returns every event chat the signed-in user belongs to,
-- including real VolunteerLink names for every member.
create or replace function v1_erd_test.get_my_event_chats()
returns table(
    chat_id uuid,
    post_id text,
    title text,
    description text,
    updated_at timestamptz,
    member_user_id text,
    member_name text,
    member_role text,
    member_initial text
)
language sql
stable
security definer
set search_path = v1_erd_test, pg_temp
as $$
    with current_member as (
        select v1_erd_test.current_chat_user_id() as user_id
    )
    select
        chat.chat_id,
        chat.post_id,
        chat.title,
        chat.description,
        chat.updated_at,
        member.user_id as member_user_id,
        coalesce(profile.full_name, 'VolunteerLink user') as member_name,
        member.member_role,
        upper(
            left(
                coalesce(profile.full_name, 'V'),
                1
            )
        ) as member_initial
    from v1_erd_test.event_chat_groups chat
    join v1_erd_test.volunteer_posts post
        on post.post_id = chat.post_id
    join v1_erd_test.event_chat_members current_chat_member
        on current_chat_member.chat_id = chat.chat_id
    join current_member
        on current_member.user_id = current_chat_member.user_id
    join v1_erd_test.event_chat_members member
        on member.chat_id = chat.chat_id
    join v1_erd_test.user_profiles profile
        on profile.user_id = member.user_id
    where post.status = 'PUBLISHED'
    order by chat.updated_at desc, member.member_role, member.user_id;
$$;

-- Returns messages only when the current user is a group member.
create or replace function v1_erd_test.get_event_chat_messages(
    p_chat_id uuid
)
returns table(
    message_id uuid,
    sender_user_id text,
    sender_name text,
    sender_initial text,
    body text,
    message_type text,
    attachment_path text,
    attachment_name text,
    attachment_mime_type text,
    reply_to_message_id uuid,
    sent_at timestamptz,
    edited_at timestamptz
)
language sql
stable
security definer
set search_path = v1_erd_test, pg_temp
as $$
    select
        message.message_id,
        message.sender_user_id,
        coalesce(profile.full_name, 'VolunteerLink user') as sender_name,
        upper(
            left(
                coalesce(profile.full_name, 'V'),
                1
            )
        ) as sender_initial,
        message.body,
        message.message_type,
        message.attachment_path,
        message.attachment_name,
        message.attachment_mime_type,
        message.reply_to_message_id,
        message.sent_at,
        message.edited_at
    from v1_erd_test.event_chat_messages message
    join v1_erd_test.user_profiles profile
        on profile.user_id = message.sender_user_id
    where message.chat_id = p_chat_id
      and v1_erd_test.is_event_chat_member(p_chat_id)
    order by message.sent_at;
$$;

grant execute on function
    v1_erd_test.get_my_event_chats(),
    v1_erd_test.get_event_chat_messages(uuid)
to authenticated;