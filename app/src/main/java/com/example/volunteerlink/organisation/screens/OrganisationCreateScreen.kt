package com.example.volunteerlink.organisation.screens

import androidx.compose.runtime.Composable
import com.example.volunteerlink.organisation.components.OrganisationModulePage

/** Organisation opportunity-creation entry page. */
@Composable
fun OrganisationCreateScreen() {
    OrganisationModulePage(
        title = "Create",
        message = "This module will be connected during group integration."
    )
}
