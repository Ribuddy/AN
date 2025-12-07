package net.ritirp.myapplication.data.repository

import android.content.Context
import android.location.Location
import android.util.Log
import kotlinx.coroutines.flow.Flow
import net.ritirp.myapplication.data.local.AppDatabase
import net.ritirp.myapplication.data.local.entity.LocationPoint
import net.ritirp.myapplication.data.local.entity.RidingRecordEntity
import java.util.Date

/**
 * 로컬 주행 기록 Repository
 */
class LocalRidingRecordRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).ridingRecordDao()

    /**
     * 새로운 주행 기록 시작
     */
    suspend fun startRiding(
        teamId: String?,
        teamName: String?,
        startLat: Double,
        startLon: Double,
        startEle: Double? = null,
        startLocationName: String? = null,
        startTime: Date = Date(), // 시작 시간을 직접 지정 가능 (기본값: 현재 시간)
    ): Result<Long> {
        return try {
            val record =
                RidingRecordEntity(
                    startTime = startTime,
                    teamId = teamId,
                    teamName = teamName,
                    startLat = startLat,
                    startLon = startLon,
                    startEle = startEle,
                    startLocationName = startLocationName,
                    isCompleted = false,
                )

            val recordId = dao.insert(record)
            Log.d("LocalRidingRecord", "주행 시작 저장 성공: id=$recordId, startTime=$startTime")
            Result.success(recordId)
        } catch (e: Exception) {
            Log.e("LocalRidingRecord", "주행 시작 저장 실패", e)
            Result.failure(e)
        }
    }

    /**
     * 주행 중 위치 업데이트 (GPS 좌표 추가)
     */
    suspend fun updateLocation(
        recordId: Long,
        lat: Double,
        lon: Double,
        ele: Double? = null,
        speedKmh: Double? = null,
        accuracy: Float? = null,
        leanAngleDegrees: Double? = null,
    ): Result<Unit> {
        return try {
            val record = dao.getById(recordId) ?: return Result.failure(Exception("Record not found"))

            // 새 위치 포인트 추가
            val newPoint =
                LocationPoint(
                    lat = lat,
                    lon = lon,
                    ele = ele,
                    timestamp = System.currentTimeMillis(),
                    speedKmh = speedKmh,
                    accuracy = accuracy,
                    leanAngleDegrees = leanAngleDegrees,
                )

            val updatedPoints = record.routePoints + newPoint

            // 거리 계산 (이전 포인트와의 거리)
            var totalDistance = record.distanceMeters
            if (record.routePoints.isNotEmpty()) {
                val lastPoint = record.routePoints.last()
                val distance =
                    calculateDistance(
                        lastPoint.lat,
                        lastPoint.lon,
                        lat,
                        lon,
                    )
                totalDistance += distance
            }

            // 최고 속도 업데이트
            val maxSpeed =
                if (speedKmh != null && speedKmh > record.maxSpeedKmh) {
                    speedKmh
                } else {
                    record.maxSpeedKmh
                }

            // 평균 속도 계산
            val speeds = updatedPoints.mapNotNull { it.speedKmh }
            val avgSpeed = if (speeds.isNotEmpty()) speeds.average() else 0.0

            // 고도 변화 계산
            var totalClimb = record.totalClimbMeters
            var totalFall = record.totalFallMeters
            var maxEle = record.maxElevation
            var minEle = record.minElevation

            if (ele != null) {
                // 최고/최저 고도 업데이트
                maxEle = if (maxEle == null || ele > maxEle) ele else maxEle
                minEle = if (minEle == null || ele < minEle) ele else minEle

                // 상승/하강 계산
                if (record.routePoints.isNotEmpty()) {
                    val lastPoint = record.routePoints.last()
                    lastPoint.ele?.let { lastEle ->
                        val elevationDiff = ele - lastEle
                        if (elevationDiff > 0) {
                            totalClimb += elevationDiff
                        } else if (elevationDiff < 0) {
                            totalFall += kotlin.math.abs(elevationDiff)
                        }
                    }
                }
            }

            // 기울기 각도 계산
            val leanAngles = updatedPoints.mapNotNull { it.leanAngleDegrees?.let { angle -> kotlin.math.abs(angle) } }
            val maxLeanAngle = if (leanAngles.isNotEmpty()) leanAngles.maxOrNull() ?: 0.0 else record.maxLeanAngleDegrees
            val avgLeanAngle = if (leanAngles.isNotEmpty()) leanAngles.average() else 0.0

            val updatedRecord =
                record.copy(
                    routePoints = updatedPoints,
                    distanceMeters = totalDistance,
                    maxSpeedKmh = maxSpeed,
                    averageSpeedKmh = avgSpeed,
                    totalClimbMeters = totalClimb,
                    totalFallMeters = totalFall,
                    maxElevation = maxEle,
                    minElevation = minEle,
                    maxLeanAngleDegrees = maxLeanAngle,
                    avgLeanAngleDegrees = avgLeanAngle,
                    updatedAt = Date(),
                )

            dao.update(updatedRecord)
            Log.d(
                "LocalRidingRecord",
                "위치 업데이트 성공: 포인트 ${updatedPoints.size}개, 거리 ${totalDistance}m, 상승 ${totalClimb}m, 하강 ${totalFall}m, 최대기울기 $maxLeanAngle°",
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("LocalRidingRecord", "위치 업데이트 실패", e)
            Result.failure(e)
        }
    }

    /**
     * 주행 종료
     */
    suspend fun endRiding(
        recordId: Long,
        endLat: Double,
        endLon: Double,
        endEle: Double? = null,
        endLocationName: String? = null,
    ): Result<Unit> {
        return try {
            val record = dao.getById(recordId) ?: return Result.failure(Exception("Record not found"))

            val endTime = Date()
            val duration = endTime.time - record.startTime.time

            val updatedRecord =
                record.copy(
                    endTime = endTime,
                    endLat = endLat,
                    endLon = endLon,
                    endEle = endEle,
                    endLocationName = endLocationName,
                    durationMillis = duration,
                    isCompleted = true,
                    updatedAt = Date(),
                )

            dao.update(updatedRecord)
            Log.d("LocalRidingRecord", "주행 종료 저장 성공: 거리=${updatedRecord.distanceMeters}m, 시간=${duration}ms")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("LocalRidingRecord", "주행 종료 저장 실패", e)
            Result.failure(e)
        }
    }

    /**
     * 현재 진행 중인 주행 기록 조회
     */
    suspend fun getOngoingRecord(): RidingRecordEntity? {
        return dao.getOngoingRecord()
    }

    /**
     * 모든 주행 기록 조회 (Flow)
     */
    fun getAllRecords(): Flow<List<RidingRecordEntity>> {
        return dao.getAllRecords()
    }

    /**
     * 완료된 주행 기록만 조회
     */
    fun getCompletedRecords(): Flow<List<RidingRecordEntity>> {
        return dao.getCompletedRecords()
    }

    /**
     * ID로 주행 기록 조회
     */
    suspend fun getRecordById(id: Long): RidingRecordEntity? {
        return dao.getById(id)
    }

    /**
     * 주행 기록 삭제
     */
    suspend fun deleteRecord(record: RidingRecordEntity): Result<Unit> {
        return try {
            dao.delete(record)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 두 GPS 좌표 사이의 거리 계산 (미터)
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
}
