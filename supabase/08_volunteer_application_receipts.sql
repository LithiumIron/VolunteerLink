-- Volunteer-owned additive API. Does not replace Organisation functions or policies.
-- Run once, only after the separate dependency inspection has passed.
-- No clock, post dates, attendance, completion or historical records are changed here.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

-- Stop rather than override a changed teammate contract. Hashes normalize CRLF.
DO $guard$
DECLARE dependency record; actual text;
BEGIN
    FOR dependency IN SELECT * FROM (VALUES
        ('v1_erd_test.application_role_start_date(text,text)', '9ee2ffd2237c952085aa29965523125e'),
        ('v1_erd_test.resolve_pending_applications_for_post(text)', 'a88fa988787fbf20735fc1fd25db2a50'),
        ('v1_erd_test.submit_role_application(text,jsonb)', '9d70f33526cd2f396a7b42e31f9f8055'),
        ('v1_erd_test.validate_role_participation()', 'c9158b02c1aa6ea0c8105db15de52d3b'),
        ('v1_erd_test.volunteer_cancel_application_v2(text,text,text)', '186f8a5ad028c40e81c9855e1cb3ea05'),
        ('v1_erd_test.volunteer_reapply_for_role(text,jsonb)', '53b81ba3dac49bc5c533f2c741c6a1c8')
    ) AS expected(signature, digest) LOOP
        SELECT md5(btrim(replace(prosrc, chr(13), ''), ' ' || chr(9) || chr(10))) INTO actual
        FROM pg_proc WHERE oid = to_regprocedure(dependency.signature);
        IF actual IS DISTINCT FROM dependency.digest THEN
            RAISE EXCEPTION 'Dependency changed or missing: %. Nothing applied. Export the current function before continuing.', dependency.signature;
        END IF;
    END LOOP;
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'v1_erd_test' AND tablename = 'role_participations'
        AND indexdef LIKE 'CREATE UNIQUE INDEX%' AND indexdef LIKE '%(post_id, user_id)%'
        AND indexdef LIKE '%PENDING%' AND indexdef LIKE '%ACCEPTED%') THEN
        RAISE EXCEPTION 'Active-role uniqueness protection is missing. Nothing applied.';
    END IF;
END $guard$;

CREATE TABLE v1_erd_test.volunteer_application_receipts_v1 (
    auth_user_id uuid NOT NULL,
    request_id uuid NOT NULL,
    post_id text NOT NULL,
    role_id text NOT NULL,
    request_digest text NOT NULL,
    result jsonb NOT NULL,
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    PRIMARY KEY (auth_user_id, request_id)
);
ALTER TABLE v1_erd_test.volunteer_application_receipts_v1 ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON v1_erd_test.volunteer_application_receipts_v1 FROM PUBLIC, anon, authenticated;
GRANT SELECT ON v1_erd_test.volunteer_application_receipts_v1 TO authenticated;
CREATE POLICY volunteer_reads_own_receipts ON v1_erd_test.volunteer_application_receipts_v1
FOR SELECT TO authenticated USING (auth.uid() = auth_user_id);

CREATE FUNCTION v1_erd_test.volunteer_application_action_v1(
    p_request_id uuid, p_post_id text, p_role_id text, p_answers jsonb,
    p_previous_role text DEFAULT NULL, p_previous_status text DEFAULT NULL,
    p_previous_created_at text DEFAULT NULL
) RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER
SET search_path = pg_catalog, v1_erd_test, pg_temp
AS $body$
DECLARE
    v_user text;
    v_digest text;
    v_receipt v1_erd_test.volunteer_application_receipts_v1%rowtype;
    v_active v1_erd_test.role_participations%rowtype;
    v_target v1_erd_test.role_participations%rowtype;
    v_result jsonb;
    v_reply jsonb;
    v_detail text;
BEGIN
    IF auth.uid() IS NULL THEN RAISE EXCEPTION 'Sign in before applying.' USING ERRCODE = '28000'; END IF;
    IF p_request_id IS NULL OR coalesce(p_post_id, '') = '' OR coalesce(p_role_id, '') = ''
       OR p_post_id LIKE '%|%' OR p_role_id LIKE '%|%' THEN
        RAISE EXCEPTION 'Invalid application request.';
    END IF;
    SELECT user_id INTO STRICT v_user FROM v1_erd_test.user_profiles
    WHERE auth_user_id = auth.uid() AND account_type = 'VOLUNTEER';
    v_digest := md5(jsonb_build_array(p_post_id, p_role_id, p_answers,
        p_previous_role, p_previous_status, p_previous_created_at)::text);

    -- One device/account request per post at a time. The unique active-role index
    -- remains the final safeguard against older clients using other APIs.
    PERFORM pg_advisory_xact_lock(hashtextextended(auth.uid()::text || ':' || p_post_id, 0));
    SELECT * INTO v_receipt FROM v1_erd_test.volunteer_application_receipts_v1
    WHERE auth_user_id = auth.uid() AND request_id = p_request_id;
    IF FOUND THEN
        IF v_receipt.request_digest <> v_digest THEN RAISE EXCEPTION 'Request changed. Reopen your application.'; END IF;
        RETURN v_receipt.result;
    END IF;

    BEGIN
        -- Deterministic locks; capacity validation and the old-role cancellation
        -- belong to ONE transaction. An exception rolls both back.
        PERFORM 1 FROM v1_erd_test.volunteer_posts WHERE post_id = p_post_id FOR UPDATE;
        IF NOT FOUND THEN RAISE EXCEPTION 'The selected role is unavailable.'; END IF;
        PERFORM 1 FROM v1_erd_test.post_roles WHERE post_id = p_post_id
            ORDER BY role_template_id FOR UPDATE;
        PERFORM 1 FROM v1_erd_test.role_participations
            WHERE post_id = p_post_id AND user_id = v_user ORDER BY role_template_id FOR UPDATE;
        PERFORM v1_erd_test.resolve_pending_applications_for_post(p_post_id);

        IF EXISTS (SELECT 1 FROM v1_erd_test.role_participations
            WHERE post_id = p_post_id AND user_id = v_user
              AND completion_status IN ('COMPLETED', 'NOT_COMPLETED')) THEN
            RAISE EXCEPTION 'Participation finalized. A second role is not allowed.';
        END IF;

        SELECT * INTO v_active FROM v1_erd_test.role_participations
        WHERE post_id = p_post_id AND user_id = v_user
          AND application_status IN ('PENDING', 'ACCEPTED');

        IF v_active.role_template_id = p_role_id THEN
            v_reply := jsonb_build_object('application_status', v_active.application_status);
        ELSE
            IF p_previous_role IS NULL AND v_active.role_template_id IS NOT NULL THEN
                RAISE EXCEPTION 'Your application changed. Refresh and confirm which role to keep.';
            ELSIF p_previous_role IS NOT NULL THEN
                IF v_active.role_template_id IS DISTINCT FROM p_previous_role
                   OR v_active.application_status IS DISTINCT FROM p_previous_status
                   OR v_active.created_at IS DISTINCT FROM nullif(p_previous_created_at, '')::timestamptz THEN
                    RAISE EXCEPTION 'Your application changed. Refresh and confirm which role to keep.';
                END IF;
                PERFORM v1_erd_test.volunteer_cancel_application_v2(
                    p_post_id || '|' || p_previous_role, 'Changing role',
                    'Confirmed a change to role ' || p_role_id || ' in the same opportunity.');
            END IF;

            SELECT * INTO v_target FROM v1_erd_test.role_participations
            WHERE post_id = p_post_id AND role_template_id = p_role_id AND user_id = v_user;
            IF FOUND AND v_target.application_status = 'CANCELLED' THEN
                v_reply := v1_erd_test.volunteer_reapply_for_role(p_post_id || '|' || p_role_id, p_answers);
            ELSE
                v_reply := v1_erd_test.submit_role_application(p_post_id || '|' || p_role_id, p_answers);
            END IF;
        END IF;
        v_result := jsonb_build_object('success', true,
            'application_status', v_reply->>'application_status',
            'message', CASE WHEN v_reply->>'application_status' = 'ACCEPTED'
                THEN 'Place confirmed. You are accepted for this role.'
                ELSE 'Application received. Awaiting organisation review; your place is not confirmed yet.' END);
    EXCEPTION
        WHEN lock_not_available OR deadlock_detected OR serialization_failure THEN
            -- Transient concurrency failure: no receipt, retry the same request ID.
            RAISE;
        WHEN OTHERS THEN
            GET STACKED DIAGNOSTICS v_detail = MESSAGE_TEXT;
            v_result := jsonb_build_object('success', false, 'message',
                CASE
                    WHEN v_detail ILIKE '%ROLE_FULL%' THEN 'This role is full. Choose another open role.'
                    WHEN v_detail ILIKE '%ALREADY_ACCEPTED%' THEN 'You already have a role in this event. Refresh My Applications; a second active role is not allowed.'
                    WHEN v_detail ILIKE '%has started%' THEN 'This role has started. Applying or changing roles is no longer allowed.'
                    WHEN v_detail ILIKE '%start date%' THEN 'This role has no valid application start date. Contact the organisation before applying.'
                    WHEN v_detail ILIKE '%Participation finalized%' THEN 'Your participation is finalized. You cannot take another role in this event.'
                    WHEN v_detail ILIKE '%not selected%' OR v_detail ILIKE '%DECLINED_ROLE%' THEN 'You were not selected for this role. Choose a different open role.'
                    WHEN v_detail ILIKE '%application changed%' OR SQLSTATE = '23505' THEN 'Your application changed on another device or during review. Refresh and check your current role.'
                    WHEN v_detail ILIKE '%screening%' THEN 'Answer every required question, then review your application again.'
                    WHEN v_detail ILIKE '%unavailable%' THEN 'This opportunity is not accepting applications. Refresh to see its latest status.'
                    WHEN v_detail ILIKE '%cannot be cancelled%' THEN 'Your current role cannot be changed. Refresh to check its status.'
                    ELSE 'The server could not complete this request. Your previous role was not cancelled by this attempt. Refresh and contact the organiser if this continues.'
                END);
    END;
    INSERT INTO v1_erd_test.volunteer_application_receipts_v1
        (auth_user_id, request_id, post_id, role_id, request_digest, result)
    VALUES (auth.uid(), p_request_id, p_post_id, p_role_id, v_digest, v_result);
    RETURN v_result;
END;
$body$;
REVOKE ALL ON FUNCTION v1_erd_test.volunteer_application_action_v1(uuid,text,text,jsonb,text,text,text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION v1_erd_test.volunteer_application_action_v1(uuid,text,text,jsonb,text,text,text) TO authenticated;
COMMIT;
