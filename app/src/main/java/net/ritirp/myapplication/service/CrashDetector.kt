package net.ritirp.myapplication.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import net.ritirp.myapplication.data.model.CrashEvent
import net.ritirp.myapplication.data.model.DetectionState
import net.ritirp.myapplication.data.model.SensitivityLevel
import net.ritirp.myapplication.data.model.SensorData
import kotlin.math.sqrt

/**
 * 포어그라운드 전용 사고 감지기
 * - 서비스 없이 Activity 생명주기에 따라 동작
 * - 선형가속도 + 자이로스코프 센서 사용
 * - 롤링 윈도우 + 상태머신 + 필터링 기반 오탐 최소화
 */
class CrashDetector(
    context: Context,
    private var sensitivity: SensitivityLevel = SensitivityLevel.MEDIUM,
) : SensorEventListener {

    companion object {
        private const val TAG = "CrashDetector"
        private const val WINDOW_SIZE_MS = 1000L // 1초 롤링 윈도우
        private const val FREE_FALL_DURATION_MS = 200L // 자유낙하 최소 지속시간
        private const val IMPACT_COOLDOWN_MS = 5000L // 사고 판정 후 쿨다운 (재판정 방지)
        private const val GRAVITY = 9.81f

        // 필터 설정
        private const val LOW_PASS_ALPHA = 0.8f // 저역통과 필터 (노이즈 제거)
        private const val SPIKE_REJECT_THRESHOLD = 50f // 비정상적으로 큰 값 제거 (센서 오류)
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val linearAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    private val _crashEvents = MutableSharedFlow<CrashEvent>(extraBufferCapacity = 1)
    val crashEvents: SharedFlow<CrashEvent> = _crashEvents.asSharedFlow()

    // 롤링 윈도우 버퍼
    private val accelBuffer = mutableListOf<SensorData>()
    private val gyroBuffer = mutableListOf<SensorData>()

    // 상태 머신
    private var currentState = DetectionState.NORMAL
    private var freeFallStartTime = 0L
    private var lastImpactTime = 0L

    // 중력 보정용 (TYPE_LINEAR_ACCELERATION 미지원 시)
    private var gravityValues = FloatArray(3)
    private var accelValues = FloatArray(3)

    private var isRunning = false

    // 저역통과 필터용 이전 값
    private var filteredAccel = floatArrayOf(0f, 0f, 0f)
    private var filteredGyro = floatArrayOf(0f, 0f, 0f)

    // 디버그 카운터
    private var sampleCount = 0
    private var crashDetectionCount = 0

    /**
     * 센서 등록 및 감지 시작
     */
    fun start() {
        if (isRunning) {
            Log.w(TAG, "Already running, ignoring start() call")
            return
        }

        Log.d(TAG, "🟢 Starting crash detection (Sensitivity: ${sensitivity.name})")

        // 선형가속도 센서 우선 사용
        if (linearAccelSensor != null) {
            sensorManager.registerListener(
                this,
                linearAccelSensor,
                SensorManager.SENSOR_DELAY_GAME
            )
            Log.d(TAG, "Using TYPE_LINEAR_ACCELERATION")
        } else {
            // 대체: 가속도 - 중력
            sensorManager.registerListener(
                this,
                accelSensor,
                SensorManager.SENSOR_DELAY_GAME
            )
            sensorManager.registerListener(
                this,
                gravitySensor,
                SensorManager.SENSOR_DELAY_GAME
            )
            Log.d(TAG, "Using TYPE_ACCELEROMETER + TYPE_GRAVITY fallback")
        }

        // 자이로스코프 등록
        gyroSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
            Log.d(TAG, "Gyroscope sensor registered")
        }

        isRunning = true
        resetState()
    }

    /**
     * 센서 해제 및 감지 중단
     */
    fun stop() {
        if (!isRunning) return

        Log.d(TAG, "🔴 Stopping crash detection")
        sensorManager.unregisterListener(this)
        isRunning = false
        resetState()
    }

    private fun resetState() {
        accelBuffer.clear()
        gyroBuffer.clear()
        currentState = DetectionState.NORMAL
        freeFallStartTime = 0L
    }

    override fun onSensorChanged(event: SensorEvent) {
        val now = System.currentTimeMillis()

        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                processLinearAcceleration(event.values, now)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                accelValues = event.values.clone()
                processManualLinearAcceleration(now)
            }
            Sensor.TYPE_GRAVITY -> {
                gravityValues = event.values.clone()
            }
            Sensor.TYPE_GYROSCOPE -> {
                processGyroscope(event.values, now)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 정확도 변경 무시
    }

    /**
     * 민감도 동적 변경
     */
    fun updateSensitivity(newSensitivity: SensitivityLevel) {
        sensitivity = newSensitivity
        Log.d(TAG, "Sensitivity updated to: ${newSensitivity.name}")
    }

    /**
     * 선형가속도 처리 (저역통과 필터 적용)
     */
    private fun processLinearAcceleration(values: FloatArray, timestamp: Long) {
        // 저역통과 필터 적용 (급격한 노이즈 제거)
        filteredAccel[0] = lowPassFilter(values[0], filteredAccel[0])
        filteredAccel[1] = lowPassFilter(values[1], filteredAccel[1])
        filteredAccel[2] = lowPassFilter(values[2], filteredAccel[2])

        val magnitude = calculateMagnitude(filteredAccel[0], filteredAccel[1], filteredAccel[2])

        // 비정상적으로 큰 값 제거 (센서 오류)
        if (magnitude > SPIKE_REJECT_THRESHOLD) {
            Log.w(TAG, "Spike rejected: |a|=${String.format("%.2f", magnitude)} m/s²")
            return
        }

        // 롤링 윈도우에 추가
        accelBuffer.add(SensorData(timestamp, filteredAccel[0], filteredAccel[1], filteredAccel[2], magnitude))
        cleanOldData(accelBuffer, timestamp)

        sampleCount++
        if (sampleCount % 50 == 0) { // 매 50샘플마다 로그
            Log.v(TAG, "Linear Accel: |a|=${String.format("%.2f", magnitude)} m/s² (samples: $sampleCount)")
        }

        analyzeImpact(magnitude, timestamp)
    }

    /**
     * 가속도 - 중력 분리 (대체 방법, 필터 적용)
     */
    private fun processManualLinearAcceleration(timestamp: Long) {
        val linearX = accelValues[0] - gravityValues[0]
        val linearY = accelValues[1] - gravityValues[1]
        val linearZ = accelValues[2] - gravityValues[2]

        // 저역통과 필터 적용
        filteredAccel[0] = lowPassFilter(linearX, filteredAccel[0])
        filteredAccel[1] = lowPassFilter(linearY, filteredAccel[1])
        filteredAccel[2] = lowPassFilter(linearZ, filteredAccel[2])

        val magnitude = calculateMagnitude(filteredAccel[0], filteredAccel[1], filteredAccel[2])

        if (magnitude > SPIKE_REJECT_THRESHOLD) {
            return
        }

        accelBuffer.add(SensorData(timestamp, filteredAccel[0], filteredAccel[1], filteredAccel[2], magnitude))
        cleanOldData(accelBuffer, timestamp)

        analyzeImpact(magnitude, timestamp)
    }

    /**
     * 자이로스코프 처리 (필터 적용)
     */
    private fun processGyroscope(values: FloatArray, timestamp: Long) {
        // 저역통과 필터 적용
        filteredGyro[0] = lowPassFilter(values[0], filteredGyro[0])
        filteredGyro[1] = lowPassFilter(values[1], filteredGyro[1])
        filteredGyro[2] = lowPassFilter(values[2], filteredGyro[2])

        val magnitude = calculateMagnitude(filteredGyro[0], filteredGyro[1], filteredGyro[2])

        if (magnitude > SPIKE_REJECT_THRESHOLD) {
            return
        }

        gyroBuffer.add(SensorData(timestamp, filteredGyro[0], filteredGyro[1], filteredGyro[2], magnitude))
        cleanOldData(gyroBuffer, timestamp)
    }

    /**
     * 임팩트 + 자유낙하 분석 (개선된 로직)
     */
    private fun analyzeImpact(magnitude: Float, timestamp: Long) {
        // 쿨다운 체크
        if (timestamp - lastImpactTime < IMPACT_COOLDOWN_MS) {
            return
        }

        val impactThresholdG = sensitivity.impactThreshold * GRAVITY
        val freeFallThresholdG = sensitivity.freeFallThreshold * GRAVITY

        when (currentState) {
            DetectionState.NORMAL -> {
                // 자유낙하 감지
                if (magnitude < freeFallThresholdG) {
                    freeFallStartTime = timestamp
                    currentState = DetectionState.POTENTIAL_FALL
                    Log.d(TAG, "⚠️ Potential free fall detected: |a|=${String.format("%.2f", magnitude)} m/s²")
                }
                // 직접 충격 감지
                else if (magnitude > impactThresholdG) {
                    checkCrashCondition(magnitude, timestamp, "Direct Impact")
                }
            }

            DetectionState.POTENTIAL_FALL -> {
                val fallDuration = timestamp - freeFallStartTime

                // 자유낙하 후 충격
                if (magnitude > impactThresholdG) {
                    Log.d(TAG, "🔥 Free fall → Impact detected! Duration: ${fallDuration}ms")
                    checkCrashCondition(magnitude, timestamp, "Free Fall + Impact (${fallDuration}ms)")
                    currentState = DetectionState.NORMAL
                }
                // 자유낙하 지속 중
                else if (magnitude < freeFallThresholdG) {
                    if (fallDuration > WINDOW_SIZE_MS) {
                        // 너무 오래 지속되면 초기화 (센서 노이즈)
                        Log.d(TAG, "Free fall timeout, resetting")
                        currentState = DetectionState.NORMAL
                    }
                }
                // 자유낙하 종료 (충격 없음)
                else {
                    if (fallDuration < FREE_FALL_DURATION_MS) {
                        Log.v(TAG, "False free fall (too short: ${fallDuration}ms), resetting")
                    }
                    currentState = DetectionState.NORMAL
                }
            }

            else -> {}
        }
    }

    /**
     * 사고 판정 (자이로 + 임팩트, 추가 검증)
     */
    private fun checkCrashCondition(impactMagnitude: Float, timestamp: Long, reason: String) {
        // 최근 500ms 내의 자이로 데이터만 확인 (시간 상관성)
        val recentGyro = gyroBuffer.filter { timestamp - it.timestamp < 500 }
        val maxGyro = recentGyro.maxOfOrNull { it.magnitude } ?: 0f
        val gyroThreshold = sensitivity.rotationThreshold

        Log.d(TAG, "🔍 Checking crash: Impact=${String.format("%.2f", impactMagnitude/GRAVITY)}g, MaxGyro=${String.format("%.2f", maxGyro)} rad/s")

        // 판정 조건: 강한 충격 + 회전
        if (impactMagnitude > sensitivity.impactThreshold * GRAVITY && maxGyro > gyroThreshold) {
            crashDetectionCount++
            Log.e(TAG, "🚨🚨 CRASH DETECTED #${crashDetectionCount}! Reason: $reason")

            val event = CrashEvent(
                timestamp = timestamp,
                impactMagnitude = impactMagnitude / GRAVITY,
                rotationMagnitude = maxGyro,
                detectionReason = reason
            )

            _crashEvents.tryEmit(event)
            lastImpactTime = timestamp
            currentState = DetectionState.AWAIT_RESPONSE

            // 쿨다운 후 상태 복구
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (currentState == DetectionState.AWAIT_RESPONSE) {
                    currentState = DetectionState.NORMAL
                    Log.d(TAG, "Cooldown finished, back to NORMAL state")
                }
            }, IMPACT_COOLDOWN_MS)
        } else {
            Log.d(TAG, "✅ Not a crash (gyro=${String.format("%.2f", maxGyro)} < ${gyroThreshold} or impact too low)")
        }
    }

    /**
     * 저역통과 필터 (Low-pass filter)
     * 급격한 노이즈 제거, 부드러운 신호 유지
     */
    private fun lowPassFilter(current: Float, previous: Float): Float {
        return previous * LOW_PASS_ALPHA + current * (1 - LOW_PASS_ALPHA)
    }

    /**
     * 크기 계산
     */
    private fun calculateMagnitude(x: Float, y: Float, z: Float): Float {
        return sqrt(x * x + y * y + z * z)
    }

    /**
     * 오래된 데이터 정리 (롤링 윈도우)
     */
    private fun cleanOldData(buffer: MutableList<SensorData>, currentTime: Long) {
        buffer.removeAll { currentTime - it.timestamp > WINDOW_SIZE_MS }
    }

    /**
     * 디버그 정보 출력
     */
    fun getDebugInfo(): String {
        return """
            Samples: $sampleCount
            Crashes Detected: $crashDetectionCount
            State: $currentState
            Sensitivity: ${sensitivity.name}
            Accel Buffer: ${accelBuffer.size}
            Gyro Buffer: ${gyroBuffer.size}
        """.trimIndent()
    }
}
