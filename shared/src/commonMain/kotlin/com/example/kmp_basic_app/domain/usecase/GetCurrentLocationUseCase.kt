package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.domain.model.GpsLocation
import com.example.kmp_basic_app.platform.PlatformLocationProvider

class GetCurrentLocationUseCase(
    private val locationProvider: PlatformLocationProvider
) {
    suspend operator fun invoke(): Result<GpsLocation> {
        return try {
            if (!locationProvider.isPermissionGranted()) {
                locationProvider.requestPermission()
            }
            if (!locationProvider.isPermissionGranted()) {
                return Result.failure(IllegalStateException("Location permission not granted"))
            }
            Result.success(locationProvider.getCurrentLocation())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isPermissionGranted(): Boolean = locationProvider.isPermissionGranted()
}
