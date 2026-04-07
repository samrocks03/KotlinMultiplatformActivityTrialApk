package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.domain.model.PostPage
import com.example.kmp_basic_app.domain.repository.PostRepository

class GetPostsUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(page: Int = 1, limit: Int = 10): Result<PostPage> {
        return postRepository.getPosts(page, limit)
    }
}
