-- Volunteer single-file Remote submissions, 20,000,000 bytes maximum.
-- Run ONCE after reviewing. No existing submissions, clock, dates, rewards or
-- Organisation review functions are updated. Storage setting changes apply to
-- future uploads to remote-submissions only; existing objects are not removed.
BEGIN;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM storage.buckets WHERE id = 'remote-submissions' AND public = false) THEN
    RAISE EXCEPTION 'Expected private remote-submissions bucket. Stop and review.';
  END IF;
  IF EXISTS (SELECT 1 FROM storage.buckets WHERE id = 'remote-submissions'
    AND (file_size_limit IS NOT NULL OR allowed_mime_types IS NOT NULL)) THEN
    RAISE EXCEPTION 'Bucket restrictions changed since the inspected snapshot. Stop and review; do not override them.';
  END IF;
END $$;

CREATE FUNCTION v1_erd_test.volunteer_remote_context_v1(p_post_id text, p_role_id text)
RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER
SET search_path = pg_catalog, v1_erd_test, pg_temp AS $$
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
  IF NOT FOUND OR v_post.mode <> 'REMOTE' THEN RAISE EXCEPTION 'This is not a Remote project.'; END IF;

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
END $$;

-- A narrow Storage INSERT predicate. Paths keep POST ID first so the existing
-- Organisation download policy remains valid: post/authUID/role/requestUUID/file.
CREATE FUNCTION v1_erd_test.volunteer_remote_can_upload_v1(p_name text)
RETURNS boolean LANGUAGE plpgsql SECURITY DEFINER
SET search_path = pg_catalog, v1_erd_test, pg_temp AS $$
DECLARE v_parts text[] := string_to_array(p_name, '/'); v_context jsonb;
BEGIN
  IF auth.uid() IS NULL OR cardinality(v_parts) <> 5 OR v_parts[2] <> auth.uid()::text
    OR v_parts[4] !~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
    OR v_parts[5] !~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,119}$'
    OR lower(v_parts[5]) !~ '\.(pdf|jpg|jpeg|png|doc|docx|xls|xlsx|ppt|pptx)$'
  THEN RETURN false; END IF;
  v_context := v1_erd_test.volunteer_remote_context_v1(v_parts[1], v_parts[3]);
  RETURN coalesce((v_context->>'can_submit')::boolean, false);
EXCEPTION WHEN OTHERS THEN RETURN false;
END $$;

CREATE FUNCTION v1_erd_test.volunteer_remote_can_read_v1(p_name text)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER
SET search_path = pg_catalog, v1_erd_test, pg_temp AS $$
  SELECT auth.uid() IS NOT NULL AND (
    -- Owner can retry an uploaded-but-not-finalized object, without reuploading.
    (cardinality(string_to_array(p_name, '/')) = 5 AND split_part(p_name, '/', 2) = auth.uid()::text)
    OR EXISTS (
      SELECT 1 FROM v1_erd_test.remote_submissions rs
      JOIN v1_erd_test.role_participations rp ON rp.post_id = rs.post_id
      JOIN v1_erd_test.user_profiles up ON up.user_id = rp.user_id
      WHERE rs.file_path = p_name AND up.auth_user_id = auth.uid()
        AND rp.application_status = 'ACCEPTED'
        AND (rs.submission_type = 'SHARED' OR (rs.user_id = rp.user_id AND rs.role_template_id = rp.role_template_id))
    )
  );
$$;

CREATE FUNCTION v1_erd_test.volunteer_remote_submit_v1(
  p_post_id text, p_role_id text, p_request_id uuid, p_file_name text
) RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER
SET search_path = pg_catalog, v1_erd_test, pg_temp AS $$
DECLARE
  v_context jsonb;
  v_path text;
  v_id text := 'VRS_' || p_request_id::text;
  v_user text;
  v_metadata jsonb;
  v_existing v1_erd_test.remote_submissions%rowtype;
  v_size bigint;
BEGIN
  IF auth.uid() IS NULL THEN RAISE EXCEPTION 'Sign in before submitting.'; END IF;
  IF p_request_id IS NULL OR p_file_name IS NULL THEN RAISE EXCEPTION 'A selected file is required.'; END IF;
  SELECT user_id INTO v_user FROM v1_erd_test.user_profiles
    WHERE auth_user_id = auth.uid() AND account_type = 'VOLUNTEER';
  IF v_user IS NULL THEN RAISE EXCEPTION 'A volunteer account is required.'; END IF;
  v_path := p_post_id || '/' || auth.uid()::text || '/' || p_role_id || '/' || p_request_id::text || '/' || p_file_name;

  -- Matches Organisation's post-first lock. One submission can win when two
  -- responsible team members submit together; the other must reload.
  PERFORM 1 FROM v1_erd_test.volunteer_posts WHERE post_id = p_post_id FOR UPDATE;
  IF NOT FOUND THEN RAISE EXCEPTION 'Project not found.'; END IF;
  SELECT * INTO v_existing FROM v1_erd_test.remote_submissions WHERE submission_id = v_id;
  IF FOUND THEN
    IF v_existing.post_id = p_post_id AND v_existing.file_path = v_path THEN
      RETURN jsonb_build_object('submission_id', v_id, 'already_saved', true);
    END IF;
    RAISE EXCEPTION 'Submission identifier mismatch. Reload your project.';
  END IF;

  v_context := v1_erd_test.volunteer_remote_context_v1(p_post_id, p_role_id);
  IF NOT coalesce((v_context->>'can_submit')::boolean, false) THEN
    RAISE EXCEPTION '%', v_context->>'reason';
  END IF;
  IF NOT v1_erd_test.volunteer_remote_can_upload_v1(v_path) THEN RAISE EXCEPTION 'Invalid submission file path.'; END IF;
  SELECT metadata INTO v_metadata FROM storage.objects
    WHERE bucket_id = 'remote-submissions' AND name = v_path;
  IF NOT FOUND THEN RAISE EXCEPTION 'The file has not finished uploading. Retry the submission.'; END IF;
  v_size := (v_metadata->>'size')::bigint;
  IF v_size IS NULL OR v_size <= 0 OR v_size > 20000000 THEN RAISE EXCEPTION 'File must be between 1 byte and 20 MB.'; END IF;
  IF coalesce(v_metadata->>'mimetype', '') <> (CASE lower(split_part(p_file_name, '.', cardinality(string_to_array(p_file_name, '.'))))
    WHEN 'pdf' THEN 'application/pdf' WHEN 'jpg' THEN 'image/jpeg' WHEN 'jpeg' THEN 'image/jpeg'
    WHEN 'png' THEN 'image/png' WHEN 'doc' THEN 'application/msword'
    WHEN 'docx' THEN 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    WHEN 'xls' THEN 'application/vnd.ms-excel'
    WHEN 'xlsx' THEN 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    WHEN 'ppt' THEN 'application/vnd.ms-powerpoint'
    WHEN 'pptx' THEN 'application/vnd.openxmlformats-officedocument.presentationml.presentation'
    ELSE 'INVALID' END) THEN RAISE EXCEPTION 'The file type does not match its extension.';
  END IF;
  INSERT INTO v1_erd_test.remote_submissions (
    submission_id, post_id, role_template_id, user_id, submission_type,
    file_path, submission_url, status, feedback, submitted_at, reviewed_at, updated_at
  ) VALUES (
    v_id, p_post_id,
    CASE WHEN v_context->>'submission_mode' = 'INDIVIDUAL' THEN p_role_id ELSE NULL END,
    CASE WHEN v_context->>'submission_mode' = 'INDIVIDUAL' THEN v_user ELSE NULL END,
    CASE WHEN v_context->>'submission_mode' = 'INDIVIDUAL' THEN 'INDIVIDUAL' ELSE 'SHARED' END,
v_path, NULL, 'PENDING_REVIEW', NULL,
    greatest(
      v1_erd_test.volunteer_app_now(),
      (SELECT max(rs.submitted_at) + interval '1 microsecond'
       FROM v1_erd_test.remote_submissions rs
       WHERE rs.post_id = p_post_id AND (
         (v_context->>'submission_mode' = 'SHARED_TEAM' AND rs.submission_type = 'SHARED') OR
         (v_context->>'submission_mode' = 'INDIVIDUAL' AND rs.submission_type = 'INDIVIDUAL'
          AND rs.role_template_id = p_role_id AND rs.user_id = v_user)))
    ), NULL, clock_timestamp()
  );
  RETURN jsonb_build_object('submission_id', v_id, 'already_saved', false);
END $$;

REVOKE ALL ON FUNCTION v1_erd_test.volunteer_remote_context_v1(text,text) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION v1_erd_test.volunteer_remote_can_upload_v1(text) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION v1_erd_test.volunteer_remote_can_read_v1(text) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION v1_erd_test.volunteer_remote_submit_v1(text,text,uuid,text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION v1_erd_test.volunteer_remote_context_v1(text,text) TO authenticated;
GRANT EXECUTE ON FUNCTION v1_erd_test.volunteer_remote_can_upload_v1(text) TO authenticated;
GRANT EXECUTE ON FUNCTION v1_erd_test.volunteer_remote_can_read_v1(text) TO authenticated;
GRANT EXECUTE ON FUNCTION v1_erd_test.volunteer_remote_submit_v1(text,text,uuid,text) TO authenticated;

CREATE POLICY volunteer_remote_file_insert_v1 ON storage.objects FOR INSERT TO authenticated
WITH CHECK (bucket_id = 'remote-submissions' AND v1_erd_test.volunteer_remote_can_upload_v1(name));
CREATE POLICY volunteer_remote_file_read_v1 ON storage.objects FOR SELECT TO authenticated
USING (bucket_id = 'remote-submissions' AND v1_erd_test.volunteer_remote_can_read_v1(name));
-- No UPDATE or DELETE permission: submitted/reviewed files cannot be overwritten.

UPDATE storage.buckets SET file_size_limit = 20000000, allowed_mime_types = ARRAY[
  'application/pdf', 'image/jpeg', 'image/png', 'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'application/vnd.ms-powerpoint', 'application/vnd.openxmlformats-officedocument.presentationml.presentation'
] WHERE id = 'remote-submissions';

NOTIFY pgrst, 'reload schema';
COMMIT;

SELECT id, public, file_size_limit, allowed_mime_types FROM storage.buckets WHERE id = 'remote-submissions';
