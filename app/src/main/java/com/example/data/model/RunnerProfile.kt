package com.example.data.model

data class RunnerProfile(
    val id: String = "user_me",
    val username: String = "Corredor_Z",
    val runnerTag: String = "Urban Conqueror",
    val email: String? = null,
    val authProvider: String = "GUEST", // "GOOGLE", "EMAIL", "GUEST"
    val isLoggedIn: Boolean = false,
    val runnerCode: String = "#RUN-8492",
    val level: Int = 14,
    val xp: Int = 8450,
    val nextLevelXp: Int = 10000,
    val signatureColorHex: String = "#00E5FF", // Neon Cyan default
    val totalConqueredSqMeters: Double = 142850.0,
    val totalDistanceMeters: Double = 128400.0,
    val totalRuns: Int = 36,
    val globalRank: Int = 4,
    val stealthModeEnabled: Boolean = true, // Hide live location during runs for privacy
    val hideHomeRadiusMeters: Int = 250, // Safe radius around start
    val autoPublishLoops: Boolean = true // Publish only once circle is closed
) {
    val formattedTotalArea: String
        get() = if (totalConqueredSqMeters >= 10000) {
            String.format("%.2f ha", totalConqueredSqMeters / 10000.0)
        } else {
            String.format("%.0f m²", totalConqueredSqMeters)
        }

    val formattedTotalDistance: String
        get() = String.format("%.1f km", totalDistanceMeters / 1000.0)
}

