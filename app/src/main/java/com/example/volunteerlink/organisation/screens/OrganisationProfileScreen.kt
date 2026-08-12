package com.example.volunteerlink.organisation.screens

import androidx.compose.runtime.Composable
import com.example.volunteerlink.organisation.components.OrganisationModulePage

/** Organisation profile page. */
@Composable
fun OrganisationProfileScreen() {
    OrganisationModulePage(
        title = "Profile",
        message = "This module will be connected during group integration."
    )
}
