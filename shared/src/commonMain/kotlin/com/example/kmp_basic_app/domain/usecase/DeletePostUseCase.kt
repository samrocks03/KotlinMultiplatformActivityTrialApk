package com.example.kmp_basic_app.domain.usecase

import com.example.kmp_basic_app.domain.repository.PostRepository

class DeletePostUseCase(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(id: String): Result<Boolean> {
        return postRepository.deletePost(id)
    }
}
