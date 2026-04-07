package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.data.repository.FakePostRepository
import com.example.kmp_basic_app.domain.model.Post
import com.example.kmp_basic_app.domain.model.PostPage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetPostsUseCaseTest {

    @Test
    fun returnsPostsFromRepository() = runTest {
        val posts = listOf(Post("1", "Title", "Body"), Post("2", "Title 2", "Body 2"))
        val useCase = GetPostsUseCase(FakePostRepository(Result.success(PostPage(posts, 2))))
        val result = useCase()
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().posts.size)
    }

    @Test
    fun returnsErrorOnFailure() = runTest {
        val useCase = GetPostsUseCase(FakePostRepository(Result.failure(Exception("network error"))))
        val result = useCase()
        assertTrue(result.isFailure)
    }
}
