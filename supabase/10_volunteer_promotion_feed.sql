-- Run ONCE in Supabase SQL Editor after compiling the Android patch.
-- NEW read-only Volunteer RPC. No existing function, RLS policy, purchase,
-- payment, clock, application or attendance row is changed.
-- This file deliberately refuses to replace an existing function of this name.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

CREATE FUNCTION v1_erd_test.volunteer_promotion_feed_v1()
RETURNS TABLE (
    post_id text,
    priority_rank integer,
    segment_start_ms bigint,
    segment_end_ms bigint,
    promotion_start_ms bigint,
    observed_at_ms bigint
)
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = pg_catalog
SET timezone = 'UTC'
AS $feed$
DECLARE
    v_now timestamptz := v1_erd_test.volunteer_app_now();
BEGIN
    IF auth.uid() IS NULL OR NOT EXISTS (
        SELECT 1 FROM v1_erd_test.user_profiles AS u
        WHERE u.auth_user_id = auth.uid() AND u.account_type = 'VOLUNTEER'
    ) THEN
        RAISE EXCEPTION 'A signed-in volunteer account is required.' USING ERRCODE = '42501';
    END IF;

    RETURN QUERY
    WITH upcoming AS (
        SELECT p.post_id,
            CASE upper(p.mode)
                WHEN 'PHYSICAL' THEN d.start_date
                WHEN 'REMOTE' THEN r.start_date
                WHEN 'HYBRID' THEN
                    CASE WHEN d.start_date IS NOT NULL AND r.start_date IS NOT NULL
                         THEN least(d.start_date, r.start_date) END
            END AS cutoff_date,
            CASE WHEN upper(p.mode) = 'REMOTE' THEN 'Asia/Kuala_Lumpur'
                 ELSE coalesce(nullif(btrim(d.time_zone), ''), 'Asia/Kuala_Lumpur') END AS zone
        FROM v1_erd_test.volunteer_posts AS p
        LEFT JOIN v1_erd_test.physical_details AS d ON d.post_id = p.post_id
        LEFT JOIN v1_erd_test.remote_details AS r ON r.post_id = p.post_id
        WHERE upper(p.status) = 'PUBLISHED'
    ), cutoffs AS (
        SELECT u.post_id,
            CASE WHEN u.cutoff_date IS NOT NULL AND EXISTS (
                SELECT 1 FROM pg_catalog.pg_timezone_names AS z WHERE z.name = u.zone
            ) THEN u.cutoff_date::timestamp AT TIME ZONE u.zone END AS cutoff_at
        FROM upcoming AS u
    ), active AS (
        SELECT p.*, count(*) OVER (PARTITION BY p.post_id) AS active_count
        FROM v1_erd_test.post_promotions AS p
        JOIN cutoffs AS c ON c.post_id = p.post_id
        WHERE p.start_at <= v_now AND p.end_at > v_now
            AND v_now < c.cutoff_at AND p.end_at <= c.cutoff_at
    ), ordered AS (
        SELECT a.post_id, a.promotion_id, a.start_at, a.end_at,
            pay.payment_id, pay.paid_at, pay.package_days, pay.amount,
            coalesce(sum(pay.package_days::bigint) OVER (
                PARTITION BY a.promotion_id ORDER BY pay.paid_at,
                    CASE WHEN pay.payment_id ~ '^PAY[0-9]+$'
                         THEN substring(pay.payment_id FROM 4)::numeric END
                ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
            ), 0) AS preceding_days,
            sum(pay.package_days::bigint) OVER (PARTITION BY a.promotion_id) AS total_days,
            min(pay.paid_at) OVER (PARTITION BY a.promotion_id) AS first_payment_at,
            bool_and(pay.payment_id ~ '^PAY[0-9]+$' AND pay.package_days IN (1, 3, 7)
                AND pay.amount > 0 AND pay.paid_at >= a.start_at AND pay.paid_at <= v_now)
                OVER (PARTITION BY a.promotion_id) AS valid_payments
        FROM active AS a
        JOIN v1_erd_test.promotion_payments AS pay ON pay.promotion_id = a.promotion_id
        WHERE a.active_count = 1
    ), segments AS (
        -- Each purchase buys the NEXT segment, including extensions. Do not use
        -- the latest receipt amount for the entire promotion; never sum amounts.
        SELECT o.*,
            o.start_at + (o.preceding_days::double precision * interval '24 hours') AS segment_start,
            o.start_at + ((o.preceding_days + o.package_days)::double precision * interval '24 hours') AS segment_end
        FROM ordered AS o
        WHERE o.valid_payments AND o.first_payment_at = o.start_at
            AND o.end_at = o.start_at + (o.total_days::double precision * interval '24 hours')
    ), validated AS (
        SELECT s.*, bool_and(s.paid_at <= s.segment_start)
            OVER (PARTITION BY s.promotion_id) AS valid_order
        FROM segments AS s
    ), current_segments AS (
        SELECT s.* FROM validated AS s
        WHERE s.valid_order AND s.segment_start <= v_now AND v_now < s.segment_end
    )
    SELECT s.post_id::text,
        dense_rank() OVER (ORDER BY s.amount DESC)::integer,
        floor(extract(epoch FROM s.segment_start) * 1000)::bigint,
        floor(extract(epoch FROM s.segment_end) * 1000)::bigint,
        floor(extract(epoch FROM s.start_at) * 1000)::bigint,
        floor(extract(epoch FROM v_now) * 1000)::bigint
    FROM current_segments AS s
    ORDER BY s.amount DESC, s.start_at, s.post_id;
END;
$feed$;

REVOKE ALL ON FUNCTION v1_erd_test.volunteer_promotion_feed_v1() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION v1_erd_test.volunteer_promotion_feed_v1() TO authenticated;
COMMENT ON FUNCTION v1_erd_test.volunteer_promotion_feed_v1() IS
    'Volunteer public promotion placement only; current purchase segment, not accumulated spend. Read-only v1.';
NOTIFY pgrst, 'reload schema';
COMMIT;
