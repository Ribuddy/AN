package net.ritirp.myapplication.data.model

/**
 * 사고 감지 관련 데이터 모델
 */

// 사고 이벤트
data class CrashEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val impactMagnitude: Float,
    val rotationMagnitude: Float,
    val detectionReason: String,
)

// 감지 민감도
enum class SensitivityLevel(
    val impactThreshold: Float,
    val rotationThreshold: Float,
    val freeFallThreshold: Float,
) {
    LOW(4.5f, 6.5f, 0.3f),
    MEDIUM(3.5f, 5.0f, 0.5f),
    HIGH(2.5f, 3.5f, 0.7f),
}

// 감지 상태
enum class DetectionState {
    NORMAL,
    POTENTIAL_FALL,
    IMPACT,
    AWAIT_RESPONSE,
}

// 센서 데이터
data class SensorData(
    val timestamp: Long,
    val ax: Float,
    val ay: Float,
    val az: Float,
    val magnitude: Float,
)

// 주행 통계 데이터
data class RidingMetrics(
    val startTime: Long = System.currentTimeMillis(),
    var endTime: Long? = null,
    var totalDistance: Double = 0.0, // 미터
    var topSpeed: Float = 0.0f, // m/s
    var maxLeanAngle: Float = 0.0f, // 도(degree)
    var totalClimb: Double = 0.0, // 상승 고도 (미터)
    var totalFall: Double = 0.0, // 하강 고도 (미터)
    var currentSpeed: Float = 0.0f, // 현재 속도 (m/s)
    var currentLeanAngle: Float = 0.0f, // 현재 기울기 (도)
) {
    val duration: Long
        get() = (endTime ?: System.currentTimeMillis()) - startTime

    val durationInMinutes: Double
        get() = duration / 60000.0

    val durationInSeconds: Double
        get() = duration / 1000.0

    val averageSpeed: Float
        get() = if (durationInSeconds > 0) (totalDistance / durationInSeconds).toFloat() else 0f

    val distanceInKm: Double
        get() = totalDistance / 1000.0

    val topSpeedInKmh: Float
        get() = topSpeed * 3.6f

    val currentSpeedInKmh: Float
        get() = currentSpeed * 3.6f

    val averageSpeedInKmh: Float
        get() = averageSpeed * 3.6f
}

// 위치 기록 (거리 및 고도 계산용)
data class LocationRecord(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val speed: Float?, // GPS 속도 (m/s)
)
