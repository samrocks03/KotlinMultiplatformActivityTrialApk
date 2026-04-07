package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.domain.model.Character
import com.example.kmp_basic_app.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow

class GetFavoritesUseCase(
    private val favoriteRepository: FavoriteRepository
) {
    operator fun invoke(): Flow<List<Character>> {
        return favoriteRepository.observeFavorites()
    }
}
