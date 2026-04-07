package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.domain.model.CapturedPhoto
import com.example.kmp_basic_app.domain.repository.PhotoRepository
import com.example.kmp_basic_app.platform.PlatformCamera
import com.example.kmp_basic_app.platform.PlatformLocationProvider

class CapturePhotoUseCase(
    private val camera: PlatformCamera,
    private val locationProvider: PlatformLocationProvider,
    private val photoRepository: PhotoRepository
) {
    suspend operator fun invoke(): Result<CapturedPhoto> {
        return try {
            val photo = camera.capturePhoto()
            val location = try {
                if (locationProvider.isPermissionGranted()) {
                    locationProvider.getCurrentLocation()
                } else null
            } catch (_: Exception) { null }
            val photoWithLocation = photo.copy(location = location)
            photoRepository.savePhoto(photoWithLocation)
            Result.success(photoWithLocation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
