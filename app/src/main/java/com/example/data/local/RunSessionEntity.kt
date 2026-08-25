package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.GeoPoint

@Entity(tableName = "run_sessions")
data class RunSessionEntity(
    @PrimaryKey val id: String,
    val startTime: Long,
    val endTime: Long?,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val avgSpeedKmh: Double,
    val caloriesBurned: Int,
    val trailPoints: List<GeoPoint>,
    val conqueredAreaSqM: Double,
    val isStealthMode: Boolean
)
