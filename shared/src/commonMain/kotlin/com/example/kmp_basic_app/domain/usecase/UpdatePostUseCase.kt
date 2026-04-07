package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.domain.model.Post
import com.example.kmp_basic_app.domain.repository.PostRepository

class UpdatePostUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(id: String, title: String, body: String): Result<Post> {
        require(title.isNotBlank()) { "Title must not be blank" }
        require(body.isNotBlank()) { "Body must not be blank" }
        return postRepository.updatePost(id, title, body)
    }
}
