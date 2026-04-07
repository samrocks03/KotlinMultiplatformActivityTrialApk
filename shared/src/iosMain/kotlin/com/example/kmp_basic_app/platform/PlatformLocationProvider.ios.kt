package com.example.kmp_basic_app.platform

import com.example.kmp_basic_app.domain.model.GpsLocation
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.CLLocation
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

actual class PlatformLocationProvider {
    private val locationManager = CLLocationManager()

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun getCurrentLocation(): GpsLocation = suspendCoroutine { continuation ->
        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                val location = didUpdateLocations.lastOrNull() as? CLLocation
                manager.stopUpdatingLocation()
                manager.delegate = null
                if (location != null) {
                    val lat = location.coordinate.useContents { latitude }
                    val lng = location.coordinate.useContents { longitude }
                    continuation.resume(
                        GpsLocation(lat, lng, location.horizontalAccuracy.toFloat())
                    )
                } else {
                    continuation.resumeWithException(Exception("Could not get location"))
                }
            }

            override fun locationManager(manager: CLLocationManager, didFailWithError: platform.Foundation.NSError) {
                manager.stopUpdatingLocation()
                manager.delegate = null
                continuation.resumeWithException(Exception("Location error: ${didFailWithError.localizedDescription}"))
            }
        }
        locationManager.delegate = delegate
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.requestLocation()
    }

    actual suspend fun requestPermission(): Boolean = suspendCoroutine { continuation ->
        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                manager.delegate = null
                continuation.resume(isPermissionGranted())
            }
        }
        locationManager.delegate = delegate
        locationManager.requestWhenInUseAuthorization()
    }

    actual fun isPermissionGranted(): Boolean {
        val status = CLLocationManager.authorizationStatus()
        return status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways
    }
}
