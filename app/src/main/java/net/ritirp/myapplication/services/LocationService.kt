package net.ritirp.myapplication.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationService(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) {
            return null
        }

        return try {
            suspendCancellableCoroutine { continuation ->
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // 권한이 없으면 서울시청 기본 위치 반환
                    val defaultLocation = Location("default").apply {
                        latitude = 37.5666805
                        longitude = 126.9784147
                    }
                    continuation.resume(defaultLocation)
                    return@suspendCancellableCoroutine
                }

                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    10000L // 10초
                ).apply {
                    setWaitForAccurateLocation(false)
                    setMaxUpdateDelayMillis(5000L)
                }.build()

                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    if (location != null && location.latitude != 0.0 && location.longitude != 0.0) {
                        // 유효한 GPS 위치인지 확인 (한국 영역 대략 체크)
                        if (location.latitude in 33.0..43.0 && location.longitude in 124.0..132.0) {
                            continuation.resume(location)
                        } else {
                            // GPS는 작동하지만 한국이 아닌 위치면 서울시청으로
                            val defaultLocation = Location("default").apply {
                                latitude = 37.5666805
                                longitude = 126.9784147
                            }
                            println("GPS 위치가 한국 밖입니다: ${location.latitude}, ${location.longitude}. 기본 위치 사용.")
                            continuation.resume(defaultLocation)
                        }
                    } else {
                        // GPS 위치를 얻지 못한 경우 서울시청 기본 위치 제공
                        val defaultLocation = Location("default").apply {
                            latitude = 37.5666805
                            longitude = 126.9784147
                        }
                        println("GPS 위치를 가져올 수 없습니다. 기본 위치 사용.")
                        continuation.resume(defaultLocation)
                    }
                }.addOnFailureListener { exception ->
                    println("위치 가져오기 실패: ${exception.message}")
                    // 실패한 경우에도 기본 위치 제공
                    val defaultLocation = Location("default").apply {
                        latitude = 37.5666805
                        longitude = 126.9784147
                    }
                    continuation.resume(defaultLocation)
                }
            }
        } catch (e: Exception) {
            println("위치 서비스 오류: ${e.message}")
            // 에뮬레이터용 기본 위치 반환
            Location("fallback").apply {
                latitude = 37.5666805
                longitude = 126.9784147
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
    }
}
