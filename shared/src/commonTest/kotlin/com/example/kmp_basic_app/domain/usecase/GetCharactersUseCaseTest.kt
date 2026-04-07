package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.data.repository.FakeCharacterRepository
import com.example.kmp_basic_app.data.repository.FakeFavoriteRepository
import com.example.kmp_basic_app.domain.model.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetCharactersUseCaseTest {

    private val testCharacters = listOf(
        Character("1", "Rick", CharacterStatus.ALIVE, "Human", "Male",
            LocationBrief(null, "Earth"), LocationBrief(null, "Citadel"),
            "https://img.com/1.jpg", listOf("1", "2")),
        Character("2", "Morty", CharacterStatus.ALIVE, "Human", "Male",
            LocationBrief(null, "Earth"), LocationBrief(null, "Earth"),
            "https://img.com/2.jpg", listOf("1"))
    )

    @Test
    fun returnsCharactersFromRepository() = runTest {
        val page = CharacterPage(PageInfo(2, 1, null), testCharacters)
        val useCase = GetCharactersUseCase(
            FakeCharacterRepository(Result.success(page)),
            FakeFavoriteRepository()
        )
        val result = useCase(page = 1)
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().results.size)
        assertEquals("Rick", result.getOrThrow().results[0].name)
    }

    @Test
    fun marksFavoritedCharacters() = runTest {
        val page = CharacterPage(PageInfo(1, 1, null), testCharacters)
        val favRepo = FakeFavoriteRepository()
        favRepo.toggleFavorite(testCharacters[0])
        val useCase = GetCharactersUseCase(
            FakeCharacterRepository(Result.success(page)),
            favRepo
        )
        val result = useCase(page = 1)
        assertTrue(result.getOrThrow().results[0].isFavorite)
        assertTrue(!result.getOrThrow().results[1].isFavorite)
    }

    @Test
    fun returnsErrorOnNetworkFailure() = runTest {
        val useCase = GetCharactersUseCase(
            FakeCharacterRepository(Result.failure(Exception("timeout"))),
            FakeFavoriteRepository()
        )
        val result = useCase(page = 1)
        assertTrue(result.isFailure)
    }
}
