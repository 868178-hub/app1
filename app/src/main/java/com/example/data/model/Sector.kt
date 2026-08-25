package com.example.data.model

data class Sector(
    val id: String,
    val name: String,
    val district: String,
    val totalAreaSqM: Double,
    val rulerId: String,
    val rulerName: String,
    val rulerColorHex: String,
    val rulerAvatarInitials: String,
    val dominancePercentage: Int,
    val status: String = "Dominado", // "Dominado", "En disputa", "Conquistado hoy"
    val runnerCount: Int = 12,
    val bestPace: String = "4:15 /km"
) {
    val formattedArea: String
        get() = if (totalAreaSqM >= 10000) {
            String.format("%.1f ha", totalAreaSqM / 10000.0)
        } else {
            String.format("%.0f m²", totalAreaSqM)
        }
}

data class LeaderboardRunner(
    val rank: Int,
    val id: String,
    val name: String,
    val avatarInitials: String,
    val colorHex: String,
    val level: Int,
    val territorySqM: Double,
    val totalKm: Double,
    val sectorsControlled: Int,
    val isUser: Boolean = false
) {
    val formattedTerritory: String
        get() = if (territorySqM >= 10000) {
            String.format("%.2f ha", territorySqM / 10000.0)
        } else {
            String.format("%.0f m²", territorySqM)
        }
}
