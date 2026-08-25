package com.example.data.model

data class GeoPoint(
    val lat: Double,
    val lng: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val speedMps: Float = 0f,
    val altitudeMeters: Double = 0.0
)
