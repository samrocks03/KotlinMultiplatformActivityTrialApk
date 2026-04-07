package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.data.repository.FakeFavoriteRepository
import com.example.kmp_basic_app.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToggleFavoriteUseCaseTest {

    private val character = Character(
        "1", "Rick", CharacterStatus.ALIVE, "Human", "Male",
        LocationBrief(null, "Earth"), LocationBrief(null, "Citadel"),
        "https://img.com/1.jpg", emptyList()
    )

    @Test
    fun togglesOnWhenNotFavorited() = runTest {
        val repo = FakeFavoriteRepository()
        val useCase = ToggleFavoriteUseCase(repo)
        val result = useCase(character)
        assertTrue(result)
        assertTrue(repo.isFavorite(character.id))
    }

    @Test
    fun togglesOffWhenAlreadyFavorited() = runTest {
        val repo = FakeFavoriteRepository()
        repo.toggleFavorite(character)
        val useCase = ToggleFavoriteUseCase(repo)
        val result = useCase(character)
        assertFalse(result)
        assertFalse(repo.isFavorite(character.id))
    }
}
