package com.example.volunteerlink.organisation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerlink.data.post.PostMode
import com.example.volunteerlink.data.post.PostTimingEvaluator
import com.example.volunteerlink.data.post.PostTimingInput
import com.example.volunteerlink.data.post.RoleApplicationWindowEvaluator
import com.example.volunteerlink.data.post.RoleApplicationWindowInput
import com.example.volunteerlink.data.time.AppClock
import com.example.volunteerlink.organisation.manage.model.OrganisationPostManagementUiState
import com.example.volunteerlink.organisation.manage.model.PostManagementPerson
import com.example.volunteerlink.organisation.manage.model.PostManagementPost
import com.example.volunteerlink.organisation.repository.OrganisationPostManagementRepository
import com.example.volunteerlink.organisation.repository.SupabaseOrganisationPostManagementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** ViewModel for one post opened from the Organisation Manage module. */
class OrganisationPostManagementViewModel : ViewModel() {

    private val repository: OrganisationPostManagementRepository =
        SupabaseOrganisationPostManagementRepository()

    private val _uiState = MutableStateFlow(OrganisationPostManagementUiState())
    val uiState = _uiState.asStateFlow()

    private var loadedPostId: String? = null
    private var cachedPost: PostManagementPost? = null

    init {
        observeAppClock()
    }

    fun load(postId: String) {
        if (loadedPostId == postId && cachedPost != null) return
        loadedPostId = postId
        refresh()
    }

    fun toggleApplicantShortlist(person: PostManagementPerson) {
        if (!person.applicationStatus.equals("PENDING", ignoreCase = true)) return

        val currentPost = cachedPost ?: return
        val newValue = !person.isShortlisted

        viewModelScope.launch {
            try {
                repository.setApplicantShortlisted(
                    postId = currentPost.postId,
                    roleTemplateId = person.roleTemplateId,
                    userId = person.userId,
                    isShortlisted = newValue
                )

                val updatedPost = currentPost.copy(
                    people = currentPost.people.map { currentPerson ->
                        if (
                            currentPerson.userId == person.userId &&
                            currentPerson.roleTemplateId == person.roleTemplateId
                        ) {
                            currentPerson.copy(isShortlisted = newValue)
                        } else {
                            currentPerson
                        }
                    }
                )

                cachedPost = updatedPost
                applyTiming(updatedPost)
            } catch (exception: Exception) {
                Log.e(TAG, "Could not update applicant shortlist.", exception)
            }
        }
    }

    fun refresh() {
        val postId = loadedPostId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                val post = repository.loadPost(postId)
                cachedPost = post
                applyTiming(post)
            } catch (exception: Exception) {
                Log.e(TAG, "Could not load post management data.", exception)
                _uiState.value = OrganisationPostManagementUiState(
                    isLoading = false,
                    errorMessage = exception.message
                        ?: "Unable to load this Volunteer Post."
                )
            }
        }
    }

    private fun observeAppClock() {
        viewModelScope.launch {
            AppClock.state.collect { clockState ->
                if (!clockState.isLoaded) return@collect
                cachedPost?.let(::applyTiming)
            }
        }
    }

    private fun applyTiming(post: PostManagementPost) {
        val nowMillis = AppClock.nowMillis()
        val mode = PostMode.fromDatabaseValue(post.mode)

        val overall = mode?.let {
            PostTimingEvaluator.evaluatePostTiming(
                PostTimingInput(
                    mode = it,
                    physicalStartDate = post.physical?.startDate,
                    physicalEndDate = post.physical?.endDate,
                    remoteStartDate = post.remote?.startDate,
                    remoteEndDate = post.remote?.endDate
                ),
                nowMillis
            )
        }

        val physicalTiming = post.physical?.let { physical ->
            PostTimingEvaluator.evaluatePostTiming(
                PostTimingInput(
                    mode = PostMode.PHYSICAL,
                    physicalStartDate = physical.startDate,
                    physicalEndDate = physical.endDate
                ),
                nowMillis
            )
        }

        val remoteTiming = post.remote?.let { remote ->
            PostTimingEvaluator.evaluatePostTiming(
                PostTimingInput(
                    mode = PostMode.REMOTE,
                    remoteStartDate = remote.startDate,
                    remoteEndDate = remote.endDate
                ),
                nowMillis
            )
        }

        val rolesWithApplicationWindows = post.roles.map { role ->
            val applicationWindow = RoleApplicationWindowEvaluator.evaluate(
                input = RoleApplicationWindowInput(
                    roleMode = role.roleMode,
                    postStatus = post.databaseStatus,
                    physicalStartDate = post.physical?.startDate,
                    remoteStartDate = post.remote?.startDate
                ),
                nowMillis = nowMillis
            )

            role.copy(
                applicationWindowState = applicationWindow.state,
                applicationCutoffDate = applicationWindow.cutoffDate,
                applicationCutoffReason = applicationWindow.cutoffReason
            )
        }

        _uiState.value = OrganisationPostManagementUiState(
            isLoading = false,
            post = post.copy(
                timingState = overall,
                physicalTimingState = physicalTiming,
                remoteTimingState = remoteTiming,
                roles = rolesWithApplicationWindows
            )
        )
    }

    companion object {
        private const val TAG = "OrgPostManagementVM"
    }
}
