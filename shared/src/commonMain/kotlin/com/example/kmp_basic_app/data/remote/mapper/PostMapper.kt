package com.example.kmp_basic_app.data.remote.mapper

import com.example.kmp_basic_app.domain.model.Post
import com.example.kmp_basic_app.domain.model.PostPage
import com.example.kmp_basic_app.graphql.graphqlzero.CreatePostMutation
import com.example.kmp_basic_app.graphql.graphqlzero.GetPostsQuery
import com.example.kmp_basic_app.graphql.graphqlzero.UpdatePostMutation

fun GetPostsQuery.Data.toDomain(): PostPage {
    val postsList = posts?.dataFilterNotNull()?.map { data ->
        Post(
            id = data.id ?: "",
            title = data.title ?: "",
            body = data.body ?: ""
        )
    } ?: emptyList()

    return PostPage(
        posts = postsList,
        totalCount = posts?.meta?.totalCount ?: 0
    )
}

fun CreatePostMutation.Data.toDomain(): Post {
    val p = createPost ?: throw IllegalStateException("Failed to create post")
    return Post(
        id = p.id ?: "",
        title = p.title ?: "",
        body = p.body ?: ""
    )
}

fun UpdatePostMutation.Data.toDomain(): Post {
    val p = updatePost ?: throw IllegalStateException("Failed to update post")
    return Post(
        id = p.id ?: "",
        title = p.title ?: "",
        body = p.body ?: ""
    )
}
