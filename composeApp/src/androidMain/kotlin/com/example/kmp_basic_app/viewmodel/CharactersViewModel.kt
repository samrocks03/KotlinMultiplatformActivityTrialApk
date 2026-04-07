package com.example.kmp_basic_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kmp_basic_app.domain.model.Character
import com.example.kmp_basic_app.domain.usecase.GetCharactersUseCase
import com.example.kmp_basic_app.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CharactersUiState(
    val characters: List<Character> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val currentPage: Int = 1,
    val hasNextPage: Boolean = false
)

class CharactersViewModel(
    private val getCharactersUseCase: GetCharactersUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CharactersUiState())
    val uiState: StateFlow<CharactersUiState> = _uiState.asStateFlow()

    init {
        loadCharacters()
    }

    fun loadCharacters() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val filter = _uiState.value.searchQuery.ifBlank { null }
            getCharactersUseCase(page = 1, nameFilter = filter)
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            characters = page.results,
                            isLoading = false,
                            currentPage = 1,
                            hasNextPage = page.info.next != null
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
                }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasNextPage) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val nextPage = state.currentPage + 1
            val filter = state.searchQuery.ifBlank { null }
            getCharactersUseCase(page = nextPage, nameFilter = filter)
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            characters = it.characters + page.results,
                            isLoadingMore = false,
                            currentPage = nextPage,
                            hasNextPage = page.info.next != null
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadCharacters()
    }

    fun toggleFavorite(character: Character) {
        viewModelScope.launch {
            val newFavoriteState = toggleFavoriteUseCase(character)
            _uiState.update { state ->
                state.copy(
                    characters = state.characters.map {
                        if (it.id == character.id) it.copy(isFavorite = newFavoriteState) else it
                    }
                )
            }
        }
    }
}
