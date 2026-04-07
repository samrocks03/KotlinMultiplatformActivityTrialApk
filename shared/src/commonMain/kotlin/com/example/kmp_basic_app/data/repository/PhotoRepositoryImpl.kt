package com.example.kmp_basic_app.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.kmp_basic_app.db.AppDatabase
import com.example.kmp_basic_app.domain.model.CapturedPhoto
import com.example.kmp_basic_app.domain.model.GpsLocation
import com.example.kmp_basic_app.domain.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PhotoRepositoryImpl(
    private val database: AppDatabase
) : PhotoRepository {

    override fun observePhotos(): Flow<List<CapturedPhoto>> {
        return database.capturedPhotoEntityQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities ->
                entities.map { entity ->
                    CapturedPhoto(
                        id = entity.id,
                        filePath = entity.file_path,
                        timestamp = entity.timestamp,
                        location = if (entity.latitude != null && entity.longitude != null) {
                            GpsLocation(
                                latitude = entity.latitude,
                                longitude = entity.longitude,
                                accuracy = entity.accuracy?.toFloat()
                            )
                        } else {
                            null
                        }
                    )
                }
            }
    }

    override suspend fun savePhoto(photo: CapturedPhoto) {
        database.capturedPhotoEntityQueries.insert(
            file_path = photo.filePath,
            timestamp = photo.timestamp,
            latitude = photo.location?.latitude,
            longitude = photo.location?.longitude,
            accuracy = photo.location?.accuracy?.toDouble()
        )
    }

    override suspend fun deletePhoto(id: Long) {
        database.capturedPhotoEntityQueries.deleteById(id)
    }
}
