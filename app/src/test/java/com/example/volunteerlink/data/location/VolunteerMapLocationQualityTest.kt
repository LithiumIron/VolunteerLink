package com.example.volunteerlink.data.location

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VolunteerMapLocationQualityTest {
    @Test fun rejectsOldOrFutureDatedCoordinates() {
        assertFalse(isUsableMapFix(60_001L, 20f, 5.4, 100.4))
        assertFalse(isUsableMapFix(-1L, 20f, 5.4, 100.4))
    }
    @Test fun rejectsInvalidOrVeryBroadAccuracy() {
        assertFalse(isUsableMapFix(0L, Float.NaN, 5.4, 100.4))
        assertFalse(isUsableMapFix(0L, 0f, 5.4, 100.4))
        assertFalse(isUsableMapFix(0L, 3_001f, 5.4, 100.4))
        assertFalse(isUsableMapFix(0L, 20f, 95.0, 100.4))
    }
    @Test fun acceptsRecentCoordinatesWithReportedAccuracy() {
        assertTrue(isUsableMapFix(5_000L, 20f, 5.4, 100.4))
        assertTrue(isUsableMapFix(60_000L, 3_000f, 5.4, 100.4))
    }
}
