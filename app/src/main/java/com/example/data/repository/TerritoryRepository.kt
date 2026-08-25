package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.RunSessionEntity
import com.example.data.local.TerritoryEntity
import com.example.data.model.GeoPoint
import com.example.data.model.LeaderboardRunner
import com.example.data.model.RunSession
import com.example.data.model.RunnerProfile
import com.example.data.model.Sector
import com.example.data.model.SocialPost
import com.example.data.model.Territory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class TerritoryRepository(
    private val database: AppDatabase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val territoryDao = database.territoryDao()
    private val runSessionDao = database.runSessionDao()

    // Runner Profile State
    private val _userProfile = MutableStateFlow(
        RunnerProfile(
            id = "user_me",
            username = "Runner_Alpha",
            runnerTag = "Sector Dominator",
            level = 14,
            xp = 8750,
            nextLevelXp = 10000,
            signatureColorHex = "#00E5FF", // Neon Cyan
            totalConqueredSqMeters = 184500.0,
            totalDistanceMeters = 142300.0,
            totalRuns = 38,
            globalRank = 3,
            stealthModeEnabled = true, // Live Ghost privacy mode
            hideHomeRadiusMeters = 200,
            autoPublishLoops = true
        )
    )
    val userProfile: StateFlow<RunnerProfile> = _userProfile.asStateFlow()

    // Social Feed State
    private val _socialPosts = MutableStateFlow<List<SocialPost>>(emptyList())
    val socialPosts: StateFlow<List<SocialPost>> = _socialPosts.asStateFlow()

    // Sectors State
    private val _sectors = MutableStateFlow<List<Sector>>(emptyList())
    val sectors: StateFlow<List<Sector>> = _sectors.asStateFlow()

    // Friends State
    private val _friends = MutableStateFlow<List<com.example.data.model.FriendRunner>>(
        listOf(
            com.example.data.model.FriendRunner(
                id = "friend_1",
                name = "Elena Moreno",
                runnerCode = "#RUN-1982",
                colorHex = "#FF3366",
                level = 19,
                conqueredAreaKm2 = 3.12,
                isOnline = true,
                territoriesCount = 14
            ),
            com.example.data.model.FriendRunner(
                id = "friend_2",
                name = "Marcos Navarro",
                runnerCode = "#RUN-4412",
                colorHex = "#FF9F1C",
                level = 16,
                conqueredAreaKm2 = 2.45,
                isOnline = false,
                territoriesCount = 9
            ),
            com.example.data.model.FriendRunner(
                id = "friend_3",
                name = "Sara Gómez",
                runnerCode = "#RUN-7731",
                colorHex = "#A855F7",
                level = 13,
                conqueredAreaKm2 = 1.68,
                isOnline = true,
                territoriesCount = 6
            )
        )
    )
    val friends: StateFlow<List<com.example.data.model.FriendRunner>> = _friends.asStateFlow()

    init {
        scope.launch {
            try {
                seedInitialTerritoriesIfEmpty()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            initSectors()
            initSocialFeed()
        }
    }

    val allTerritories: Flow<List<Territory>> = territoryDao.getAllTerritories().map { list ->
        list.map { it.toDomainModel() }
    }

    val userTerritories: Flow<List<Territory>> = territoryDao.getUserTerritories().map { list ->
        list.map { it.toDomainModel() }
    }

    val allRunSessions: Flow<List<RunSessionEntity>> = runSessionDao.getAllSessions()

    suspend fun saveConqueredTerritory(territory: Territory) {
        territoryDao.insertTerritory(TerritoryEntity.fromDomain(territory))
        // Update user stats
        val current = _userProfile.value
        val newTotalArea = current.totalConqueredSqMeters + territory.areaSqMeters
        val newTotalRuns = current.totalRuns + (if (territory.isUserOwned) 1 else 0)
        val newXp = current.xp + (territory.areaSqMeters / 10).toInt().coerceAtLeast(150)
        val newLevel = if (newXp >= current.nextLevelXp) current.level + 1 else current.level
        val newNextXp = if (newXp >= current.nextLevelXp) current.nextLevelXp + 3000 else current.nextLevelXp

        _userProfile.value = current.copy(
            totalConqueredSqMeters = newTotalArea,
            totalRuns = newTotalRuns,
            xp = newXp,
            level = newLevel,
            nextLevelXp = newNextXp
        )

        // If public & loop closed, add to social feed
        if (territory.isPublic) {
            val post = SocialPost(
                id = UUID.randomUUID().toString(),
                runnerName = territory.ownerName,
                runnerInitials = territory.ownerName.take(2).uppercase(),
                runnerColorHex = territory.ownerColorHex,
                territoryName = territory.name,
                sectorName = territory.sectorName,
                areaSqMeters = territory.areaSqMeters,
                avgPace = territory.formattedPace,
                durationMinutes = (territory.perimeterMeters / 150).toInt().coerceAtLeast(5),
                timeAgo = "Hace unos momentos",
                likesCount = 1,
                commentsCount = 0,
                isLoopClosed = true,
                stealthProtectionBadge = true
            )
            _socialPosts.value = listOf(post) + _socialPosts.value
        }
    }

    suspend fun saveRunSession(session: RunSession) {
        val entity = RunSessionEntity(
            id = session.id,
            startTime = session.startTime,
            endTime = session.endTime,
            distanceMeters = session.distanceMeters,
            durationSeconds = session.durationSeconds,
            avgSpeedKmh = session.avgSpeedKmh,
            caloriesBurned = session.caloriesBurned,
            trailPoints = session.trailPoints,
            conqueredAreaSqM = session.totalConqueredArea,
            isStealthMode = session.isStealthActive
        )
        runSessionDao.insertSession(entity)

        // Update runner total km
        val current = _userProfile.value
        _userProfile.value = current.copy(
            totalDistanceMeters = current.totalDistanceMeters + session.distanceMeters
        )
    }

    fun updateProfile(updated: RunnerProfile) {
        _userProfile.value = updated
    }

    fun loginWithGoogle(accountName: String, accountEmail: String) {
        _userProfile.value = _userProfile.value.copy(
            username = accountName.ifBlank { "Corredor Google" },
            email = accountEmail,
            authProvider = "GOOGLE",
            isLoggedIn = true
        )
    }

    fun loginWithEmail(email: String, name: String) {
        _userProfile.value = _userProfile.value.copy(
            username = name.ifBlank { email.substringBefore("@") },
            email = email,
            authProvider = "EMAIL",
            isLoggedIn = true
        )
    }

    fun logout() {
        _userProfile.value = _userProfile.value.copy(
            email = null,
            authProvider = "GUEST",
            isLoggedIn = false
        )
    }

    fun addFriendByCode(code: String): Boolean {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isBlank()) return false
        val newFriend = com.example.data.model.FriendRunner(
            id = "friend_${System.currentTimeMillis()}",
            name = "Corredor $cleanCode",
            runnerCode = cleanCode,
            colorHex = "#10B981",
            level = (8..18).random(),
            conqueredAreaKm2 = (1..5).random() + 0.45,
            isOnline = true,
            territoriesCount = (4..12).random()
        )
        _friends.value = _friends.value + newFriend
        return true
    }

    fun toggleStealthMode(enabled: Boolean) {
        _userProfile.value = _userProfile.value.copy(stealthModeEnabled = enabled)
    }

    fun setSignatureColor(colorHex: String) {
        _userProfile.value = _userProfile.value.copy(signatureColorHex = colorHex)
    }

    fun toggleLikePost(postId: String) {
        _socialPosts.value = _socialPosts.value.map { post ->
            if (post.id == postId) {
                val newLiked = !post.isLikedByMe
                val newCount = if (newLiked) post.likesCount + 1 else (post.likesCount - 1).coerceAtLeast(0)
                post.copy(isLikedByMe = newLiked, likesCount = newCount)
            } else post
        }
    }

    fun getLeaderboard(): List<LeaderboardRunner> {
        val user = _userProfile.value
        val runners = mutableListOf(
            LeaderboardRunner(
                rank = 1,
                id = "runner_1",
                name = "Elena 'Flash' Moreno",
                avatarInitials = "EM",
                colorHex = "#FF3366", // Coral
                level = 19,
                territorySqM = 312000.0,
                totalKm = 348.5,
                sectorsControlled = 4
            ),
            LeaderboardRunner(
                rank = 2,
                id = "runner_2",
                name = "Marcos 'Volt' Navarro",
                avatarInitials = "MN",
                colorHex = "#FF9F1C", // Amber
                level = 16,
                territorySqM = 245000.0,
                totalKm = 289.0,
                sectorsControlled = 3
            ),
            LeaderboardRunner(
                rank = user.globalRank,
                id = user.id,
                name = "${user.username} (Tú)",
                avatarInitials = "YO",
                colorHex = user.signatureColorHex,
                level = user.level,
                territorySqM = user.totalConqueredSqMeters,
                totalKm = user.totalDistanceMeters / 1000.0,
                sectorsControlled = 2,
                isUser = true
            ),
            LeaderboardRunner(
                rank = 4,
                id = "runner_4",
                name = "Sara 'Trail' Gómez",
                avatarInitials = "SG",
                colorHex = "#A855F7", // Violet
                level = 13,
                territorySqM = 168000.0,
                totalKm = 195.2,
                sectorsControlled = 1
            ),
            LeaderboardRunner(
                rank = 5,
                id = "runner_5",
                name = "David 'Cyber' Ruiz",
                avatarInitials = "DR",
                colorHex = "#10B981", // Emerald
                level = 11,
                territorySqM = 124000.0,
                totalKm = 142.0,
                sectorsControlled = 1
            ),
            LeaderboardRunner(
                rank = 6,
                id = "runner_6",
                name = "Carla 'Breeze' Vega",
                avatarInitials = "CV",
                colorHex = "#3B82F6", // Blue
                level = 9,
                territorySqM = 88000.0,
                totalKm = 104.3,
                sectorsControlled = 0
            )
        )
        return runners.sortedByDescending { it.territorySqM }.mapIndexed { index, runner ->
            runner.copy(rank = index + 1)
        }
    }

    private fun initSectors() {
        _sectors.value = listOf(
            Sector(
                id = "sec_1",
                name = "Parque del Lago & Alameda",
                district = "Distrito Centro",
                totalAreaSqM = 120000.0,
                rulerId = "user_me",
                rulerName = "Runner_Alpha (Tú)",
                rulerColorHex = "#00E5FF",
                rulerAvatarInitials = "YO",
                dominancePercentage = 68,
                status = "Dominado",
                runnerCount = 19,
                bestPace = "4:12 /km"
            ),
            Sector(
                id = "sec_2",
                name = "Campus Tecnológico e Innovación",
                district = "Distrito Norte",
                totalAreaSqM = 95000.0,
                rulerId = "runner_1",
                rulerName = "Elena 'Flash' Moreno",
                rulerColorHex = "#FF3366",
                rulerAvatarInitials = "EM",
                dominancePercentage = 74,
                status = "Dominado",
                runnerCount = 14,
                bestPace = "4:05 /km"
            ),
            Sector(
                id = "sec_3",
                name = "Paseo de la Ribera & Puentes",
                district = "Distrito Este",
                totalAreaSqM = 160000.0,
                rulerId = "runner_2",
                rulerName = "Marcos 'Volt' Navarro",
                rulerColorHex = "#FF9F1C",
                rulerAvatarInitials = "MN",
                dominancePercentage = 52,
                status = "En disputa",
                runnerCount = 28,
                bestPace = "4:20 /km"
            ),
            Sector(
                id = "sec_4",
                name = "Casco Histórico & Murallas",
                district = "Distrito Sur",
                totalAreaSqM = 80000.0,
                rulerId = "runner_4",
                rulerName = "Sara 'Trail' Gómez",
                rulerColorHex = "#A855F7",
                rulerAvatarInitials = "SG",
                dominancePercentage = 61,
                status = "Conquistado ayer",
                runnerCount = 11,
                bestPace = "4:45 /km"
            )
        )
    }

    private fun initSocialFeed() {
        _socialPosts.value = listOf(
            SocialPost(
                id = "post_1",
                runnerName = "Elena 'Flash' Moreno",
                runnerInitials = "EM",
                runnerColorHex = "#FF3366",
                territoryName = "Manzana del Centro de Negocios",
                sectorName = "Distrito Norte",
                areaSqMeters = 34200.0,
                avgPace = "4:08 /km",
                durationMinutes = 24,
                timeAgo = "Hace 12 min",
                likesCount = 28,
                commentsCount = 4,
                isLoopClosed = true,
                stealthProtectionBadge = true
            ),
            SocialPost(
                id = "post_2",
                runnerName = "Marcos 'Volt' Navarro",
                runnerInitials = "MN",
                runnerColorHex = "#FF9F1C",
                territoryName = "Circuito de la Ribera Sur",
                sectorName = "Distrito Este",
                areaSqMeters = 52100.0,
                avgPace = "4:30 /km",
                durationMinutes = 35,
                timeAgo = "Hace 1 hora",
                likesCount = 42,
                commentsCount = 7,
                isLoopClosed = true,
                stealthProtectionBadge = true
            ),
            SocialPost(
                id = "post_3",
                runnerName = "David 'Cyber' Ruiz",
                runnerInitials = "DR",
                runnerColorHex = "#10B981",
                territoryName = "Bucle de la Plaza de los Naranjos",
                sectorName = "Distrito Centro",
                areaSqMeters = 18600.0,
                avgPace = "5:02 /km",
                durationMinutes = 18,
                timeAgo = "Hace 3 horas",
                likesCount = 15,
                commentsCount = 2,
                isLoopClosed = true,
                stealthProtectionBadge = true
            )
        )
    }

    private suspend fun seedInitialTerritoriesIfEmpty() {
        // Base center coordinates around modern city center (e.g. 40.4168, -3.7038 or local baseline)
        val centerLat = 40.4168
        val centerLng = -3.7038

        // Seed 4 rich pre-existing conquered territories from other runners and yourself
        val baseTerritories = listOf(
            Territory(
                id = "terr_seed_1",
                name = "Sector Alameda & Jardín Central",
                ownerId = "user_me",
                ownerName = "Runner_Alpha (Tú)",
                ownerColorHex = "#00E5FF", // Neon Cyan
                points = listOf(
                    GeoPoint(centerLat + 0.0010, centerLng - 0.0020),
                    GeoPoint(centerLat + 0.0028, centerLng - 0.0018),
                    GeoPoint(centerLat + 0.0032, centerLng + 0.0005),
                    GeoPoint(centerLat + 0.0015, centerLng + 0.0012),
                    GeoPoint(centerLat + 0.0002, centerLng - 0.0005),
                    GeoPoint(centerLat + 0.0010, centerLng - 0.0020)
                ),
                areaSqMeters = 42500.0,
                perimeterMeters = 1180.0,
                capturedAt = System.currentTimeMillis() - 86400000L,
                avgPaceMinPerKm = 4.75,
                defenseLevel = 3,
                sectorName = "Parque del Lago & Alameda",
                isUserOwned = true,
                isPublic = true
            ),
            Territory(
                id = "terr_seed_2",
                name = "Manzana Innovación & Torres",
                ownerId = "runner_1",
                ownerName = "Elena 'Flash' Moreno",
                ownerColorHex = "#FF3366", // Coral
                points = listOf(
                    GeoPoint(centerLat + 0.0040, centerLng + 0.0015),
                    GeoPoint(centerLat + 0.0062, centerLng + 0.0020),
                    GeoPoint(centerLat + 0.0058, centerLng + 0.0048),
                    GeoPoint(centerLat + 0.0035, centerLng + 0.0042),
                    GeoPoint(centerLat + 0.0040, centerLng + 0.0015)
                ),
                areaSqMeters = 56800.0,
                perimeterMeters = 1350.0,
                capturedAt = System.currentTimeMillis() - 43200000L,
                avgPaceMinPerKm = 4.15,
                defenseLevel = 4,
                sectorName = "Campus Tecnológico",
                isUserOwned = false,
                isPublic = true
            ),
            Territory(
                id = "terr_seed_3",
                name = "Paseo Ribera del Río",
                ownerId = "runner_2",
                ownerName = "Marcos 'Volt' Navarro",
                ownerColorHex = "#FF9F1C", // Amber
                points = listOf(
                    GeoPoint(centerLat - 0.0015, centerLng + 0.0010),
                    GeoPoint(centerLat - 0.0005, centerLng + 0.0038),
                    GeoPoint(centerLat - 0.0028, centerLng + 0.0049),
                    GeoPoint(centerLat - 0.0038, centerLng + 0.0022),
                    GeoPoint(centerLat - 0.0015, centerLng + 0.0010)
                ),
                areaSqMeters = 61200.0,
                perimeterMeters = 1420.0,
                capturedAt = System.currentTimeMillis() - 25000000L,
                avgPaceMinPerKm = 4.35,
                defenseLevel = 2,
                sectorName = "Paseo de la Ribera",
                isUserOwned = false,
                isPublic = true
            ),
            Territory(
                id = "terr_seed_4",
                name = "Plaza de los Arcos Históricos",
                ownerId = "runner_4",
                ownerName = "Sara 'Trail' Gómez",
                ownerColorHex = "#A855F7", // Violet
                points = listOf(
                    GeoPoint(centerLat - 0.0035, centerLng - 0.0025),
                    GeoPoint(centerLat - 0.0018, centerLng - 0.0028),
                    GeoPoint(centerLat - 0.0020, centerLng - 0.0050),
                    GeoPoint(centerLat - 0.0042, centerLng - 0.0046),
                    GeoPoint(centerLat - 0.0035, centerLng - 0.0025)
                ),
                areaSqMeters = 38400.0,
                perimeterMeters = 990.0,
                capturedAt = System.currentTimeMillis() - 12000000L,
                avgPaceMinPerKm = 4.85,
                defenseLevel = 3,
                sectorName = "Casco Histórico",
                isUserOwned = false,
                isPublic = true
            )
        )

        territoryDao.insertAll(baseTerritories.map { TerritoryEntity.fromDomain(it) })
    }
}
