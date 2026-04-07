package com.example.kmp_basic_app.platform

import com.example.kmp_basic_app.domain.model.GpsLocation

expect class PlatformLocationProvider {
    suspend fun getCurrentLocation(): GpsLocation
    suspend fun requestPermission(): Boolean
    fun isPermissionGranted(): Boolean
}
