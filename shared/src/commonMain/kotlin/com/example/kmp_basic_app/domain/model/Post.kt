package com.example.kmp_basic_app.domain.model

data class Post(
    val id: String,
    val title: String,
    val body: String
)

data class PostPage(
    val posts: List<Post>,
    val totalCount: Int
)
