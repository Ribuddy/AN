package net.ritirp.myapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 주행 기록 데이터베이스
 */
@Database(
    entities = [RidingRecordEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class RidingRecordDatabase : RoomDatabase() {
    abstract fun ridingRecordDao(): RidingRecordDao

    companion object {
        @Volatile
        private var INSTANCE: RidingRecordDatabase? = null

        fun getDatabase(context: Context): RidingRecordDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        RidingRecordDatabase::class.java,
                        "riding_record_database",
                    ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
