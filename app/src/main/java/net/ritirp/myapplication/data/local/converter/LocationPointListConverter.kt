package net.ritirp.myapplication.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.ritirp.myapplication.data.local.entity.LocationPoint

/**
 * LocationPoint 리스트를 JSON String으로 변환하는 Converter
 */
class LocationPointListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromLocationPointList(value: List<LocationPoint>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toLocationPointList(value: String?): List<LocationPoint>? {
        return value?.let {
            val listType = object : TypeToken<List<LocationPoint>>() {}.type
            gson.fromJson(it, listType)
        }
    }
}
