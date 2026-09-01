-- V16 remote revision ordering repair. Run THIS file, not migration 06 again.
-- Does not change the shared clock, deadlines, review decisions, files or policies.
-- Only updates our volunteer submission RPC and the exact confirmed pending row.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

-- Refuse to overwrite a function changed since the inspected version.
DO $guard$
DECLARE actual text;
BEGIN
  SELECT prosrc INTO actual FROM pg_proc
  WHERE oid = to_regprocedure('v1_erd_test.volunteer_remote_submit_v1(text,text,uuid,text)');
  IF actual IS NULL OR (
    btrim(replace(actual, chr(13), '')) <> btrim($oldbody$
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
    v_path, NULL, 'PENDING_REVIEW', NULL, clock_timestamp(), NULL, clock_timestamp()
  );
  RETURN jsonb_build_object('submission_id', v_id, 'already_saved', false);
END $oldbody$)
    AND btrim(replace(actual, chr(13), '')) <> btrim($newbody$
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
END $newbody$)
  ) THEN
    RAISE EXCEPTION 'Submission function changed or missing. Nothing applied; stop and review.';
  END IF;
END $guard$;

CREATE OR REPLACE FUNCTION v1_erd_test.volunteer_remote_submit_v1(
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

-- One known upload only: retain its status, file, feedback and real updated_at.
-- Original submitted_at: 2026-09-01 17:50:58.00751+00 (also retained in updated_at).
DO $repair$
DECLARE target v1_erd_test.remote_submissions%rowtype; previous_time timestamptz;
BEGIN
  PERFORM 1 FROM v1_erd_test.volunteer_posts WHERE post_id = 'POST014' FOR UPDATE;
  SELECT * INTO target FROM v1_erd_test.remote_submissions
  WHERE submission_id = 'VRS_f67ef59b-6c8a-41b7-95eb-045fd1292e51' FOR UPDATE;
  IF NOT FOUND OR target.post_id IS DISTINCT FROM 'POST014'
     OR target.role_template_id IS DISTINCT FROM 'ROLE028'
     OR target.user_id IS DISTINCT FROM 'USER005'
     OR target.submission_type IS DISTINCT FROM 'INDIVIDUAL'
     OR target.status IS DISTINCT FROM 'PENDING_REVIEW'
     OR target.reviewed_at IS NOT NULL THEN
    RAISE EXCEPTION 'Target upload changed or was reviewed. Nothing applied; stop and review.';
  END IF;
  SELECT max(submitted_at) INTO previous_time FROM v1_erd_test.remote_submissions
  WHERE post_id = 'POST014' AND role_template_id = 'ROLE028'
    AND user_id = 'USER005' AND submission_type = 'INDIVIDUAL'
    AND submission_id <> target.submission_id;
  IF target.submitted_at = timestamptz '2026-09-01 17:50:58.00751+00' THEN
    IF NOT EXISTS (SELECT 1 FROM v1_erd_test.remote_submissions
      WHERE submission_id = 'SUB010' AND post_id = 'POST014'
        AND role_template_id = 'ROLE028' AND user_id = 'USER005'
        AND submission_type = 'INDIVIDUAL' AND status = 'REVISION_REQUESTED'
        AND submitted_at = timestamptz '2026-09-10 02:30:00+00')
      OR EXISTS (SELECT 1 FROM v1_erd_test.remote_submissions
        WHERE post_id = 'POST014' AND role_template_id = 'ROLE028'
          AND user_id = 'USER005' AND submission_type = 'INDIVIDUAL'
          AND submission_id NOT IN ('SUB010', target.submission_id)) THEN
      RAISE EXCEPTION 'Submission history changed since inspection. Stop and review.';
    END IF;
    IF EXISTS (SELECT 1 FROM v1_erd_test.remote_submissions
      WHERE post_id = 'POST014' AND role_template_id = 'ROLE028'
        AND user_id = 'USER005' AND submission_type = 'INDIVIDUAL'
        AND submission_id <> target.submission_id AND status = 'PENDING_REVIEW') THEN
      RAISE EXCEPTION 'Another pending upload exists. Stop and review; do not reorder multiple uploads.';
    END IF;
    UPDATE v1_erd_test.remote_submissions
    SET submitted_at = greatest(v1_erd_test.volunteer_app_now(), previous_time + interval '1 microsecond')
    WHERE submission_id = target.submission_id;
  ELSIF target.submitted_at <= previous_time OR target.submitted_at IS NULL THEN
    RAISE EXCEPTION 'Target timestamp changed unexpectedly. Stop and review.';
  END IF;
END $repair$;
NOTIFY pgrst, 'reload schema';
COMMIT;

SELECT submission_id, status, submitted_at, updated_at
FROM v1_erd_test.remote_submissions
WHERE post_id = 'POST014' AND role_template_id = 'ROLE028'
  AND user_id = 'USER005' AND submission_type = 'INDIVIDUAL'
ORDER BY submitted_at DESC NULLS LAST, submission_id DESC;
