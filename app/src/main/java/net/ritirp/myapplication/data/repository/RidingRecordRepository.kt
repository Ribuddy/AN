package net.ritirp.myapplication.data.repository

import kotlinx.coroutines.flow.Flow
import net.ritirp.myapplication.data.local.RidingRecordDao
import net.ritirp.myapplication.data.local.RidingRecordEntity
import net.ritirp.myapplication.data.model.RidingMetrics

/**
 * 주행 기록 Repository
 */
class RidingRecordRepository(
    private val ridingRecordDao: RidingRecordDao,
) {
    /**
     * 주행 기록 저장
     */
    suspend fun saveRidingRecord(
        metrics: RidingMetrics,
        teamName: String? = null,
    ): Long {
        val entity =
            RidingRecordEntity(
                startTime = metrics.startTime,
                endTime = metrics.endTime ?: System.currentTimeMillis(),
                totalDistance = metrics.totalDistance,
                topSpeed = metrics.topSpeed,
                averageSpeed = metrics.averageSpeed,
                maxLeanAngle = metrics.maxLeanAngle,
                totalClimb = metrics.totalClimb,
                totalFall = metrics.totalFall,
                duration = metrics.duration,
                teamName = teamName,
            )
        return ridingRecordDao.insert(entity)
    }

    /**
     * 모든 주행 기록 가져오기
     */
    fun getAllRecords(): Flow<List<RidingRecordEntity>> {
        return ridingRecordDao.getAllRecords()
    }

    /**
     * 최근 주행 기록 가져오기
     */
    fun getRecentRecords(limit: Int = 10): Flow<List<RidingRecordEntity>> {
        return ridingRecordDao.getRecentRecords(limit)
    }

    /**
     * 주행 기록 삭제
     */
    suspend fun deleteRecord(id: Long) {
        ridingRecordDao.deleteRecord(id)
    }

    /**
     * 모든 주행 기록 삭제
     */
    suspend fun deleteAllRecords() {
        ridingRecordDao.deleteAllRecords()
    }
}
