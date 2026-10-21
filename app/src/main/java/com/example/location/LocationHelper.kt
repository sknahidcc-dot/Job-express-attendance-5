package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class LocationCheckResult(
    val isWithinRange: Boolean,
    val distanceMeters: Float,
    val userLat: Double,
    val userLng: Double,
    val targetLat: Double,
    val targetLng: Double,
    val message: String
)

class LocationHelper(private val context: Context) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val cts = CancellationTokenSource()
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    continuation.resume(loc)
                } else {
                    fusedClient.lastLocation
                        .addOnSuccessListener { lastLoc -> continuation.resume(lastLoc) }
                        .addOnFailureListener { continuation.resume(null) }
                }
            }
            .addOnFailureListener {
                fusedClient.lastLocation
                    .addOnSuccessListener { lastLoc -> continuation.resume(lastLoc) }
                    .addOnFailureListener { continuation.resume(null) }
            }

        continuation.invokeOnCancellation {
            cts.cancel()
        }
    }

    suspend fun verifyLocation(
        targetLat: Double = 23.5462131,
        targetLng: Double = 89.1720178,
        allowedRadiusMeters: Double = 300.0
    ): LocationCheckResult {
        val loc = getCurrentLocation()
        if (loc == null) {
            return LocationCheckResult(
                isWithinRange = false,
                distanceMeters = -1f,
                userLat = 0.0,
                userLng = 0.0,
                targetLat = targetLat,
                targetLng = targetLng,
                message = "ডিভাইসের জিপিএস লোকেশন সনাক্ত করা যায়নি। অনুগ্রহ করে ফোনের লোকেশন/GPS চালু করুন এবং পুনরায় চেষ্টা করুন।"
            )
        }

        val results = FloatArray(1)
        Location.distanceBetween(
            loc.latitude,
            loc.longitude,
            targetLat,
            targetLng,
            results
        )
        val distance = results[0]

        return if (distance <= allowedRadiusMeters) {
            LocationCheckResult(
                isWithinRange = true,
                distanceMeters = distance,
                userLat = loc.latitude,
                userLng = loc.longitude,
                targetLat = targetLat,
                targetLng = targetLng,
                message = "লোকেশন সঠিক! আপনি লাইব্রেরির মধ্যে উপস্থিত আছেন।"
            )
        } else {
            LocationCheckResult(
                isWithinRange = false,
                distanceMeters = distance,
                userLat = loc.latitude,
                userLng = loc.longitude,
                targetLat = targetLat,
                targetLng = targetLng,
                message = "আপনি লাইব্রেরি এরিয়া থেকে বাহিরে আছেন (${distance.toInt()} মিটার দূরে)। বাসায় বা অন্য স্থান থেকে হাজিরা দেওয়া নিষিদ্ধ।"
            )
        }
    }
}
