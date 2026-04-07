package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.domain.model.Post
import com.example.kmp_basic_app.domain.repository.PostRepository

class CreatePostUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(title: String, body: String): Result<Post> {
        require(title.isNotBlank()) { "Title must not be blank" }
        require(body.isNotBlank()) { "Body must not be blank" }
        return postRepository.createPost(title, body)
    }
}
