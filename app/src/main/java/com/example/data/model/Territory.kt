package com.example.data.model

data class Territory(
    val id: String,
    val name: String,
    val ownerId: String,
    val ownerName: String,
    val ownerColorHex: String,
    val points: List<GeoPoint>,
    val areaSqMeters: Double,
    val perimeterMeters: Double,
    val capturedAt: Long,
    val avgPaceMinPerKm: Double,
    val defenseLevel: Int = 1,
    val sectorName: String = "Distrito Central",
    val isUserOwned: Boolean = false,
    val isPublic: Boolean = true
) {
    val formattedArea: String
        get() = if (areaSqMeters >= 10000) {
            String.format("%.2f ha", areaSqMeters / 10000.0)
        } else {
            String.format("%.0f m²", areaSqMeters)
        }

    val formattedPace: String
        get() {
            val mins = avgPaceMinPerKm.toInt()
            val secs = ((avgPaceMinPerKm - mins) * 60).toInt()
            return String.format("%d:%02d /km", mins, secs.coerceIn(0, 59))
        }
}
