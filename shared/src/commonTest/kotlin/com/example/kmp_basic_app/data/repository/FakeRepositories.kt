package com.example.kmp_basic_app.data.repository

import com.example.kmp_basic_app.domain.model.*
import com.example.kmp_basic_app.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCharacterRepository(
    private val result: Result<CharacterPage> = Result.success(
        CharacterPage(PageInfo(0, 0, null), emptyList())
    ),
    private val detailResult: Result<CharacterDetail>? = null
) : CharacterRepository {
    override suspend fun getCharacters(page: Int, nameFilter: String?): Result<CharacterPage> = result
    override suspend fun getCharacterDetail(id: String): Result<CharacterDetail> =
        detailResult ?: Result.failure(Exception("Not configured"))
}

class FakePostRepository(
    private val postsResult: Result<PostPage> = Result.success(PostPage(emptyList(), 0)),
    private val createResult: Result<Post> = Result.success(Post("1", "Test", "Body")),
    private val updateResult: Result<Post> = Result.success(Post("1", "Updated", "Body")),
    private val deleteResult: Result<Boolean> = Result.success(true)
) : PostRepository {
    override suspend fun getPosts(page: Int, limit: Int): Result<PostPage> = postsResult
    override suspend fun createPost(title: String, body: String): Result<Post> = createResult
    override suspend fun updatePost(id: String, title: String, body: String): Result<Post> = updateResult
    override suspend fun deletePost(id: String): Result<Boolean> = deleteResult
}

class FakeFavoriteRepository : FavoriteRepository {
    private val favorites = MutableStateFlow<List<Character>>(emptyList())
    private val favoriteIds = mutableSetOf<String>()

    override fun observeFavorites(): Flow<List<Character>> = favorites
    override suspend fun toggleFavorite(character: Character): Boolean {
        return if (favoriteIds.contains(character.id)) {
            favoriteIds.remove(character.id)
            favorites.value = favorites.value.filter { it.id != character.id }
            false
        } else {
            favoriteIds.add(character.id)
            favorites.value = favorites.value + character.copy(isFavorite = true)
            true
        }
    }
    override suspend fun isFavorite(characterId: String): Boolean = favoriteIds.contains(characterId)
}
