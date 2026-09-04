package com.example.volunteerlink.organisation.repository

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Defines every server operation required after an organisation opens a single Volunteer Post for management.
//
// The interface covers post detail loading, applicant decisions, shortlist state, Physical attendance/review,
// Remote submission review, publishing saved drafts and downloadable files.
//
// Separating this contract from the Supabase implementation keeps OrganisationPostManagementViewModel focused on
// workflow and UI state rather than database syntax.
//
// Architectural layer: Data/repository layer.
// ============================================================================


import com.example.volunteerlink.organisation.manage.model.PostManagementAttendanceSnapshot
import com.example.volunteerlink.organisation.manage.model.PostManagementPost
import com.example.volunteerlink.organisation.manage.model.PostManagementPendingReviewDecision
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteMissingDecision
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteSubmissionDecision

/** Loads and updates the normalized data needed to manage one Volunteer Post. */
/**
 * DETAILED DECLARATION — OrganisationPostManagementRepository
 *
 * Contract for Organisation Post Management Repository. Callers depend on this abstraction rather than a
 * concrete Supabase implementation.
 *
 * Implementations may perform network/storage work, while ViewModels and Compose remain expressed in
 * VolunteerLink domain types.
 */
interface OrganisationPostManagementRepository {
    /**
     * Loads the post needed by the organisation Manage Post flow.
     * Supabase, RPC and storage details stay here so callers work with VolunteerLink models and results.
     */
    /**
     * DETAILED BEHAVIOUR — loadPost
     *
     * Performs the repository/data-layer operation for load post.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun loadPost(postId: String): PostManagementPost

    /** Publishes one complete saved Draft after server-side ownership and 7-day checks. */
    /**
     * DETAILED BEHAVIOUR — publishSavedDraft
     *
     * Performs the repository/data-layer operation for publish saved draft.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun publishSavedDraft(postId: String, appNowMillis: Long)

    /**
     * Lightweight read used while Physical attendance is visible.
     * Only attendance_days and attendance_records are fetched.
     */
    /**
     * DETAILED BEHAVIOUR — loadPhysicalAttendance
     *
     * Performs the repository/data-layer operation for load physical attendance.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun loadPhysicalAttendance(postId: String): PostManagementAttendanceSnapshot

    /** Downloads one Remote submission file after confirming this organisation owns the post. */
    /**
     * DETAILED BEHAVIOUR — downloadRemoteSubmission
     *
     * Performs the repository/data-layer operation for download remote submission.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun downloadRemoteSubmission(
        postId: String,
        filePath: String
    ): ByteArray

    /**
     * Reviews one Remote submission while the project is ongoing.
     * During Ongoing review this changes the work-submission status. The ended-project Submission stage later maps accepted/rejected work to final Remote participation outcomes automatically.
     */
    /**
     * DETAILED BEHAVIOUR — reviewRemoteSubmission
     *
     * Performs the repository/data-layer operation for review remote submission.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun reviewRemoteSubmission(
        postId: String,
        submissionId: String,
        action: String,
        feedback: String? = null
    )

    /**
     * Saves the ended Remote Submission stage in one database transaction.
     * Missing Individual work can be finalized as Not Completed while other
     * unresolved work receives one later project deadline.
     */
    /**
     * DETAILED BEHAVIOUR — saveRemoteSubmissionReviewStage
     *
     * Performs the repository/data-layer operation for save remote submission review stage.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun saveRemoteSubmissionReviewStage(
        postId: String,
        decisions: List<PostManagementRemoteSubmissionDecision>,
        missingDecisions: List<PostManagementRemoteMissingDecision>,
        newEndDate: String?
    )

    /**
     * Final Remote close-out. Submission Review has already settled every volunteer
     * as COMPLETED or NOT_COMPLETED; Finish only saves optional feedback, issues
     * Completed evidence, rebuilds progress, and closes the post.
     */
    /**
     * DETAILED BEHAVIOUR — finalizeRemoteReviewBatch
     *
     * Performs the repository/data-layer operation for finalize remote review batch.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun finalizeRemoteReviewBatch(
        postId: String,
        feedbackByParticipation: Map<String, String>
    )

    /** Shortlists or unshortlists one pending application for comparison. */
    /**
     * DETAILED BEHAVIOUR — setApplicantShortlisted
     *
     * Performs the repository/data-layer operation for set applicant shortlisted.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun setApplicantShortlisted(
        postId: String,
        roleTemplateId: String,
        userId: String,
        isShortlisted: Boolean
    )

    /**
     * Accepts or declines one pending REVIEW_APPLICANTS application.
     * A manual decline must include the organisation's reason; it is saved to
     * role_participations.decision_note and is visible to the volunteer.
     */
    /**
     * DETAILED BEHAVIOUR — reviewApplicant
     *
     * Performs the repository/data-layer operation for review applicant.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun reviewApplicant(
        postId: String,
        roleTemplateId: String,
        userId: String,
        decision: String,
        decisionNote: String? = null
    )

    /**
     * Lazily opens today's Physical attendance session.
     * The database function validates ownership, lifecycle, live time window and volunteer count,
     * and returns the existing session rather than generating another PIN.
     */
    /**
     * DETAILED BEHAVIOUR — startPhysicalAttendance
     *
     * Performs the repository/data-layer operation for start physical attendance.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun startPhysicalAttendance(postId: String)

    /** Organisation correction authority: mark an accepted Physical volunteer present. */
    /**
     * DETAILED BEHAVIOUR — markVolunteerPresent
     *
     * Performs the repository/data-layer operation for mark volunteer present.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun markVolunteerPresent(
        postId: String,
        eventDate: String,
        roleTemplateId: String,
        userId: String
    )

    /**
     * Organisation correction authority: persist an explicit ABSENT decision.
     * That decision blocks another volunteer PIN check-in for the same role/date
     * until the organisation explicitly uses Mark Present.
     */
    /**
     * DETAILED BEHAVIOUR — markVolunteerAbsent
     *
     * Performs the repository/data-layer operation for mark volunteer absent.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun markVolunteerAbsent(
        postId: String,
        eventDate: String,
        roleTemplateId: String,
        userId: String
    )


    /** Classifies ended Physical participants into Ready / Needs Review. */
    /**
     * DETAILED BEHAVIOUR — preparePhysicalReview
     *
     * Performs the repository/data-layer operation for prepare physical review.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun preparePhysicalReview(postId: String)

    /** Moves one fully-attended Ready volunteer into Needs Review for a known work issue. */
    /**
     * DETAILED BEHAVIOUR — reportPhysicalReviewIssue
     *
     * Performs the repository/data-layer operation for report physical review issue.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun reportPhysicalReviewIssue(
        postId: String,
        roleTemplateId: String,
        userId: String,
        reason: String
    )

    /** Completes every currently Ready Physical volunteer in one batch. */
    /**
     * DETAILED BEHAVIOUR — completeAllReadyPhysical
     *
     * Performs the repository/data-layer operation for complete all ready physical.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun completeAllReadyPhysical(postId: String)

    /** Finalizes any unresolved Physical volunteer after attendance has been explicitly resolved. */
    /**
     * DETAILED BEHAVIOUR — finalizePhysicalVolunteer
     *
     * Performs the repository/data-layer operation for finalize physical volunteer.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun finalizePhysicalVolunteer(
        postId: String,
        roleTemplateId: String,
        userId: String,
        decision: String,
        note: String?
    )

    /** Saves or edits one feedback group. Group membership is not stored separately. */
    /**
     * DETAILED BEHAVIOUR — savePhysicalFeedback
     *
     * Performs the repository/data-layer operation for save physical feedback.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun savePhysicalFeedback(
        postId: String,
        userIds: List<String>,
        feedback: String,
        replaceExisting: Boolean
    )

    /** Finalizes the whole Physical review after every accepted volunteer has a final decision. */
    /**
     * DETAILED BEHAVIOUR — finalizePhysicalReviewPost
     *
     * Performs the repository/data-layer operation for finalize physical review post.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun finalizePhysicalReviewPost(postId: String)

    /**
     * Atomically commits every temporary Physical review decision and feedback item,
     * then marks the post Completed. No draft rows are stored.
     */
    /**
     * DETAILED BEHAVIOUR — finalizePhysicalReviewBatch
     *
     * Performs the repository/data-layer operation for finalize physical review batch.
     *
     * The caller works with VolunteerLink models while this method is responsible for Supabase request shape,
     * decoding and backend-specific errors.
     */
    suspend fun finalizePhysicalReviewBatch(
        postId: String,
        decisions: List<PostManagementPendingReviewDecision>,
        feedbackByUserId: Map<String, String>
    )
}

