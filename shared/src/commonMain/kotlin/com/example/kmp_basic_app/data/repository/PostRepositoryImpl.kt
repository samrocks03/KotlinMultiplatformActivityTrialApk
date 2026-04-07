package com.example.kmp_basic_app.data.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.example.kmp_basic_app.data.remote.mapper.toDomain
import com.example.kmp_basic_app.domain.model.Post
import com.example.kmp_basic_app.domain.model.PostPage
import com.example.kmp_basic_app.domain.repository.PostRepository
import com.example.kmp_basic_app.graphql.graphqlzero.CreatePostMutation
import com.example.kmp_basic_app.graphql.graphqlzero.DeletePostMutation
import com.example.kmp_basic_app.graphql.graphqlzero.GetPostsQuery
import com.example.kmp_basic_app.graphql.graphqlzero.UpdatePostMutation

class PostRepositoryImpl(
    private val apolloClient: ApolloClient
) : PostRepository {

    override suspend fun getPosts(page: Int, limit: Int): Result<PostPage> {
        return try {
            val response = apolloClient.query(
                GetPostsQuery(
                    page = Optional.present(page),
                    limit = Optional.present(limit)
                )
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val data = response.data
                if (data != null) {
                    Result.success(data.toDomain())
                } else {
                    Result.failure(Exception("No data returned"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createPost(title: String, body: String): Result<Post> {
        return try {
            val response = apolloClient.mutation(
                CreatePostMutation(title = title, body = body)
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val data = response.data
                if (data != null) {
                    Result.success(data.toDomain())
                } else {
                    Result.failure(Exception("No data returned"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePost(id: String, title: String, body: String): Result<Post> {
        return try {
            val response = apolloClient.mutation(
                UpdatePostMutation(id = id, title = title, body = body)
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                val data = response.data
                if (data != null) {
                    Result.success(data.toDomain())
                } else {
                    Result.failure(Exception("No data returned"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePost(id: String): Result<Boolean> {
        return try {
            val response = apolloClient.mutation(
                DeletePostMutation(id = id)
            ).execute()

            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.firstOrNull()?.message ?: "Unknown error"))
            } else {
                Result.success(response.data?.deletePost ?: false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
