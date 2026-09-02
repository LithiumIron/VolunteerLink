package com.example.volunteerlink.organisation.repository

import com.example.volunteerlink.organisation.manage.model.PostManagementAttendanceSnapshot
import com.example.volunteerlink.organisation.manage.model.PostManagementPost
import com.example.volunteerlink.organisation.manage.model.PostManagementPendingReviewDecision
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteMissingDecision
import com.example.volunteerlink.organisation.manage.model.PostManagementRemoteSubmissionDecision

/** Loads and updates the normalized data needed to manage one Volunteer Post. */
interface OrganisationPostManagementRepository {
    suspend fun loadPost(postId: String): PostManagementPost

    /**
     * Lightweight read used while Physical attendance is visible.
     * Only attendance_days and attendance_records are fetched.
     */
    suspend fun loadPhysicalAttendance(postId: String): PostManagementAttendanceSnapshot

    /** Downloads one Remote submission file after confirming this organisation owns the post. */
    suspend fun downloadRemoteSubmission(
        postId: String,
        filePath: String
    ): ByteArray

    /**
     * Reviews one Remote submission while the project is ongoing.
     * During Ongoing review this changes the work-submission status. The ended-project Submission stage later maps accepted/rejected work to final Remote participation outcomes automatically.
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
    suspend fun finalizeRemoteReviewBatch(
        postId: String,
        feedbackByParticipation: Map<String, String>
    )

    /** Shortlists or unshortlists one pending application for comparison. */
    suspend fun setApplicantShortlisted(
        postId: String,
        roleTemplateId: String,
        userId: String,
        isShortlisted: Boolean
    )

    /** Accepts or declines one pending REVIEW_APPLICANTS application. */
    suspend fun reviewApplicant(
        postId: String,
        roleTemplateId: String,
        userId: String,
        decision: String
    )

    /**
     * Lazily opens today's Physical attendance session.
     * The database function validates ownership, lifecycle, live time window and volunteer count,
     * and returns the existing session rather than generating another PIN.
     */
    suspend fun startPhysicalAttendance(postId: String)

    /** Organisation correction authority: mark an accepted Physical volunteer present. */
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
    suspend fun markVolunteerAbsent(
        postId: String,
        eventDate: String,
        roleTemplateId: String,
        userId: String
    )


    /** Classifies ended Physical participants into Ready / Needs Review. */
    suspend fun preparePhysicalReview(postId: String)

    /** Moves one fully-attended Ready volunteer into Needs Review for a known work issue. */
    suspend fun reportPhysicalReviewIssue(
        postId: String,
        roleTemplateId: String,
        userId: String,
        reason: String
    )

    /** Completes every currently Ready Physical volunteer in one batch. */
    suspend fun completeAllReadyPhysical(postId: String)

    /** Finalizes any unresolved Physical volunteer after attendance has been explicitly resolved. */
    suspend fun finalizePhysicalVolunteer(
        postId: String,
        roleTemplateId: String,
        userId: String,
        decision: String,
        note: String?
    )

    /** Saves or edits one feedback group. Group membership is not stored separately. */
    suspend fun savePhysicalFeedback(
        postId: String,
        userIds: List<String>,
        feedback: String,
        replaceExisting: Boolean
    )

    /** Finalizes the whole Physical review after every accepted volunteer has a final decision. */
    suspend fun finalizePhysicalReviewPost(postId: String)

    /**
     * Atomically commits every temporary Physical review decision and feedback item,
     * then marks the post Completed. No draft rows are stored.
     */
    suspend fun finalizePhysicalReviewBatch(
        postId: String,
        decisions: List<PostManagementPendingReviewDecision>,
        feedbackByUserId: Map<String, String>
    )
}

