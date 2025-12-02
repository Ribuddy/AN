package net.ritirp.myapplication.data.local.converter

import androidx.room.TypeConverter
import java.util.Date

/**
 * Date 타입을 Long으로 변환하는 Converter
 */
class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
