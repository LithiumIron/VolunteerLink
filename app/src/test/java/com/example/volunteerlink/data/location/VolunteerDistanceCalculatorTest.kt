package com.example.volunteerlink.data.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VolunteerDistanceCalculatorTest {
    @Test
    fun sameCoordinateReturnsZero() {
        assertEquals(
            0.0,
            VolunteerDistanceCalculator.kilometres(
                fromLatitude = 5.4141,
                fromLongitude = 100.3288,
                toLatitude = 5.4141,
                toLongitude = 100.3288
            ),
            0.0
        )
    }

    @Test
    fun penangToKualaLumpurIsRealistic() {
        val distance = VolunteerDistanceCalculator.kilometres(
            fromLatitude = 5.4141,
            fromLongitude = 100.3288,
            toLatitude = 3.1390,
            toLongitude = 101.6869
        )

        assertEquals(294.4, distance, 0.1)
    }

    @Test
    fun invalidLatitudeIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            VolunteerDistanceCalculator.kilometres(
                fromLatitude = 91.0,
                fromLongitude = 100.0,
                toLatitude = 5.0,
                toLongitude = 100.0
            )
        }
    }
}
