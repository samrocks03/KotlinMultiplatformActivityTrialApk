package com.example.kmp_basic_app.domain.repository

import com.example.kmp_basic_app.domain.model.Post
import com.example.kmp_basic_app.domain.model.PostPage

interface PostRepository {
    suspend fun getPosts(page: Int, limit: Int): Result<PostPage>
    suspend fun createPost(title: String, body: String): Result<Post>
    suspend fun updatePost(id: String, title: String, body: String): Result<Post>
    suspend fun deletePost(id: String): Result<Boolean>
}
