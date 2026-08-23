package com.example.volunteerlink.organisation.repository

import com.example.volunteerlink.organisation.home.model.OrganisationHomeSnapshot

/** Data source used by the Organisation Home ViewModel. */
interface OrganisationHomeRepository {
    suspend fun loadHomeSnapshot(
        organisationId: String
    ): OrganisationHomeSnapshot
}
