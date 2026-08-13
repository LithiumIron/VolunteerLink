package com.example.volunteerlink.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volunteerlink.data.VolunteerSkillPathRepository
import com.example.volunteerlink.model.VolunteerSkillPath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VolunteerSkillPathUiState(
    val isLoading: Boolean = true,
    val skillPaths: List<VolunteerSkillPath> =
        emptyList(),
    val errorMessage: String? = null
)

class VolunteerSkillPathViewModel : ViewModel() {

    private val mutableUiState =
        MutableStateFlow(
            VolunteerSkillPathUiState()
        )

    val uiState: StateFlow<VolunteerSkillPathUiState> =
        mutableUiState.asStateFlow()

    init {
        loadSkillPaths()
    }

    fun retry() {
        loadSkillPaths(
            forceRefresh = true
        )
    }

    private fun loadSkillPaths(
        forceRefresh: Boolean = false
    ) {
        viewModelScope.launch {
            mutableUiState.update {
                    currentUiState ->
                currentUiState.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            try {
                val skillPaths =
                    VolunteerSkillPathRepository
                        .getSkillPaths(
                            forceRefresh =
                                forceRefresh
                        )

                mutableUiState.value =
                    VolunteerSkillPathUiState(
                        isLoading = false,
                        skillPaths = skillPaths
                    )
            } catch (exception: Exception) {
                exception.printStackTrace()

                mutableUiState.value =
                    VolunteerSkillPathUiState(
                        isLoading = false,
                        errorMessage =
                            "Skill Path data could not be loaded from Supabase. " +
                                    "Check the connection and table read policies."
                    )
            }
        }
    }
}
