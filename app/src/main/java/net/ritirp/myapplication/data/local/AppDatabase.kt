package net.ritirp.myapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.ritirp.myapplication.data.local.converter.DateConverter
import net.ritirp.myapplication.data.local.converter.LocationPointListConverter
import net.ritirp.myapplication.data.local.dao.RidingRecordDao
import net.ritirp.myapplication.data.local.entity.RidingRecordEntity

/**
 * RiBuddy 앱의 로컬 데이터베이스
 */
@Database(
    entities = [RidingRecordEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(DateConverter::class, LocationPointListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ridingRecordDao(): RidingRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "ribuddy_database",
                    )
                        .fallbackToDestructiveMigration() // 개발 중에만 사용
                        .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
