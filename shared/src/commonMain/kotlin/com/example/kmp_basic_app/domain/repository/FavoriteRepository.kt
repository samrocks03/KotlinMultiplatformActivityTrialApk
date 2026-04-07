package com.example.kmp_basic_app.domain.repository

import com.example.kmp_basic_app.domain.model.Character
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun observeFavorites(): Flow<List<Character>>
    suspend fun toggleFavorite(character: Character): Boolean
    suspend fun isFavorite(characterId: String): Boolean
}
