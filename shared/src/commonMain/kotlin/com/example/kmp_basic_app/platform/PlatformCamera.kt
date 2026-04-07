package com.example.kmp_basic_app.platform

import com.example.kmp_basic_app.domain.model.CapturedPhoto

expect class PlatformCamera {
    suspend fun capturePhoto(): CapturedPhoto
    fun isAvailable(): Boolean
}
