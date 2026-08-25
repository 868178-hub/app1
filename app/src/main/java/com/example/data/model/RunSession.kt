package com.example.data.model

data class RunSession(
    val id: String,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val distanceMeters: Double = 0.0,
    val durationSeconds: Long = 0,
    val avgSpeedKmh: Double = 0.0,
    val currentPaceMinPerKm: Double = 0.0,
    val caloriesBurned: Int = 0,
    val trailPoints: List<GeoPoint> = emptyList(),
    val conqueredTerritories: List<Territory> = emptyList(),
    val isStealthActive: Boolean = true,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false
) {
    val formattedDuration: String
        get() {
            val hours = durationSeconds / 3600
            val minutes = (durationSeconds % 3600) / 60
            val seconds = durationSeconds % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val formattedDistance: String
        get() = String.format("%.2f km", distanceMeters / 1000.0)

    val formattedPace: String
        get() {
            if (currentPaceMinPerKm <= 0 || currentPaceMinPerKm > 30) return "--:--"
            val mins = currentPaceMinPerKm.toInt()
            val secs = ((currentPaceMinPerKm - mins) * 60).toInt()
            return String.format("%d:%02d", mins, secs.coerceIn(0, 59))
        }

    val totalConqueredArea: Double
        get() = conqueredTerritories.sumOf { it.areaSqMeters }

    val formattedTotalArea: String
        get() = if (totalConqueredArea >= 10000) {
            String.format("%.2f ha", totalConqueredArea / 10000.0)
        } else {
            String.format("%.0f m²", totalConqueredArea)
        }
}
