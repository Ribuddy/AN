package net.ritirp.myapplication.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * 오토바이 기울기 각도를 측정하는 센서 매니저
 *
 * 좌회전/우회전 시 좌우 기울기 각도를 실시간으로 측정합니다.
 */
class LeanAngleSensorManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val _leanAngle = MutableStateFlow(0.0)
    val leanAngle: StateFlow<Double> = _leanAngle.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private var hasGravityData = false
    private var hasMagneticData = false

    /**
     * 센서 측정 시작
     */
    fun start() {
        if (_isActive.value) {
            Log.w(TAG, "센서가 이미 활성화되어 있습니다.")
            return
        }

        accelerometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI, // UI 업데이트에 적합한 속도
            )
            Log.d(TAG, "가속도계 센서 시작")
        } ?: Log.e(TAG, "가속도계 센서를 사용할 수 없습니다.")

        magnetometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI,
            )
            Log.d(TAG, "지자기 센서 시작")
        } ?: Log.e(TAG, "지자기 센서를 사용할 수 없습니다.")

        _isActive.value = true
    }

    /**
     * 센서 측정 중지
     */
    fun stop() {
        if (!_isActive.value) {
            return
        }

        sensorManager.unregisterListener(this)
        hasGravityData = false
        hasMagneticData = false
        _isActive.value = false
        _leanAngle.value = 0.0
        Log.d(TAG, "센서 중지")
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // 중력 가속도 값 저장
                System.arraycopy(event.values, 0, gravity, 0, gravity.size)
                hasGravityData = true
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                // 지자기 값 저장
                System.arraycopy(event.values, 0, geomagnetic, 0, geomagnetic.size)
                hasMagneticData = true
            }
        }

        // 두 센서 데이터가 모두 있을 때만 계산
        if (hasGravityData && hasMagneticData) {
            calculateLeanAngle()
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) {
        // 정확도 변경 시 처리 (필요시 구현)
    }

    /**
     * 기울기 각도 계산
     */
    private fun calculateLeanAngle() {
        // 회전 행렬 계산
        val success =
            SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                gravity,
                geomagnetic,
            )

        if (success) {
            // 방향 각도 계산 (azimuth, pitch, roll)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            // Roll 값이 좌우 기울기 각도
            // orientationAngles[2] = roll (라디안 단위)
            // 왼쪽 기울기: 음수, 오른쪽 기울기: 양수
            val rollRadians = orientationAngles[2]
            val rollDegrees = Math.toDegrees(rollRadians.toDouble())

            // 기울기 각도 업데이트
            _leanAngle.value = rollDegrees

            // 큰 기울기 감지 시 로그 (30도 이상)
            if (abs(rollDegrees) > 30.0) {
                val direction = if (rollDegrees > 0) "우회전" else "좌회전"
                Log.d(TAG, "큰 기울기 감지: $direction ${"%.1f".format(abs(rollDegrees))}°")
            }
        }
    }

    /**
     * 간단한 기울기 각도 계산 (가속도계만 사용)
     * 지자기 센서가 없을 경우 대체 방법
     */
    private fun calculateSimpleLeanAngle() {
        // X축 가속도와 Z축 가속도를 이용한 기울기 계산
        val x = gravity[0]
        val y = gravity[1]
        val z = gravity[2]

        // Roll 각도 계산 (라디안)
        val rollRadians = atan2(x.toDouble(), sqrt((y * y + z * z).toDouble()))
        val rollDegrees = Math.toDegrees(rollRadians)

        _leanAngle.value = rollDegrees
    }

    companion object {
        private const val TAG = "LeanAngleSensor"
    }
}
