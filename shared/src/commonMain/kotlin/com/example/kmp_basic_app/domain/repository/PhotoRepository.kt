package com.example.kmp_basic_app.domain.repository

import com.example.kmp_basic_app.domain.model.CapturedPhoto
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    fun observePhotos(): Flow<List<CapturedPhoto>>
    suspend fun savePhoto(photo: CapturedPhoto)
    suspend fun deletePhoto(id: Long)
}
