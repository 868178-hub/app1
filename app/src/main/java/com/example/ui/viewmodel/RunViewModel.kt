package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.GeoPoint
import com.example.data.model.LeaderboardRunner
import com.example.data.model.RunSession
import com.example.data.model.RunnerProfile
import com.example.data.model.Sector
import com.example.data.model.SocialPost
import com.example.data.model.Territory
import com.example.data.repository.TerritoryRepository
import com.example.domain.engine.RunTrackerEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RunViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = TerritoryRepository(database, viewModelScope)
    private val trackerEngine = RunTrackerEngine(application, viewModelScope)

    val userProfile: StateFlow<RunnerProfile> = repository.userProfile
    val currentLocation: StateFlow<GeoPoint> = trackerEngine.currentLocation
    val currentSession: StateFlow<RunSession> = trackerEngine.currentSession
    val isSimulating: StateFlow<Boolean> = trackerEngine.isSimulating

    val territories: StateFlow<List<Territory>> = repository.allTerritories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sectors: StateFlow<List<Sector>> = repository.sectors
    val socialPosts: StateFlow<List<SocialPost>> = repository.socialPosts
    val friends: StateFlow<List<com.example.data.model.FriendRunner>> = repository.friends

    private val _leaderboard = MutableStateFlow<List<LeaderboardRunner>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardRunner>> = _leaderboard.asStateFlow()

    // Dialog state when a polygon loop is closed
    private val _pendingConquest = MutableStateFlow<Territory?>(null)
    val pendingConquest: StateFlow<Territory?> = _pendingConquest.asStateFlow()

    init {
        refreshLeaderboard()

        // Sync user-owned territories with tracking engine for expansion detection
        viewModelScope.launch {
            territories.collect { list ->
                val userOwned = list.filter { it.isUserOwned }
                trackerEngine.setUserOwnedTerritories(userOwned)
            }
        }

        // Listen to loop closures emitted by the tracker engine
        viewModelScope.launch {
            trackerEngine.conquestEvent.collect { territory ->
                val profile = userProfile.value
                val coloredTerritory = territory.copy(
                    ownerId = profile.id,
                    ownerName = profile.username,
                    ownerColorHex = profile.signatureColorHex,
                    isUserOwned = true
                )
                _pendingConquest.value = coloredTerritory
            }
        }
    }

    fun startRun() {
        val stealth = userProfile.value.stealthModeEnabled
        trackerEngine.startRun(isStealth = stealth)
    }

    fun pauseRun() {
        trackerEngine.pauseRun()
    }

    fun resumeRun() {
        trackerEngine.resumeRun()
    }

    fun finishRun() {
        val finishedSession = trackerEngine.finishRun()
        viewModelScope.launch {
            repository.saveRunSession(finishedSession)
            refreshLeaderboard()
        }
    }

    fun toggleSimulation() {
        trackerEngine.toggleSimulation(userProfile.value)
    }

    fun forceCloseLoop() {
        trackerEngine.forceCloseCurrentLoop()
    }

    fun confirmConquest(customName: String, isPublic: Boolean) {
        val territory = _pendingConquest.value ?: return
        val profile = userProfile.value
        val updated = territory.copy(
            name = customName.ifBlank { "Sector ${profile.username} #${(profile.totalRuns + 1)}" },
            ownerColorHex = profile.signatureColorHex,
            isPublic = isPublic
        )

        viewModelScope.launch {
            repository.saveConqueredTerritory(updated)
            refreshLeaderboard()
            _pendingConquest.value = null
        }
    }

    fun dismissConquestDialog() {
        _pendingConquest.value = null
    }

    fun toggleStealthMode(enabled: Boolean) {
        repository.toggleStealthMode(enabled)
    }

    fun setSignatureColor(colorHex: String) {
        repository.setSignatureColor(colorHex)
    }

    fun toggleLikePost(postId: String) {
        repository.toggleLikePost(postId)
    }

    fun refreshLeaderboard() {
        _leaderboard.value = repository.getLeaderboard()
    }

    fun refreshGpsLocation() {
        trackerEngine.fetchLastKnownLocation()
        trackerEngine.startRealGpsTracking()
    }

    fun loginWithGoogle(name: String, email: String) {
        repository.loginWithGoogle(name, email)
        refreshLeaderboard()
    }

    fun loginWithEmail(email: String, name: String) {
        repository.loginWithEmail(email, name)
        refreshLeaderboard()
    }

    fun logout() {
        repository.logout()
        refreshLeaderboard()
    }

    fun addFriend(code: String): Boolean {
        return repository.addFriendByCode(code)
    }
}

