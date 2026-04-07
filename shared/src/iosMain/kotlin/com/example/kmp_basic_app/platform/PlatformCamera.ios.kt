package com.example.kmp_basic_app.platform

import com.example.kmp_basic_app.domain.model.CapturedPhoto
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo

actual class PlatformCamera {
    actual suspend fun capturePhoto(): CapturedPhoto {
        val filePath = NSTemporaryDirectory() + "photo_${NSUUID().UUIDString}.jpg"
        return CapturedPhoto(
            filePath = filePath,
            timestamp = (NSDate().timeIntervalSince1970 * 1000).toLong()
        )
    }

    actual fun isAvailable(): Boolean {
        return AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) != null
    }
}
