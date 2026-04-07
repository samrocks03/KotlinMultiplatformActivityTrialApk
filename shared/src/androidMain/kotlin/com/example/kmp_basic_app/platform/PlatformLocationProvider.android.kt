package com.example.kmp_basic_app.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.kmp_basic_app.domain.model.GpsLocation
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

actual class PlatformLocationProvider(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    actual suspend fun getCurrentLocation(): GpsLocation = suspendCoroutine { continuation ->
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMaxUpdates(1)
                .build()
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    fusedLocationClient.removeLocationUpdates(this)
                    val location = result.lastLocation
                    if (location != null) {
                        continuation.resume(GpsLocation(location.latitude, location.longitude, location.accuracy))
                    } else {
                        continuation.resumeWithException(Exception("Could not get location"))
                    }
                }
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            continuation.resumeWithException(e)
        }
    }

    actual suspend fun requestPermission(): Boolean = isPermissionGranted()

    actual fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}
