package com.example.kmp_basic_app.domain.repository

import com.example.kmp_basic_app.domain.model.CharacterDetail
import com.example.kmp_basic_app.domain.model.CharacterPage

interface CharacterRepository {
    suspend fun getCharacters(page: Int, nameFilter: String?): Result<CharacterPage>
    suspend fun getCharacterDetail(id: String): Result<CharacterDetail>
}
