package com.example.kmp_basic_app.platform

import android.content.Context
import android.os.Environment
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.kmp_basic_app.domain.model.CapturedPhoto
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

actual class PlatformCamera(private val context: Context) {
    actual suspend fun capturePhoto(): CapturedPhoto = suspendCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    ProcessLifecycleOwner.get(),
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    imageCapture
                )
                val photoFile = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "photo_${System.currentTimeMillis()}.jpg"
                )
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            cameraProvider.unbindAll()
                            continuation.resume(
                                CapturedPhoto(filePath = photoFile.absolutePath, timestamp = System.currentTimeMillis())
                            )
                        }
                        override fun onError(exception: ImageCaptureException) {
                            cameraProvider.unbindAll()
                            continuation.resumeWithException(exception)
                        }
                    }
                )
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    actual fun isAvailable(): Boolean {
        return context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)
    }
}
