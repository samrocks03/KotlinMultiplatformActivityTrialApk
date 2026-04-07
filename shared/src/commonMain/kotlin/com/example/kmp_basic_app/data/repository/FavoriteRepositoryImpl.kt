package com.example.kmp_basic_app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.kmp_basic_app.db.AppDatabase
import com.example.kmp_basic_app.domain.model.Character
import com.example.kmp_basic_app.domain.model.CharacterStatus
import com.example.kmp_basic_app.domain.model.LocationBrief
import com.example.kmp_basic_app.domain.repository.FavoriteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class FavoriteRepositoryImpl(
    private val database: AppDatabase
) : FavoriteRepository {

    override fun observeFavorites(): Flow<List<Character>> {
        return database.favoriteQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { favorites ->
                favorites.map { fav ->
                    Character(
                        id = fav.character_id,
                        name = fav.name,
                        status = CharacterStatus.fromString(fav.status),
                        species = fav.species,
                        gender = "",
                        origin = LocationBrief(id = null, name = "Unknown"),
                        location = LocationBrief(id = null, name = "Unknown"),
                        imageUrl = fav.image_url,
                        episodeIds = emptyList(),
                        isFavorite = true
                    )
                }
            }
    }

    override suspend fun toggleFavorite(character: Character): Boolean {
        val currentlyFavorite = isFavorite(character.id)
        if (currentlyFavorite) {
            database.favoriteQueries.deleteById(character.id)
        } else {
            database.favoriteQueries.insert(
                character_id = character.id,
                name = character.name,
                status = character.status.name,
                species = character.species,
                image_url = character.imageUrl,
                added_at = Clock.System.now().toEpochMilliseconds()
            )
        }
        return !currentlyFavorite
    }

    override suspend fun isFavorite(characterId: String): Boolean {
        return database.favoriteQueries.isFavorite(characterId).executeAsOne() > 0
    }
}
