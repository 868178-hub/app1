package com.example.domain.engine

import com.example.data.model.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoMath {
    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates geodesic distance between two points in meters using Haversine formula.
     */
    fun distanceMeters(p1: GeoPoint, p2: GeoPoint): Double {
        val lat1Rad = Math.toRadians(p1.lat)
        val lat2Rad = Math.toRadians(p2.lat)
        val dLat = Math.toRadians(p2.lat - p1.lat)
        val dLng = Math.toRadians(p2.lng - p1.lng)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1Rad) * cos(lat2Rad) * sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Calculates total perimeter or trail length in meters.
     */
    fun pathLengthMeters(points: List<GeoPoint>): Double {
        if (points.size < 2) return 0.0
        var dist = 0.0
        for (i in 0 until points.size - 1) {
            dist += distanceMeters(points[i], points[i + 1])
        }
        return dist
    }

    /**
     * Calculates the enclosed polygon area in square meters using Shoelace formula
     * projected around the centroid latitude.
     */
    fun calculatePolygonAreaSqMeters(points: List<GeoPoint>): Double {
        if (points.size < 3) return 0.0

        val centerLat = points.map { it.lat }.average()
        val centerLng = points.map { it.lng }.average()
        val cosLat = cos(Math.toRadians(centerLat))

        // Convert lat/lng to local Cartesian metric coordinates (x, y)
        val metricPoints = points.map { p ->
            val x = Math.toRadians(p.lng - centerLng) * EARTH_RADIUS_METERS * cosLat
            val y = Math.toRadians(p.lat - centerLat) * EARTH_RADIUS_METERS
            Pair(x, y)
        }

        var area = 0.0
        val n = metricPoints.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            area += metricPoints[i].first * metricPoints[j].second
            area -= metricPoints[j].first * metricPoints[i].second
        }

        return kotlin.math.abs(area) / 2.0
    }

    /**
     * Closed Loop Result containing the detected polygon boundary and metrics.
     */
    data class ClosedLoopResult(
        val polygonPoints: List<GeoPoint>,
        val areaSqMeters: Double,
        val perimeterMeters: Double,
        val loopStartIndex: Int,
        val loopEndIndex: Int
    )

    /**
     * Detects if the runner has closed a circuit/loop around an area.
     * Checks if current point is close to an earlier point in the trail (excluding immediate recent points).
     */
    fun detectLoopClosure(
        trail: List<GeoPoint>,
        closeDistanceThresholdMeters: Double = 35.0,
        minPointsInLoop: Int = 6,
        minAreaThresholdSqM: Double = 100.0
    ): ClosedLoopResult? {
        if (trail.size < minPointsInLoop + 2) return null

        val latest = trail.last()
        val endIndex = trail.size - 1

        // Look back from beginning up to (endIndex - minPointsInLoop)
        // We look for the best closure match
        var bestStartIndex = -1
        var minDistance = Double.MAX_VALUE

        for (i in 0..(endIndex - minPointsInLoop)) {
            val candidate = trail[i]
            val dist = distanceMeters(latest, candidate)
            if (dist <= closeDistanceThresholdMeters && dist < minDistance) {
                minDistance = dist
                bestStartIndex = i
            }
        }

        if (bestStartIndex != -1) {
            val rawLoop = trail.subList(bestStartIndex, trail.size)
            // Close the polygon precisely
            val closedPoints = rawLoop.toMutableList()
            if (closedPoints.first() != closedPoints.last()) {
                closedPoints.add(closedPoints.first())
            }

            val area = calculatePolygonAreaSqMeters(closedPoints)
            if (area >= minAreaThresholdSqM) {
                val perimeter = pathLengthMeters(closedPoints)
                return ClosedLoopResult(
                    polygonPoints = closedPoints,
                    areaSqMeters = area,
                    perimeterMeters = perimeter,
                    loopStartIndex = bestStartIndex,
                    loopEndIndex = endIndex
                )
            }
        }

        return null
    }

    /**
     * Point in Polygon test (Ray Casting algorithm).
     */
    fun isPointInPolygon(point: GeoPoint, polygon: List<GeoPoint>): Boolean {
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val pi = polygon[i]
            val pj = polygon[j]
            if ((pi.lat > point.lat) != (pj.lat > point.lat) &&
                (point.lng < (pj.lng - pi.lng) * (point.lat - pi.lat) / (pj.lat - pi.lat) + pi.lng)
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    /**
     * Calculates distance from point P to line segment AB in meters.
     */
    fun distancePointToSegmentMeters(point: GeoPoint, a: GeoPoint, b: GeoPoint): Double {
        val centerLat = point.lat
        val cosLat = cos(Math.toRadians(centerLat))
        val radFactor = Math.PI / 180.0

        val ax = (a.lng - point.lng) * radFactor * EARTH_RADIUS_METERS * cosLat
        val ay = (a.lat - point.lat) * radFactor * EARTH_RADIUS_METERS

        val bx = (b.lng - point.lng) * radFactor * EARTH_RADIUS_METERS * cosLat
        val by = (b.lat - point.lat) * radFactor * EARTH_RADIUS_METERS

        val dx = bx - ax
        val dy = by - ay
        val lengthSq = dx * dx + dy * dy

        if (lengthSq < 1e-6) {
            return sqrt(ax * ax + ay * ay)
        }

        val t = (-(ax * dx + ay * dy) / lengthSq).coerceIn(0.0, 1.0)
        val projX = ax + t * dx
        val projY = ay + t * dy
        return sqrt(projX * projX + projY * projY)
    }

    /**
     * Calculates the minimum distance in meters from a point to the perimeter of a polygon.
     */
    fun minDistanceToPolygonPerimeter(point: GeoPoint, polygon: List<GeoPoint>): Double {
        if (polygon.size < 2) return Double.MAX_VALUE
        var minD = Double.MAX_VALUE
        for (i in polygon.indices) {
            val nextIdx = (i + 1) % polygon.size
            val d = distancePointToSegmentMeters(point, polygon[i], polygon[nextIdx])
            if (d < minD) minD = d
        }
        return minD
    }

    /**
     * Checks if a point is inside or near the boundary of a polygon (within thresholdMeters).
     */
    fun isPointInsideOrNearPolygon(
        point: GeoPoint,
        polygon: List<GeoPoint>,
        thresholdMeters: Double = 35.0
    ): Boolean {
        if (polygon.size < 3) return false
        if (isPointInPolygon(point, polygon)) return true
        return minDistanceToPolygonPerimeter(point, polygon) <= thresholdMeters
    }

    /**
     * Detects if a runner who owns territories starts in/near an owned territory,
     * leaves it into neutral ground, and returns to ANY point of ANY owned territory.
     * When touched, it automatically completes the closure without having to return
     * to the exact starting point!
     */
    fun detectTerritoryExpansion(
        trail: List<GeoPoint>,
        userTerritories: List<com.example.data.model.Territory>,
        startIndex: Int = 0,
        touchDistanceThresholdMeters: Double = 35.0,
        minOutsidePoints: Int = 4,
        minOutsideDistanceMeters: Double = 22.0,
        minAreaThresholdSqM: Double = 50.0
    ): ClosedLoopResult? {
        if (userTerritories.isEmpty()) return null
        val subTrail = if (startIndex > 0 && startIndex < trail.size) trail.subList(startIndex, trail.size) else trail
        if (subTrail.size < minOutsidePoints + 2) return null

        val latest = subTrail.last()

        // 1. Check if latest point touches or is inside ANY owned territory
        val isTouchingAny = userTerritories.any { territory ->
            isPointInsideOrNearPolygon(latest, territory.points, touchDistanceThresholdMeters)
        }
        if (!isTouchingAny) return null

        // 2. Scan backwards to see if we were out in neutral ground
        var firstOutsideIndex = -1
        var exitFromOwnedIndex = -1
        var maxDistanceToTerritory = 0.0
        var outsideCount = 0

        for (i in (subTrail.size - 2) downTo 0) {
            val pt = subTrail[i]
            val isInsideAny = userTerritories.any { territory ->
                isPointInsideOrNearPolygon(pt, territory.points, touchDistanceThresholdMeters)
            }

            if (!isInsideAny) {
                // Point is in neutral ground
                outsideCount++
                val minDistanceToAny = userTerritories.minOfOrNull { territory ->
                    minDistanceToPolygonPerimeter(pt, territory.points)
                } ?: 0.0
                if (minDistanceToAny > maxDistanceToTerritory) {
                    maxDistanceToTerritory = minDistanceToAny
                }
                firstOutsideIndex = i
            } else {
                // Found the point where the runner was still inside/at the owned territory before venturing out!
                if (outsideCount >= minOutsidePoints) {
                    exitFromOwnedIndex = i
                    break
                } else {
                    // Reset if runner was just lingering inside
                    outsideCount = 0
                    maxDistanceToTerritory = 0.0
                    firstOutsideIndex = -1
                }
            }
        }

        // If the trail started directly outside but connected into owned territory
        if (exitFromOwnedIndex == -1 && firstOutsideIndex == 0 && outsideCount >= minOutsidePoints) {
            exitFromOwnedIndex = 0
        }

        if (exitFromOwnedIndex != -1 && outsideCount >= minOutsidePoints && maxDistanceToTerritory >= minOutsideDistanceMeters) {
            val loopPoints = subTrail.subList(exitFromOwnedIndex, subTrail.size).toMutableList()
            // Connect latest point back to the exit point to seal the closed polygon
            if (loopPoints.first() != loopPoints.last()) {
                loopPoints.add(loopPoints.first())
            }

            val area = calculatePolygonAreaSqMeters(loopPoints)
            if (area >= minAreaThresholdSqM) {
                val perimeter = pathLengthMeters(loopPoints)
                val globalStart = (if (startIndex > 0) startIndex else 0) + exitFromOwnedIndex
                val globalEnd = (if (startIndex > 0) startIndex else 0) + subTrail.size - 1
                return ClosedLoopResult(
                    polygonPoints = loopPoints,
                    areaSqMeters = area,
                    perimeterMeters = perimeter,
                    loopStartIndex = globalStart,
                    loopEndIndex = globalEnd
                )
            }
        }

        return null
    }
}
