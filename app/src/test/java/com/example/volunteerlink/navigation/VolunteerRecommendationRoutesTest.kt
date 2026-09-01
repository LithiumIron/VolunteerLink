package com.example.volunteerlink.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VolunteerRecommendationRoutesTest {
    @Test fun ordinaryDetailsRouteRemainsCompatible() {
        assertEquals("volunteer_opportunity_details/42",
            VolunteerOpportunityNavigationRoutes.createVolunteerOpportunityDetailsRoute(42))
    }
    @Test fun forYouCarriesTheExactEventAndRole() {
        assertEquals("volunteer_opportunity_details/42?recommendedRoleId=7&source=for_you",
            VolunteerOpportunityNavigationRoutes.createVolunteerOpportunityDetailsRoute(42, 7, "for_you"))
    }
    @Test fun skillPathKeepsItsRecommendationSource() {
        assertTrue(VolunteerOpportunityNavigationRoutes.createVolunteerOpportunityDetailsRoute(42, 7, "skill_path")
            .endsWith("source=skill_path"))
    }
}
