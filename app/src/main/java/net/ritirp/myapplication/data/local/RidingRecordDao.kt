package net.ritirp.myapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 주행 기록 DAO
 */
@Dao
interface RidingRecordDao {
    @Insert
    suspend fun insert(record: RidingRecordEntity): Long

    @androidx.room.Update
    suspend fun update(record: RidingRecordEntity)

    @Query("SELECT * FROM riding_records ORDER BY startTime DESC")
    fun getAllRecords(): Flow<List<RidingRecordEntity>>

    @Query("SELECT * FROM riding_records WHERE id = :id")
    suspend fun getRecordById(id: Long): RidingRecordEntity?

    @Query("SELECT * FROM riding_records WHERE id = :id")
    suspend fun getRecord(id: Long): RidingRecordEntity?

    @Query("DELETE FROM riding_records WHERE id = :id")
    suspend fun deleteRecord(id: Long)

    @Query("DELETE FROM riding_records")
    suspend fun deleteAllRecords()

    @Query("SELECT * FROM riding_records ORDER BY startTime DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<RidingRecordEntity>>
}
