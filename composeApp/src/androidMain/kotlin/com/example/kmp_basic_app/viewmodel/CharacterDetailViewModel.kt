package com.example.kmp_basic_app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kmp_basic_app.domain.model.CharacterDetail
import com.example.kmp_basic_app.domain.usecase.GetCharacterDetailUseCase
import com.example.kmp_basic_app.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CharacterDetailUiState(
    val detail: CharacterDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class CharacterDetailViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getCharacterDetailUseCase: GetCharacterDetailUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterDetailUiState())
    val uiState: StateFlow<CharacterDetailUiState> = _uiState.asStateFlow()

    fun loadDetail(characterId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getCharacterDetailUseCase(characterId)
                .onSuccess { detail ->
                    _uiState.update { it.copy(detail = detail, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
                }
        }
    }

    fun toggleFavorite() {
        val detail = _uiState.value.detail ?: return
        viewModelScope.launch {
            val newState = toggleFavoriteUseCase(detail.character)
            _uiState.update { state ->
                state.detail?.let { d ->
                    state.copy(detail = d.copy(character = d.character.copy(isFavorite = newState)))
                } ?: state
            }
        }
    }
}
