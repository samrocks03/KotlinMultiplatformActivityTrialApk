package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.domain.model.Character
import com.example.kmp_basic_app.domain.repository.FavoriteRepository

class ToggleFavoriteUseCase(
    private val favoriteRepository: FavoriteRepository
) {
    suspend operator fun invoke(character: Character): Boolean {
        return favoriteRepository.toggleFavorite(character)
    }
}
