package net.ritirp.myapplication.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ritirp.myapplication.GlobalApplication
import net.ritirp.myapplication.data.model.LocationRecord
import net.ritirp.myapplication.data.model.RidingMetrics
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * 주행 통계 추적기
 * - Distance: GPS 위치 기반 거리 계산
 * - Duration: 시작/종료 시간 추적
 * - Top Speed: GPS 속도 최댓값
 * - Climb/Fall: 고도 변화 누적
 * - Lean Angle: 가속도계와 자이로스코프 기반 기울기 계산
 * - 하나의 주행 레코드에 3초마다 위치 포인트 추가
 */
class RidingMetricsTracker(
    private val context: Context,
) : SensorEventListener {
    companion object {
        private const val TAG = "RidingMetricsTracker"
        private const val MIN_ALTITUDE_CHANGE = 2.0 // 최소 고도 변화 (미터) - 노이즈 제거
        private const val MIN_DISTANCE_CHANGE = 5.0 // 최소 거리 변화 (미터) - 정지 시 거리 증가 방지
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    private val _metrics = MutableStateFlow(RidingMetrics())
    val metrics: StateFlow<RidingMetrics> = _metrics.asStateFlow()

    // 위치 기록 버퍼 (메모리에만 저장, DB에는 종료 시 한번에 저장)
    private val locationHistory = mutableListOf<LocationRecord>()
    private var lastLocation: LocationRecord? = null

    // 센서 데이터
    private var gravityValues = FloatArray(3) { 0f }
    private var accelValues = FloatArray(3) { 0f }

    // 기울기 캘리브레이션 오프셋
    private var leanAngleOffset = 0f

    private var isRunning = false

    // 코루틴 스코프
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 주행 통계 추적 시작
     */
    fun start() {
        if (isRunning) {
            Log.w(TAG, "Already running, ignoring start() call")
            return
        }

        Log.d(TAG, "🟢 Starting riding metrics tracking")

        // 센서 등록
        accelerometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME,
            )
        }
        gravitySensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME,
            )
        }

        // 초기화
        _metrics.value = RidingMetrics()
        locationHistory.clear()
        lastLocation = null
        isRunning = true

        Log.d(TAG, "📊 Data collection started (memory only, will save on stop)")
    }

    /**
     * 주행 통계 추적 중지
     * 메모리에 수집된 모든 데이터를 DB에 한번에 저장
     */
    fun stop() {
        if (!isRunning) return

        Log.d(TAG, "🔴 Stopping riding metrics tracking")

        sensorManager.unregisterListener(this)
        _metrics.value = _metrics.value.copy(endTime = System.currentTimeMillis())
        isRunning = false

        // 모든 데이터를 하나의 레코드로 저장
        scope.launch {
            saveAllDataToDatabase()
        }
    }

    /**
     * 메모리에 수집된 모든 데이터를 하나의 레코드로 DB에 저장
     */
    private suspend fun saveAllDataToDatabase() {
        try {
            if (locationHistory.isEmpty()) {
                Log.w(TAG, "⚠️ No location data collected")
                return
            }

            val repository = GlobalApplication.getLocalRidingRecordRepository(context)
            val currentMetrics = _metrics.value

            val firstLocation = locationHistory.first()
            val lastLocation = locationHistory.last()

            Log.d(TAG, "💾 Saving riding record with ${locationHistory.size} location points...")

            // 새 레코드 생성
            val result = repository.startRiding(
                teamId = null,
                teamName = "주행_${System.currentTimeMillis() / 1000}",
                startLat = firstLocation.latitude,
                startLon = firstLocation.longitude,
                startEle = firstLocation.altitude,
                startLocationName = null
            )

            result.onSuccess { recordId ->
                Log.d(TAG, "✅ Riding record created (ID: $recordId)")

                // 모든 위치 포인트를 하나씩 추가 (각 포인트의 기울기 값 포함)
                locationHistory.forEachIndexed { index, loc ->
                    repository.updateLocation(
                        recordId = recordId,
                        lat = loc.latitude,
                        lon = loc.longitude,
                        ele = loc.altitude,
                        speedKmh = (loc.speed ?: 0f) * 3.6, // m/s -> km/h
                        accuracy = null,
                        leanAngleDegrees = (loc.leanAngle ?: 0f).toDouble() // 각 포인트의 기울기
                    )

                    if ((index + 1) % 10 == 0) {
                        Log.d(TAG, "   📍 Saved ${index + 1}/${locationHistory.size} points...")
                    }
                }

                // 주행 완료 처리
                repository.endRiding(
                    recordId = recordId,
                    endLat = lastLocation.latitude,
                    endLon = lastLocation.longitude,
                    endEle = lastLocation.altitude,
                    endLocationName = null
                )

                Log.d(TAG, "✅ Riding record completed!")
                Log.d(TAG, "   📊 Total Points: ${locationHistory.size}")
                Log.d(TAG, "   📏 Distance: ${String.format("%.2f", currentMetrics.totalDistance)}m")
                Log.d(TAG, "   🏃 Max Speed: ${String.format("%.1f", currentMetrics.topSpeed * 3.6f)} km/h")
                Log.d(TAG, "   📐 Max LeanAngle: ${String.format("%.1f", currentMetrics.maxLeanAngle)}°")
                Log.d(TAG, "   ⬆️ Climb: ${String.format("%.2f", currentMetrics.totalClimb)}m")
                Log.d(TAG, "   ⬇️ Fall: ${String.format("%.2f", currentMetrics.totalFall)}m")
            }.onFailure { error ->
                Log.e(TAG, "❌ Failed to create riding record", error)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save riding data", e)
        }
    }

    /**
     * GPS 위치 업데이트
     */
    fun updateLocation(
        latitude: Double,
        longitude: Double,
        altitude: Double?,
        speed: Float?,
    ) {
        if (!isRunning) {
            Log.w(TAG, "⚠️ updateLocation called but tracker is not running")
            return
        }

        val now = System.currentTimeMillis()
        // 현재 기울기 값을 포함한 LocationRecord 생성
        val currentLeanAngle = _metrics.value.currentLeanAngle
        val locationRecord = LocationRecord(now, latitude, longitude, altitude, speed, currentLeanAngle)

        Log.d(TAG, "🌍 GPS Update #${locationHistory.size + 1}: lat=$latitude, lon=$longitude, alt=$altitude, speed=$speed, lean=${String.format("%.1f", currentLeanAngle)}°")

        // 거리 계산
        lastLocation?.let { last ->
            val distance =
                calculateDistance(
                    last.latitude,
                    last.longitude,
                    latitude,
                    longitude,
                )

            // 최소 거리 변화 이상인 경우만 누적 (정지 시 노이즈 방지)
            if (distance >= MIN_DISTANCE_CHANGE) {
                _metrics.value =
                    _metrics.value.copy(
                        totalDistance = _metrics.value.totalDistance + distance,
                    )
                Log.d(
                    TAG,
                    "Distance updated: +${String.format(
                        "%.2f",
                        distance,
                    )}m, Total: ${String.format("%.2f", _metrics.value.totalDistance)}m",
                )
            }

            // 고도 변화 계산
            if (altitude != null && last.altitude != null) {
                val altitudeChange = altitude - last.altitude
                if (abs(altitudeChange) >= MIN_ALTITUDE_CHANGE) {
                    if (altitudeChange > 0) {
                        _metrics.value =
                            _metrics.value.copy(
                                totalClimb = _metrics.value.totalClimb + altitudeChange,
                            )
                        Log.d(TAG, "Climb: +${String.format("%.2f", altitudeChange)}m")
                    } else {
                        _metrics.value =
                            _metrics.value.copy(
                                totalFall = _metrics.value.totalFall + abs(altitudeChange),
                            )
                        Log.d(TAG, "Fall: +${String.format("%.2f", abs(altitudeChange))}m")
                    }
                }
            }
        }

        // 속도 업데이트
        speed?.let {
            _metrics.value = _metrics.value.copy(currentSpeed = it)
            if (it > _metrics.value.topSpeed) {
                _metrics.value = _metrics.value.copy(topSpeed = it)
                Log.d(TAG, "New top speed: ${String.format("%.2f", it * 3.6f)} km/h")
            }
        }

        // 위치 기록 저장
        locationHistory.add(locationRecord)
        lastLocation = locationRecord

        // 오래된 기록 정리 (최근 100개만 유지)
        if (locationHistory.size > 100) {
            locationHistory.removeAt(0)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isRunning) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accelValues = event.values.clone()
                calculateLeanAngle()
            }
            Sensor.TYPE_GRAVITY -> {
                gravityValues = event.values.clone()
            }
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) {
        // 정확도 변경 무시
    }

    /**
     * 기울기 각도 계산 (Roll & Pitch)
     * - Roll: 좌우 기울기
     * - Pitch: 전후 기울기
     * - 최대값을 Lean Angle로 사용
     */
    private fun calculateLeanAngle() {
        // 중력 센서가 없으면 가속도계 사용
        val gx = if (gravitySensor != null) gravityValues[0] else accelValues[0]
        val gy = if (gravitySensor != null) gravityValues[1] else accelValues[1]
        val gz = if (gravitySensor != null) gravityValues[2] else accelValues[2]

        // Roll (좌우 기울기) - 라디안을 도로 변환
        val roll = Math.toDegrees(atan2(gy.toDouble(), gz.toDouble())).toFloat()

        // Pitch (전후 기울기)
        val pitch =
            Math.toDegrees(
                atan2(-gx.toDouble(), sqrt((gy * gy + gz * gz).toDouble())),
            ).toFloat()

        // 현재 기울기 (절댓값의 최댓값)
        val rawLeanAngle = maxOf(abs(roll), abs(pitch))

        // 캘리브레이션 오프셋 적용
        val currentLeanAngle = maxOf(0f, rawLeanAngle - leanAngleOffset)

        _metrics.value = _metrics.value.copy(currentLeanAngle = currentLeanAngle)

        // 최대 기울기 업데이트
        if (currentLeanAngle > _metrics.value.maxLeanAngle) {
            _metrics.value = _metrics.value.copy(maxLeanAngle = currentLeanAngle)
            Log.d(TAG, "New max lean angle: ${String.format("%.1f", currentLeanAngle)}°")
        }
    }

    /**
     * 두 GPS 좌표 간 거리 계산 (Haversine 공식)
     */
    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }

    /**
     * 현재 통계 가져오기
     */
    fun getCurrentMetrics(): RidingMetrics = _metrics.value

    /**
     * 기울기 캘리브레이션
     * 현재 센서 값을 기준점(0도)으로 설정
     */
    fun calibrateLeanAngle() {
        val gx = if (gravitySensor != null) gravityValues[0] else accelValues[0]
        val gy = if (gravitySensor != null) gravityValues[1] else accelValues[1]
        val gz = if (gravitySensor != null) gravityValues[2] else accelValues[2]

        val roll = Math.toDegrees(atan2(gy.toDouble(), gz.toDouble())).toFloat()
        val pitch = Math.toDegrees(atan2(-gx.toDouble(), sqrt((gy * gy + gz * gz).toDouble()))).toFloat()

        leanAngleOffset = maxOf(abs(roll), abs(pitch))

        Log.d(TAG, "✅ Lean angle calibrated! Offset: ${String.format("%.1f", leanAngleOffset)}°")
        Log.d(TAG, "   Current position set as 0° baseline")
    }

    /**
     * 통계 초기화
     */
    fun reset() {
        _metrics.value = RidingMetrics()
        locationHistory.clear()
        lastLocation = null
        Log.d(TAG, "Metrics reset")
    }
}
