package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.GeoPoint
import com.example.data.model.Territory

@Entity(tableName = "territories")
data class TerritoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ownerId: String,
    val ownerName: String,
    val ownerColorHex: String,
    val points: List<GeoPoint>,
    val areaSqMeters: Double,
    val perimeterMeters: Double,
    val capturedAt: Long,
    val avgPaceMinPerKm: Double,
    val defenseLevel: Int,
    val sectorName: String,
    val isUserOwned: Boolean,
    val isPublic: Boolean
) {
    fun toDomainModel(): Territory = Territory(
        id = id,
        name = name,
        ownerId = ownerId,
        ownerName = ownerName,
        ownerColorHex = ownerColorHex,
        points = points,
        areaSqMeters = areaSqMeters,
        perimeterMeters = perimeterMeters,
        capturedAt = capturedAt,
        avgPaceMinPerKm = avgPaceMinPerKm,
        defenseLevel = defenseLevel,
        sectorName = sectorName,
        isUserOwned = isUserOwned,
        isPublic = isPublic
    )

    companion object {
        fun fromDomain(domain: Territory): TerritoryEntity = TerritoryEntity(
            id = domain.id,
            name = domain.name,
            ownerId = domain.ownerId,
            ownerName = domain.ownerName,
            ownerColorHex = domain.ownerColorHex,
            points = domain.points,
            areaSqMeters = domain.areaSqMeters,
            perimeterMeters = domain.perimeterMeters,
            capturedAt = domain.capturedAt,
            avgPaceMinPerKm = domain.avgPaceMinPerKm,
            defenseLevel = domain.defenseLevel,
            sectorName = domain.sectorName,
            isUserOwned = domain.isUserOwned,
            isPublic = domain.isPublic
        )
    }
}
