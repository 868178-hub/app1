package com.example.data.model

data class SocialPost(
    val id: String,
    val runnerName: String,
    val runnerInitials: String,
    val runnerColorHex: String,
    val territoryName: String,
    val sectorName: String,
    val areaSqMeters: Double,
    val avgPace: String,
    val durationMinutes: Int,
    val timeAgo: String,
    val likesCount: Int,
    val commentsCount: Int,
    val isLikedByMe: Boolean = false,
    val isLoopClosed: Boolean = true, // Loop completion confirmation
    val stealthProtectionBadge: Boolean = true // Flag confirming privacy during run
) {
    val formattedArea: String
        get() = if (areaSqMeters >= 10000) {
            String.format("%.2f ha", areaSqMeters / 10000.0)
        } else {
            String.format("%.0f m²", areaSqMeters)
        }
}
