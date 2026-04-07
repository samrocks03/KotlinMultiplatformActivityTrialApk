package com.example.kmp_basic_app.data.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.example.kmp_basic_app.data.remote.mapper.toDomain
import com.example.kmp_basic_app.domain.model.CharacterDetail
import com.example.kmp_basic_app.domain.model.CharacterPage
import com.example.kmp_basic_app.domain.repository.CharacterRepository
import com.example.kmp_basic_app.graphql.rickandmorty.GetCharacterDetailQuery
import com.example.kmp_basic_app.graphql.rickandmorty.GetCharactersQuery

class CharacterRepositoryImpl(
    private val apolloClient: ApolloClient
) : CharacterRepository {

    override suspend fun getCharacters(page: Int, nameFilter: String?): Result<CharacterPage> {
        return try {
            val response = apolloClient.query(
                GetCharactersQuery(
                    page = Optional.present(page),
                    nameFilter = if (nameFilter != null) Optional.present(nameFilter) else Optional.absent()
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

    override suspend fun getCharacterDetail(id: String): Result<CharacterDetail> {
        return try {
            val response = apolloClient.query(
                GetCharacterDetailQuery(id = id)
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
}
