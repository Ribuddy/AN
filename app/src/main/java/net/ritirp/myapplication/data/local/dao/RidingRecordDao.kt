package net.ritirp.myapplication.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.ritirp.myapplication.data.local.entity.RidingRecordEntity

/**
 * 주행 기록 DAO
 */
@Dao
interface RidingRecordDao {
    /**
     * 주행 기록 삽입
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: RidingRecordEntity): Long

    /**
     * 주행 기록 업데이트
     */
    @Update
    suspend fun update(record: RidingRecordEntity)

    /**
     * 주행 기록 삭제
     */
    @Delete
    suspend fun delete(record: RidingRecordEntity)

    /**
     * ID로 주행 기록 조회
     */
    @Query("SELECT * FROM riding_records WHERE id = :id")
    suspend fun getById(id: Long): RidingRecordEntity?

    /**
     * 모든 주행 기록 조회 (최신순)
     */
    @Query("SELECT * FROM riding_records ORDER BY startTime DESC")
    fun getAllRecords(): Flow<List<RidingRecordEntity>>

    /**
     * 완료된 주행 기록만 조회
     */
    @Query("SELECT * FROM riding_records WHERE isCompleted = 1 ORDER BY startTime DESC")
    fun getCompletedRecords(): Flow<List<RidingRecordEntity>>

    /**
     * 진행 중인 주행 기록 조회
     */
    @Query("SELECT * FROM riding_records WHERE isCompleted = 0 ORDER BY startTime DESC LIMIT 1")
    suspend fun getOngoingRecord(): RidingRecordEntity?

    /**
     * 서버에 동기화되지 않은 기록 조회
     */
    @Query("SELECT * FROM riding_records WHERE isCompleted = 1 AND isSyncedToServer = 0")
    suspend fun getUnsyncedRecords(): List<RidingRecordEntity>

    /**
     * 특정 팀의 주행 기록 조회
     */
    @Query("SELECT * FROM riding_records WHERE teamId = :teamId ORDER BY startTime DESC")
    fun getRecordsByTeam(teamId: String): Flow<List<RidingRecordEntity>>

    /**
     * 날짜 범위로 주행 기록 조회
     */
    @Query("SELECT * FROM riding_records WHERE startTime BETWEEN :startDate AND :endDate ORDER BY startTime DESC")
    fun getRecordsByDateRange(
        startDate: Long,
        endDate: Long,
    ): Flow<List<RidingRecordEntity>>

    /**
     * 전체 주행 통계 조회
     */
    @Query(
        """
        SELECT 
            COUNT(*) as totalRides,
            SUM(distanceMeters) as totalDistance,
            SUM(durationMillis) as totalDuration,
            AVG(averageSpeedKmh) as avgSpeed,
            MAX(maxSpeedKmh) as maxSpeed
        FROM riding_records 
        WHERE isCompleted = 1
    """,
    )
    suspend fun getTotalStats(): RidingStats?
}

/**
 * 주행 통계 데이터 클래스
 */
data class RidingStats(
    val totalRides: Int,
    val totalDistance: Double,
    val totalDuration: Long,
    val avgSpeed: Double,
    val maxSpeed: Double,
)
