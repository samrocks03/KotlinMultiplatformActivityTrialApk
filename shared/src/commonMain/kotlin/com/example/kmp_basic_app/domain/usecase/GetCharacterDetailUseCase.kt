package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.domain.model.CharacterDetail
import com.example.kmp_basic_app.domain.repository.CharacterRepository
import com.example.kmp_basic_app.domain.repository.FavoriteRepository

class GetCharacterDetailUseCase(
    private val characterRepository: CharacterRepository,
    private val favoriteRepository: FavoriteRepository
) {
    suspend operator fun invoke(id: String): Result<CharacterDetail> {
        return characterRepository.getCharacterDetail(id).map { detail ->
            val isFav = favoriteRepository.isFavorite(detail.character.id)
            detail.copy(character = detail.character.copy(isFavorite = isFav))
        }
    }
}
