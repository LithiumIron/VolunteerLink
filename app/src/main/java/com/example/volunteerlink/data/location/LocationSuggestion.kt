package com.example.volunteerlink.data.location

data class LocationSuggestion(
    val displayName: String,
    val city: String?,
    val state: String?,
    val country: String?,
    val latitude: Double,
    val longitude: Double
)