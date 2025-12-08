package net.ritirp.myapplication.service

import android.annotation.SuppressLint
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.ritirp.myapplication.data.repository.DrivingRepository
import net.ritirp.myapplication.data.repository.MapRepository
import kotlin.coroutines.resume

/**
 * Application Scope에서 위치 업데이트를 관리하는 Manager
 * 앱이 백그라운드에 있어도 계속 실행됨
 */
class LocationUpdateManager(
    private val applicationScope: CoroutineScope,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val drivingRepository: DrivingRepository,
    private val mapRepository: MapRepository,
    private val ridingMetricsTracker: RidingMetricsTracker? = null,
) {
    private var locationUpdateJob: Job? = null

    init {
        // DrivingRepository의 ridingRecordId를 관찰하여 자동으로 위치 업데이트 시작/중지
        applicationScope.launch {
            drivingRepository.currentRidingRecordId.collect { ridingRecordId ->
                if (ridingRecordId != null) {
                    Log.d("LocationUpdateManager", "라이딩 시작 감지, 위치 업데이트 시작: $ridingRecordId")
                    ridingMetricsTracker?.start()
                    startLocationUpdates()
                } else {
                    Log.d("LocationUpdateManager", "라이딩 종료 감지, 위치 업데이트 중지")
                    stopLocationUpdates()
                    ridingMetricsTracker?.stop()
                }
            }
        }
    }

    /**
     * GPS 위치 가져오기 (위도, 경도, 고도, 속도)
     */
    @SuppressLint("MissingPermission")
    private suspend fun getCurrentGpsLocation(): LocationData =
        suspendCancellableCoroutine { continuation ->
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token,
            ).addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(
                        LocationData(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            altitude = if (location.hasAltitude()) location.altitude else null,
                            speed = if (location.hasSpeed()) location.speed else null,
                        ),
                    )
                } else {
                    // 위치를 가져오지 못한 경우 기본 위치 사용
                    continuation.resume(
                        LocationData(
                            latitude = 37.5666102,
                            longitude = 126.9783881,
                            altitude = null,
                            speed = null,
                        ),
                    )
                }
            }.addOnFailureListener {
                // 실패한 경우 기본 위치 사용
                continuation.resume(
                    LocationData(
                        latitude = 37.5666102,
                        longitude = 126.9783881,
                        altitude = null,
                        speed = null,
                    ),
                )
            }
        }

    private data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val altitude: Double?,
        val speed: Float?,
    )

    /**
     * 위치 업데이트 시작
     */
    private fun startLocationUpdates() {
        // 기존 Job이 있으면 취소
        stopLocationUpdates()

        Log.d("LocationUpdateManager", "위치 업데이트 루프 시작")

        locationUpdateJob =
            applicationScope.launch {
                while (drivingRepository.currentRidingRecordId.value != null) {
                    try {
                        val ridingRecordId = drivingRepository.currentRidingRecordId.value
                        if (ridingRecordId != null) {
                            // GPS 위치 가져오기
                            val locationData = getCurrentGpsLocation()
                            Log.d(
                                "LocationUpdateManager",
                                "위치 업데이트: lat=${locationData.latitude}, lon=${locationData.longitude}, " +
                                    "alt=${locationData.altitude}, speed=${locationData.speed}",
                            )

                            // RidingMetricsTracker에 위치 업데이트
                            ridingMetricsTracker?.updateLocation(
                                locationData.latitude,
                                locationData.longitude,
                                locationData.altitude,
                                locationData.speed,
                            )

                            // 서버에 위치 업데이트 & 팀원 위치 조회
                            drivingRepository.updateLocationAndGetTeamLocations(
                                ridingRecordId,
                                locationData.latitude,
                                locationData.longitude,
                                locationData.altitude,
                            ).onSuccess { locations ->
                                Log.d("LocationUpdateManager", "팀원 ${locations.size}명 위치 업데이트 성공")
                                locations.forEach { member ->
                                    Log.d("LocationUpdateManager", "  - ${member.memberName}: lat=${member.lat}, lon=${member.lon}")
                                }

                                // 사고 정보 가져오기
                                val accidents = drivingRepository.accidents.value
                                val accidentUserIds = accidents.map { it.userId }.toSet()
                                if (accidentUserIds.isNotEmpty()) {
                                    Log.w("LocationUpdateManager", "⚠️ 사고난 팀원: $accidentUserIds")
                                }

                                // MapRepository에 사고 정보 업데이트
                                mapRepository.updateAccidentUserIds(accidentUserIds)

                                // MapRepository에 팀원 마커 업데이트
                                if (locations.isNotEmpty()) {
                                    Log.d("LocationUpdateManager", "MapRepository에 ${locations.size}개 마커 업데이트 시작")
                                    mapRepository.updateTeamMemberMarkers(locations, accidentUserIds)
                                    Log.d("LocationUpdateManager", "MapRepository에 마커 업데이트 완료")
                                } else {
                                    Log.d("LocationUpdateManager", "팀원이 없어 마커 클리어")
                                    mapRepository.clearTeamMarkers()
                                }
                            }.onFailure { error ->
                                Log.e("LocationUpdateManager", "위치 업데이트 실패: ${error.message}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("LocationUpdateManager", "위치 업데이트 오류: ${e.message}", e)
                    }

                    // 5초마다 업데이트
                    delay(5000)
                }

                Log.d("LocationUpdateManager", "위치 업데이트 루프 종료")
            }
    }

    /**
     * 위치 업데이트 중지
     */
    private fun stopLocationUpdates() {
        locationUpdateJob?.cancel()
        locationUpdateJob = null
        Log.d("LocationUpdateManager", "위치 업데이트 중지")
    }
}
