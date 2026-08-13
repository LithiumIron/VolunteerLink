VOLUNTEER OPPORTUNITY SUPABASE INTEGRATION
==========================================

What is connected
-----------------
1. Supabase email/password sign-in for volunteers.
2. Published organisations, posts, physical/remote details, roles,
   schedules, skills and Skill Path progress.
3. Current volunteer's own application history through RLS.
4. Secure submit_role_application RPC.
5. Secure cancel_my_application RPC.
6. Optional privacy-safe aggregate opportunity metrics RPC.

One database step
-----------------
Run this file once in Supabase SQL Editor:

supabase/volunteer_opportunity_metrics.sql

The app has a safe fallback if this optional function has not been run, but
application totals and remaining places are more accurate after installation.

Demo sign-in
------------
The email is pre-filled in the app:
volunteer.demo.2026@example.com

The password is intentionally NOT stored in source code. Enter the demo
password that was used when the Supabase Auth user was created.

Verification flow
-----------------
1. Sync Gradle.
2. Run: .\gradlew.bat assembleDebug
3. Launch app > Continue as Volunteer.
4. Sign in with the volunteer demo account.
5. Confirm 3 published opportunities and 2 applications are visible.
6. Open Skill Path and confirm the volunteer's progress is visible.
7. Submit a role and verify it appears in My Applications.
8. Cancel a PENDING application and verify its status changes to Cancelled.
