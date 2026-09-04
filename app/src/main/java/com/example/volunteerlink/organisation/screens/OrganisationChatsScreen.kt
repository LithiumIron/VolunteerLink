package com.example.volunteerlink.organisation.screens

// ============================================================================
// DETAILED FILE RESPONSIBILITY
// ============================================================================
// Implements the Organisation UI associated with Organisation Chats Screen.
//
// The composable layer is responsible for layout, interaction and displaying loading/error/validation state;
// business rules and persistence are delegated to ViewModels/repositories.
//
// This separation makes it clear during maintenance which code changes appearance versus which code changes real
// server data.
//
// Where the screen displays cached information, server-changing actions remain disabled or routed through a fresh
// authenticated repository operation.
//
// Architectural layer: Compose presentation layer.
// ============================================================================


import androidx.compose.runtime.Composable
import com.example.volunteerlink.organisation.components.OrganisationModulePage

/** Organisation conversations page. */
@Composable
/**
 * DETAILED BEHAVIOUR — OrganisationChatsScreen
 *
 * Renders the Organisation Chats screen from state supplied by the owning ViewModel/repository-facing
 * coordinator.
 *
 * The composable maps state to Material3 UI and emits callbacks; it does not become the source of truth for
 * persisted VolunteerLink data.
 */
fun OrganisationChatsScreen() {
    OrganisationModulePage(
        title = "Chats",
        message = "This module will be connected during group integration."
    )
}
