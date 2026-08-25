package com.example.domain.engine

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import com.example.data.model.GeoPoint
import com.example.data.model.RunSession
import com.example.data.model.RunnerProfile
import com.example.data.model.Territory
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

class RunTrackerEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    // Current GPS position of the user - starts null or initial fallback
    private val _currentLocation = MutableStateFlow(GeoPoint(40.4168, -3.7038))
    val currentLocation: StateFlow<GeoPoint> = _currentLocation.asStateFlow()

    // Current active run session
    private val _currentSession = MutableStateFlow(RunSession(id = UUID.randomUUID().toString()))
    val currentSession: StateFlow<RunSession> = _currentSession.asStateFlow()

    // Simulated Run status
    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    // Event when a loop is closed and territory conquered
    private val _conquestEvent = MutableSharedFlow<Territory>(extraBufferCapacity = 1)
    val conquestEvent: SharedFlow<Territory> = _conquestEvent.asSharedFlow()

    // Current user's owned territories for territory expansion detection
    private val _userOwnedTerritories = MutableStateFlow<List<Territory>>(emptyList())
    val userOwnedTerritories: StateFlow<List<Territory>> = _userOwnedTerritories.asStateFlow()

    private var lastConquestIndex = 0

    private var timerJob: Job? = null
    private var simulationJob: Job? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var fusedLocationCallback: LocationCallback? = null
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null

    fun setUserOwnedTerritories(territories: List<Territory>) {
        _userOwnedTerritories.value = territories
    }

    init {
        setupLocationClients()
    }

    private fun setupLocationClients() {
        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

            fusedLocationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    if (!_isSimulating.value) {
                        onNewLocationReceived(
                            GeoPoint(
                                lat = loc.latitude,
                                lng = loc.longitude,
                                timestamp = loc.time,
                                speedMps = loc.speed,
                                altitudeMeters = loc.altitude
                            )
                        )
                    }
                }
            }

            locationListener = LocationListener { location ->
                if (!_isSimulating.value) {
                    onNewLocationReceived(
                        GeoPoint(
                            lat = location.latitude,
                            lng = location.longitude,
                            timestamp = location.time,
                            speedMps = location.speed,
                            altitudeMeters = location.altitude
                        )
                    )
                }
            }

            fetchLastKnownLocation()
            startRealGpsTracking()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchLastKnownLocation() {
        try {
            fusedLocationClient?.lastLocation?.addOnSuccessListener { loc ->
                if (loc != null && !_isSimulating.value) {
                    _currentLocation.value = GeoPoint(
                        lat = loc.latitude,
                        lng = loc.longitude,
                        timestamp = loc.time,
                        speedMps = loc.speed,
                        altitudeMeters = loc.altitude
                    )
                }
            }

            val lm = locationManager ?: return
            val lastGps = try { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (e: SecurityException) { null }
            val lastNetwork = try { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (e: SecurityException) { null }
            val lastPassive = try { lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } catch (e: SecurityException) { null }
            val best = lastGps ?: lastNetwork ?: lastPassive
            best?.let { loc ->
                if (!_isSimulating.value) {
                    _currentLocation.value = GeoPoint(
                        lat = loc.latitude,
                        lng = loc.longitude,
                        timestamp = loc.time,
                        speedMps = loc.speed,
                        altitudeMeters = loc.altitude
                    )
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    fun startRealGpsTracking() {
        try {
            // Priority High Accuracy using Google Play Services Fused Location
            val callback = fusedLocationCallback
            if (callback != null && fusedLocationClient != null) {
                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    1000L
                ).apply {
                    setMinUpdateIntervalMillis(500L)
                    setMinUpdateDistanceMeters(1f)
                    setWaitForAccurateLocation(false)
                }.build()

                fusedLocationClient?.requestLocationUpdates(
                    locationRequest,
                    callback,
                    Looper.getMainLooper()
                )
            }

            // Fallback: Native Android LocationManager GPS & Network Provider
            val listener = locationListener
            val lm = locationManager
            if (listener != null && lm != null) {
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1000L,
                        1f,
                        listener,
                        Looper.getMainLooper()
                    )
                }
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        1500L,
                        2f,
                        listener,
                        Looper.getMainLooper()
                    )
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun stopRealGpsTracking() {
        try {
            fusedLocationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
            locationListener?.let { locationManager?.removeUpdates(it) }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun startRun(isStealth: Boolean) {
        val newId = UUID.randomUUID().toString()
        lastConquestIndex = 0
        _currentSession.value = RunSession(
            id = newId,
            startTime = System.currentTimeMillis(),
            isRunning = true,
            isPaused = false,
            isStealthActive = isStealth,
            trailPoints = listOf(_currentLocation.value)
        )

        startRealGpsTracking()
        startTimer()
    }

    fun pauseRun() {
        _currentSession.value = _currentSession.value.copy(isPaused = true)
    }

    fun resumeRun() {
        _currentSession.value = _currentSession.value.copy(isPaused = false)
    }

    fun finishRun(): RunSession {
        stopTimer()
        stopSimulation()
        stopRealGpsTracking()
        val finished = _currentSession.value.copy(
            endTime = System.currentTimeMillis(),
            isRunning = false,
            isPaused = false
        )
        _currentSession.value = RunSession(id = UUID.randomUUID().toString(), isRunning = false)
        return finished
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000L)
                val session = _currentSession.value
                if (session.isRunning && !session.isPaused) {
                    val newSeconds = session.durationSeconds + 1
                    // Approximate calories: ~60 kcal per km + base metabolic rate
                    val km = session.distanceMeters / 1000.0
                    val cal = (km * 62.0 + (newSeconds * 0.05)).toInt()
                    
                    val pace = if (km > 0.05) {
                        (newSeconds / 60.0) / km
                    } else 0.0

                    _currentSession.value = session.copy(
                        durationSeconds = newSeconds,
                        caloriesBurned = cal,
                        currentPaceMinPerKm = pace,
                        avgSpeedKmh = if (newSeconds > 0) (km / (newSeconds / 3600.0)) else 0.0
                    )
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun onNewLocationReceived(point: GeoPoint) {
        _currentLocation.value = point
        val session = _currentSession.value
        if (!session.isRunning || session.isPaused) return

        val trail = session.trailPoints.toMutableList()
        val lastPoint = trail.lastOrNull()
        
        var addedDistance = 0.0
        if (lastPoint != null) {
            addedDistance = GeoMath.distanceMeters(lastPoint, point)
            // Filter noise or jitter: only append if moved at least 2.5 meters
            if (addedDistance < 2.5) return
        }

        trail.add(point)
        val newDistance = session.distanceMeters + addedDistance

        // 1. If runner already owns territories, check if they exited an owned territory into neutral ground and touched ANY owned territory!
        var conquestTriggered = false
        val owned = _userOwnedTerritories.value
        if (owned.isNotEmpty()) {
            val expansionResult = GeoMath.detectTerritoryExpansion(
                trail = trail,
                userTerritories = owned,
                startIndex = lastConquestIndex,
                touchDistanceThresholdMeters = 35.0,
                minOutsidePoints = 4,
                minOutsideDistanceMeters = 20.0,
                minAreaThresholdSqM = 50.0
            )

            if (expansionResult != null) {
                lastConquestIndex = expansionResult.loopEndIndex
                handleLoopClosed(expansionResult, session)
                conquestTriggered = true
            }
        }

        // 2. If no territory expansion triggered, check classic closed-loop circuit (e.g. initial conquest starting from unowned ground)
        if (!conquestTriggered) {
            val uncompletedTrail = if (lastConquestIndex > 0 && lastConquestIndex < trail.size) {
                trail.subList(lastConquestIndex, trail.size)
            } else trail

            val loopResult = GeoMath.detectLoopClosure(
                trail = uncompletedTrail,
                closeDistanceThresholdMeters = 38.0,
                minPointsInLoop = 6,
                minAreaThresholdSqM = 100.0
            )

            if (loopResult != null) {
                lastConquestIndex = (if (lastConquestIndex > 0) lastConquestIndex else 0) + loopResult.loopEndIndex
                handleLoopClosed(loopResult, session)
            }
        }

        _currentSession.value = session.copy(
            trailPoints = trail,
            distanceMeters = newDistance
        )
    }

    private fun handleLoopClosed(
        loopResult: GeoMath.ClosedLoopResult,
        session: RunSession
    ) {
        val territory = Territory(
            id = UUID.randomUUID().toString(),
            name = "Zona Conquistada #${(session.conqueredTerritories.size + 1)}",
            ownerId = "user_me",
            ownerName = "Tú",
            ownerColorHex = "#00E5FF",
            points = loopResult.polygonPoints,
            areaSqMeters = loopResult.areaSqMeters,
            perimeterMeters = loopResult.perimeterMeters,
            capturedAt = System.currentTimeMillis(),
            avgPaceMinPerKm = if (session.currentPaceMinPerKm > 0) session.currentPaceMinPerKm else 4.8,
            defenseLevel = 1,
            sectorName = "Sector Ciudad",
            isUserOwned = true,
            isPublic = true
        )

        val updatedConquests = session.conqueredTerritories + territory
        _currentSession.value = session.copy(
            conqueredTerritories = updatedConquests
        )

        scope.launch {
            _conquestEvent.emit(territory)
        }
    }

    /**
     * Test runner simulator that runs a realistic city block loop.
     * Perfect for testing loop closure, territory conquest animations, and stealth mode!
     */
    fun toggleSimulation(userProfile: RunnerProfile) {
        if (_isSimulating.value) {
            stopSimulation()
        } else {
            startSimulation(userProfile)
        }
    }

    fun startSimulation(userProfile: RunnerProfile) {
        _isSimulating.value = true
        if (!_currentSession.value.isRunning) {
            startRun(isStealth = userProfile.stealthModeEnabled)
        }

        val startLat = _currentLocation.value.lat
        val startLng = _currentLocation.value.lng

        // Define a nice rectangular/polygon city block loop path with 16 waypoints
        val deltaLat = 0.0018 // ~200 meters
        val deltaLng = 0.0024 // ~200 meters

        val simulatedWaypoints = listOf(
            GeoPoint(startLat, startLng),
            GeoPoint(startLat + deltaLat * 0.35, startLng),
            GeoPoint(startLat + deltaLat * 0.70, startLng),
            GeoPoint(startLat + deltaLat, startLng),
            GeoPoint(startLat + deltaLat, startLng + deltaLng * 0.35),
            GeoPoint(startLat + deltaLat, startLng + deltaLng * 0.70),
            GeoPoint(startLat + deltaLat, startLng + deltaLng),
            GeoPoint(startLat + deltaLat * 0.65, startLng + deltaLng),
            GeoPoint(startLat + deltaLat * 0.35, startLng + deltaLng),
            GeoPoint(startLat, startLng + deltaLng),
            GeoPoint(startLat, startLng + deltaLng * 0.65),
            GeoPoint(startLat, startLng + deltaLng * 0.35),
            GeoPoint(startLat, startLng) // Closes the circuit!
        )

        simulationJob?.cancel()
        simulationJob = scope.launch(Dispatchers.Default) {
            var stepIndex = 0
            while (isActive && _isSimulating.value) {
                val targetPoint = simulatedWaypoints[stepIndex % simulatedWaypoints.size]
                // Interpolate 5 micro-steps towards target for smooth 60fps breadcrumb trail
                val current = _currentLocation.value
                for (micro in 1..5) {
                    if (!isActive || !_isSimulating.value) break
                    val fraction = micro / 5.0
                    val interpLat = current.lat + (targetPoint.lat - current.lat) * fraction
                    val interpLng = current.lng + (targetPoint.lng - current.lng) * fraction
                    
                    onNewLocationReceived(
                        GeoPoint(
                            lat = interpLat,
                            lng = interpLng,
                            timestamp = System.currentTimeMillis(),
                            speedMps = 3.2f // ~11.5 km/h
                        )
                    )
                    delay(450L)
                }
                stepIndex++
                delay(300L)
            }
        }
    }

    fun stopSimulation() {
        _isSimulating.value = false
        simulationJob?.cancel()
        simulationJob = null
    }

    /**
     * Force conquer immediate loop around current trail for quick instant testing!
     */
    fun forceCloseCurrentLoop() {
        val session = _currentSession.value
        if (session.trailPoints.size < 3) {
            // Generate a small loop around current position
            val lat = _currentLocation.value.lat
            val lng = _currentLocation.value.lng
            val syntheticLoop = listOf(
                GeoPoint(lat, lng),
                GeoPoint(lat + 0.0012, lng - 0.0006),
                GeoPoint(lat + 0.0015, lng + 0.0010),
                GeoPoint(lat + 0.0003, lng + 0.0014),
                GeoPoint(lat - 0.0004, lng + 0.0004),
                GeoPoint(lat, lng)
            )
            val area = GeoMath.calculatePolygonAreaSqMeters(syntheticLoop)
            val perimeter = GeoMath.pathLengthMeters(syntheticLoop)
            handleLoopClosed(
                GeoMath.ClosedLoopResult(
                    polygonPoints = syntheticLoop,
                    areaSqMeters = area,
                    perimeterMeters = perimeter,
                    loopStartIndex = 0,
                    loopEndIndex = syntheticLoop.size - 1
                ),
                session
            )
        } else {
            val closedPoints = session.trailPoints.toMutableList()
            if (closedPoints.first() != closedPoints.last()) {
                closedPoints.add(closedPoints.first())
            }
            val area = GeoMath.calculatePolygonAreaSqMeters(closedPoints).coerceAtLeast(3500.0)
            val perimeter = GeoMath.pathLengthMeters(closedPoints).coerceAtLeast(240.0)
            handleLoopClosed(
                GeoMath.ClosedLoopResult(
                    polygonPoints = closedPoints,
                    areaSqMeters = area,
                    perimeterMeters = perimeter,
                    loopStartIndex = 0,
                    loopEndIndex = closedPoints.size - 1
                ),
                session
            )
        }
    }
}
