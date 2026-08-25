package com.example.data.model

data class FriendRunner(
    val id: String,
    val name: String,
    val runnerCode: String,
    val colorHex: String,
    val level: Int,
    val conqueredAreaKm2: Double,
    val isOnline: Boolean = false,
    val territoriesCount: Int = 5
)
