-- Volunteer-owned context only. Keeps Organisation RPCs/UI, attendance, dates and clock unchanged.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';
DO $guard$
DECLARE actual text;
BEGIN
    SELECT md5(btrim(replace(prosrc, chr(13), ''), ' ' || chr(9) || chr(10))) INTO actual
    FROM pg_proc WHERE oid = to_regprocedure('v1_erd_test.volunteer_remote_context_v1(text,text)');
    IF actual IS DISTINCT FROM '3c9ebaad4ecea051b655f7764979f7d1' THEN
        RAISE EXCEPTION 'Volunteer Remote context changed or already patched. Nothing applied; review before continuing.';
    END IF;
END $guard$;
CREATE OR REPLACE FUNCTION v1_erd_test.volunteer_remote_context_v1(p_post_id text, p_role_id text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'pg_catalog', 'v1_erd_test', 'pg_temp'
AS $function$
DECLARE
  v_user text;
  v_rp v1_erd_test.role_participations%rowtype;
  v_remote v1_erd_test.remote_details%rowtype;
  v_post v1_erd_test.volunteer_posts%rowtype;
  v_last v1_erd_test.remote_submissions%rowtype;
  v_reason text := '';
  v_requirement text;
  v_history jsonb;
  v_today date := v1_erd_test.volunteer_app_now()::date;
BEGIN
  IF auth.uid() IS NULL THEN RAISE EXCEPTION 'Sign in to view your project submission.'; END IF;
  SELECT user_id INTO v_user FROM v1_erd_test.user_profiles
  WHERE auth_user_id = auth.uid() AND account_type = 'VOLUNTEER';
  IF v_user IS NULL THEN RAISE EXCEPTION 'No volunteer profile is linked to this account.'; END IF;
  SELECT rp.* INTO v_rp FROM v1_erd_test.role_participations rp
  JOIN v1_erd_test.role_templates rt ON rt.role_template_id = rp.role_template_id
  WHERE rp.post_id = p_post_id AND rp.role_template_id = p_role_id AND rp.user_id = v_user
    AND rt.role_mode = 'REMOTE';
  IF NOT FOUND THEN RAISE EXCEPTION 'This Remote participation was not found.'; END IF;
  SELECT * INTO v_post FROM v1_erd_test.volunteer_posts WHERE post_id = p_post_id;
  SELECT * INTO v_remote FROM v1_erd_test.remote_details WHERE post_id = p_post_id;
  IF NOT FOUND OR v_post.mode NOT IN ('REMOTE', 'HYBRID') THEN RAISE EXCEPTION 'This is not a Remote project.'; END IF;

  -- Same latest-version ordering and submission identity as Organisation review.
  SELECT * INTO v_last FROM v1_erd_test.remote_submissions rs
  WHERE rs.post_id = p_post_id AND (
    (v_remote.submission_mode = 'SHARED_TEAM' AND rs.submission_type = 'SHARED') OR
    (v_remote.submission_mode = 'INDIVIDUAL' AND rs.submission_type = 'INDIVIDUAL'
      AND rs.role_template_id = p_role_id AND rs.user_id = v_user))
  ORDER BY rs.submitted_at DESC NULLS LAST, rs.submission_id DESC LIMIT 1;

  IF v_rp.application_status <> 'ACCEPTED' THEN
    v_reason := 'Only accepted volunteers can submit project work.';
  ELSIF v_rp.completion_status NOT IN ('IN_PROGRESS', 'NEEDS_REVIEW') THEN
    v_reason := 'Your participation has been finalized. No further submission is allowed.';
  ELSIF v_post.status NOT IN ('PUBLISHED', 'CLOSED') THEN
    v_reason := 'This project is no longer accepting submissions.';
  ELSIF v_remote.submission_mode = 'SHARED_TEAM' AND p_role_id IS DISTINCT FROM v_remote.responsible_role_template_id THEN
    v_reason := 'The designated responsible role submits the shared team deliverable.';
  ELSIF v_remote.submission_mode NOT IN ('INDIVIDUAL', 'SHARED_TEAM') THEN
    v_reason := 'The project submission mode is not supported.';
  ELSIF v_today < v_remote.start_date THEN
    v_reason := 'Project submissions open on the project start date.';
  ELSIF v_today > coalesce(v_remote.new_end_date, v_remote.end_date) THEN
    v_reason := 'The submission deadline has passed. Wait for the organisation to review or extend it.';
  ELSIF v_last.status = 'PENDING_REVIEW' THEN
    v_reason := 'Your submitted work is awaiting organisation review.';
  ELSIF v_last.status IN ('ACCEPTED', 'NOT_ACCEPTED') THEN
    v_reason := 'This work has been reviewed. Further submission is not allowed.';
  ELSIF v_last.submission_id IS NOT NULL AND v_last.status IS DISTINCT FROM 'REVISION_REQUESTED' THEN
    v_reason := 'Refresh the project before making another submission.';
  END IF;

  SELECT CASE WHEN v_remote.submission_mode = 'SHARED_TEAM' THEN v_remote.shared_deliverable
    ELSE pr.individual_submission_requirement END INTO v_requirement
  FROM v1_erd_test.post_roles pr WHERE pr.post_id = p_post_id AND pr.role_template_id = p_role_id;
  SELECT coalesce(jsonb_agg(jsonb_build_object(
      'submission_id', rs.submission_id, 'file_path', rs.file_path,
      'status', rs.status, 'feedback', rs.feedback, 'submitted_at', rs.submitted_at
    ) ORDER BY rs.submitted_at DESC NULLS LAST, rs.submission_id DESC), '[]'::jsonb)
  INTO v_history FROM (
    SELECT * FROM v1_erd_test.remote_submissions s WHERE s.post_id = p_post_id AND (
      (v_remote.submission_mode = 'SHARED_TEAM' AND s.submission_type = 'SHARED' AND v_rp.application_status = 'ACCEPTED') OR
      (v_remote.submission_mode = 'INDIVIDUAL' AND s.submission_type = 'INDIVIDUAL'
        AND s.role_template_id = p_role_id AND s.user_id = v_user))
    ORDER BY s.submitted_at DESC NULLS LAST, s.submission_id DESC LIMIT 20
  ) rs;
  RETURN jsonb_build_object('can_submit', v_reason = '', 'reason', v_reason,
    'effective_deadline', coalesce(v_remote.new_end_date, v_remote.end_date),
    'submission_mode', v_remote.submission_mode, 'requirement', coalesce(v_requirement, ''),
    'completion_status', v_rp.completion_status, 'history', v_history);
END $function$;
COMMIT;
