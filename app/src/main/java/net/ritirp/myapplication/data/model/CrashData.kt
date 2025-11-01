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
