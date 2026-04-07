package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.domain.model.CharacterPage
import com.example.kmp_basic_app.domain.repository.CharacterRepository
import com.example.kmp_basic_app.domain.repository.FavoriteRepository

class GetCharactersUseCase(
    private val characterRepository: CharacterRepository,
    private val favoriteRepository: FavoriteRepository
) {
    suspend operator fun invoke(page: Int, nameFilter: String? = null): Result<CharacterPage> {
        return characterRepository.getCharacters(page, nameFilter).map { characterPage ->
            val results = characterPage.results.map { character ->
                character.copy(isFavorite = favoriteRepository.isFavorite(character.id))
            }
            characterPage.copy(results = results)
        }
    }
}
