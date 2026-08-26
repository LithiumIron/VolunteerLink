-- Public aggregate only: no user IDs, answers or private application rows.
-- Run once in Supabase SQL Editor after the existing RLS/RPC script.

create or replace function public.get_published_opportunity_metrics()
returns table (
    post_id text,
    application_count integer,
    available_spots integer
)
language sql
stable
security definer
set search_path = public
as $$
    with role_capacity as (
        select
            pr.post_id,
            sum(pr.capacity)::integer as total_capacity
        from public.post_roles pr
        group by pr.post_id
    ),
    participation_totals as (
        select
            pr.post_id,
            count(*) filter (
                where rp.application_status <> 'CANCELLED'
            )::integer as application_count,
            count(*) filter (
                where rp.application_status = 'ACCEPTED'
            )::integer as accepted_count
        from public.post_roles pr
        left join public.role_participations rp
            on rp.post_role_id = pr.post_role_id
        group by pr.post_id
    )
    select
        vp.post_id,
        coalesce(pt.application_count, 0) as application_count,
        greatest(
            coalesce(rc.total_capacity, 0) -
                coalesce(pt.accepted_count, 0),
            0
        )::integer as available_spots
    from public.volunteer_posts vp
    left join role_capacity rc
        on rc.post_id = vp.post_id
    left join participation_totals pt
        on pt.post_id = vp.post_id
    where vp.status = 'PUBLISHED'
    order by vp.post_id;
$$;

revoke all on function
    public.get_published_opportunity_metrics()
    from public;

grant execute on function
    public.get_published_opportunity_metrics()
    to anon, authenticated;

comment on function
    public.get_published_opportunity_metrics()
is 'Returns safe published-post application totals and remaining capacity without exposing private applications.';
