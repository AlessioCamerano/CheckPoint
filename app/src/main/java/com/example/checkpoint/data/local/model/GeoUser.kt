package com.example.checkpoint.data.local.model

import com.example.checkpoint.data.local.entity.UserEntity

/**
 * Modello che rappresenta un utente e la sua distanza calcolata in chilometri.
 */
data class GeoUser(
    val user: UserEntity,
    val distanceKm: Double,
)
