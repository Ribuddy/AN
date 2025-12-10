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

    // 위치 기록 버퍼
    private val locationHistory = mutableListOf<LocationRecord>()
    private var lastLocation: LocationRecord? = null

    // 센서 데이터
    private var gravityValues = FloatArray(3) { 0f }
    private var accelValues = FloatArray(3) { 0f }

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
    }

    /**
     * 주행 통계 추적 중지
     */
    fun stop() {
        if (!isRunning) return

        Log.d(TAG, "🔴 Stopping riding metrics tracking")
        sensorManager.unregisterListener(this)
        _metrics.value = _metrics.value.copy(endTime = System.currentTimeMillis())
        isRunning = false

        // 의미 있는 주행 데이터가 있으면 자동 저장 (거리 100m 이상 또는 1분 이상)
        val currentMetrics = _metrics.value
        if (currentMetrics.totalDistance > 100 || currentMetrics.durationInSeconds > 60) {
            saveRidingRecord()
        }
    }

    /**
     * 주행 기록 저장
     */
    fun saveRidingRecord(teamName: String? = null) {
        scope.launch {
            try {
                val repository = GlobalApplication.getRidingRecordRepository(context)
                val recordId = repository.saveRidingRecord(_metrics.value, teamName)
                Log.d(TAG, "✅ Riding record saved with ID: $recordId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save riding record", e)
            }
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
        if (!isRunning) return

        val now = System.currentTimeMillis()
        val locationRecord = LocationRecord(now, latitude, longitude, altitude, speed)

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
        val currentLeanAngle = maxOf(abs(roll), abs(pitch))

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
     * 통계 초기화
     */
    fun reset() {
        _metrics.value = RidingMetrics()
        locationHistory.clear()
        lastLocation = null
        Log.d(TAG, "Metrics reset")
    }
}
