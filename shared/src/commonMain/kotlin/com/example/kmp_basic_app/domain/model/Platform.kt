package com.example.kmp_basic_app.domain.model

data class GpsLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null
)

data class CapturedPhoto(
    val id: Long = 0,
    val filePath: String,
    val timestamp: Long,
    val location: GpsLocation? = null
)
