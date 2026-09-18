package com.example.checkpoint.data.local.model

import com.example.checkpoint.data.local.entity.UserEntity

data class GeoUser(
    val user: UserEntity,
    val distanceKm: Double
)
