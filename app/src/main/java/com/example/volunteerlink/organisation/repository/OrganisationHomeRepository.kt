package com.example.volunteerlink.organisation.repository

import com.example.volunteerlink.organisation.home.model.OrganisationHomeSnapshot

/** Data source used by the Organisation Home ViewModel. */
interface OrganisationHomeRepository {
    suspend fun loadHomeSnapshot(
        organisationId: String
    ): OrganisationHomeSnapshot

    suspend fun loadPartnerPosts(): List<PartnerPostSummary>
}

data class PartnerPostSummary(
    val postId: String,
    val ownerOrganisationName: String,
    val title: String,
    val description: String,
    val mode: String,
    val status: String,
    val startDate: String?,
    val endDate: String?,
    val locationName: String?,
    val contributionSummary: String,
    val isOwner: Boolean = false
)
