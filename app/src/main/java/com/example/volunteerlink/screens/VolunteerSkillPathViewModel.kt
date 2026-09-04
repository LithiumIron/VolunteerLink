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

// Purpose: Handles volunteer skill path ui state as one reusable step in the Volunteer flow.
// Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
// State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
data class VolunteerSkillPathUiState(
    val isLoading: Boolean = true,
    val skillPaths: List<VolunteerSkillPath> =
        emptyList(),
    val errorMessage: String? = null
)

/**
 * Owns the screen state for Skill Path. The screen observes this read-only StateFlow;
 * only this ViewModel starts repository work or changes loading/error values.
 */
class VolunteerSkillPathViewModel : ViewModel() {

    // Private mutable state prevents Composables from changing progress directly.
    private val mutableUiState =
        MutableStateFlow(
            VolunteerSkillPathUiState()
        )

    val uiState: StateFlow<VolunteerSkillPathUiState> =
        mutableUiState.asStateFlow()

    init {
        // Start the first fetch when the ViewModel is created. It survives ordinary
        // recomposition, unlike placing the fetch directly inside the Composable.
        loadSkillPaths()
    }

    // Purpose: Repeats the last failed dashboard load.
    // Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
    // State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
    fun retry() {
        // The Retry button uses exactly the same loading path as the first screen visit.
        loadSkillPaths()
    }

    // Purpose: Handles load skill paths as one reusable step in the Volunteer flow.
    // Usage: Called by the parent Volunteer screen or navigation callback during Compose rendering.
    // State effect: Its callbacks update screen state or navigate; this UI helper does not own database records.
    private fun loadSkillPaths() {
        viewModelScope.launch {
            // Keep old values only while loading; clear the old error so the UI does not
            // show an outdated failure together with a new request.
            mutableUiState.update {
                    currentUiState ->
                currentUiState.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            try {
                // Repository combines verified completion records into the level, role
                // count and service-time values displayed by the Skill Path screens.
                val skillPaths =
                    VolunteerSkillPathRepository
                        .getSkillPaths()

                mutableUiState.value =
                    VolunteerSkillPathUiState(
                        isLoading = false,
                        skillPaths = skillPaths
                    )
            } catch (exception: Exception) {
                // Do not fabricate zero progress after a failed network/policy request.
                // A visible error is safer than telling a volunteer that achievements vanished.
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
