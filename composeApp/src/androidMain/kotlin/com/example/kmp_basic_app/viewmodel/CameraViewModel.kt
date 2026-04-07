package com.example.kmp_basic_app.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kmp_basic_app.domain.model.CapturedPhoto
import com.example.kmp_basic_app.domain.model.GpsLocation
import com.example.kmp_basic_app.domain.repository.PhotoRepository
import com.example.kmp_basic_app.platform.PlatformLocationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CameraUiState(
    val lastPhoto: CapturedPhoto? = null,
    val photoHistory: List<CapturedPhoto> = emptyList(),
    val isCapturing: Boolean = false,
    val error: String? = null,
    val pendingPhotoUri: Uri? = null
)

class CameraViewModel(
    private val photoRepository: PhotoRepository,
    private val locationProvider: PlatformLocationProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    init {
        observePhotos()
    }

    private fun observePhotos() {
        viewModelScope.launch {
            photoRepository.observePhotos().collect { photos ->
                _uiState.update {
                    it.copy(
                        photoHistory = photos,
                        lastPhoto = photos.firstOrNull()
                    )
                }
            }
        }
    }

    fun setPendingPhotoUri(uri: Uri) {
        _uiState.update { it.copy(pendingPhotoUri = uri) }
    }

    fun onPhotoCaptured(success: Boolean) {
        val uri = _uiState.value.pendingPhotoUri ?: return
        if (!success) {
            _uiState.update { it.copy(pendingPhotoUri = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCapturing = true, error = null) }

            val location: GpsLocation? = try {
                if (locationProvider.isPermissionGranted()) {
                    locationProvider.getCurrentLocation()
                } else null
            } catch (_: Exception) { null }

            val photo = CapturedPhoto(
                id = 0,
                filePath = uri.toString(),
                timestamp = System.currentTimeMillis(),
                location = location
            )

            try {
                photoRepository.savePhoto(photo)
                _uiState.update { it.copy(lastPhoto = photo, isCapturing = false, pendingPhotoUri = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isCapturing = false, error = e.message ?: "Save failed", pendingPhotoUri = null) }
            }
        }
    }

    fun deletePhoto(id: Long) {
        viewModelScope.launch {
            photoRepository.deletePhoto(id)
        }
    }
}
