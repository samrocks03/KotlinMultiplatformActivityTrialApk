package com.example.kmp_basic_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kmp_basic_app.domain.model.GpsLocation
import com.example.kmp_basic_app.domain.usecase.GetCurrentLocationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocationUiState(
    val location: GpsLocation? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdated: Long? = null,
    val permissionGranted: Boolean = false
)

class LocationViewModel(
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(permissionGranted = getCurrentLocationUseCase.isPermissionGranted()) }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getCurrentLocationUseCase()
                .onSuccess { location ->
                    _uiState.update {
                        it.copy(
                            location = location,
                            isLoading = false,
                            lastUpdated = System.currentTimeMillis(),
                            permissionGranted = true
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Location error") }
                }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(permissionGranted = granted) }
        if (granted) {
            refreshLocation()
        }
    }
}
