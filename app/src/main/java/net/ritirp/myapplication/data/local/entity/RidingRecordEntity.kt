package net.ritirp.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import net.ritirp.myapplication.data.local.converter.DateConverter
import net.ritirp.myapplication.data.local.converter.LocationPointListConverter
import java.util.Date

/**
 * 주행 기록 Entity
 */
@Entity(tableName = "riding_records")
@TypeConverters(DateConverter::class, LocationPointListConverter::class)
data class RidingRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // 기본 정보
    val startTime: Date,
    val endTime: Date? = null,
    val durationMillis: Long = 0, // 주행 시간 (밀리초)
    // 거리 정보
    val distanceMeters: Double = 0.0, // 총 주행 거리 (미터)
    // 속도 정보
    val averageSpeedKmh: Double = 0.0, // 평균 속도 (km/h)
    val maxSpeedKmh: Double = 0.0, // 최고 속도 (km/h)
    // 고도 정보
    val totalClimbMeters: Double = 0.0, // 총 상승 고도 (미터)
    val totalFallMeters: Double = 0.0, // 총 하강 고도 (미터)
    val maxElevation: Double? = null, // 최고 고도
    val minElevation: Double? = null, // 최저 고도
    // 기울기 각도 정보 (좌우 기울기)
    val maxLeanAngleDegrees: Double = 0.0, // 최대 기울기 각도 (절대값)
    val avgLeanAngleDegrees: Double = 0.0, // 평균 기울기 각도 (절대값)
    // 팀 정보
    val teamId: String? = null,
    val teamName: String? = null,
    // 위치 정보
    val startLocationName: String? = null,
    val startLat: Double,
    val startLon: Double,
    val startEle: Double? = null,
    val endLocationName: String? = null,
    val endLat: Double? = null,
    val endLon: Double? = null,
    val endEle: Double? = null,
    // 경로 정보 (GPS 좌표들)
    val routePoints: List<LocationPoint> = emptyList(),
    // 상태
    val isCompleted: Boolean = false, // 종료 여부
    val isSyncedToServer: Boolean = false, // 서버 동기화 여부
    val serverRecordId: String? = null, // 서버에서 발급한 ID
    // 메타 정보
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
)

/**
 * GPS 위치 포인트
 */
data class LocationPoint(
    val lat: Double,
    val lon: Double,
    val ele: Double? = null,
    val timestamp: Long, // Unix timestamp
    val speedKmh: Double? = null, // 해당 지점의 속도
    val accuracy: Float? = null, // GPS 정확도 (미터)
    val leanAngleDegrees: Double? = null, // 좌우 기울기 각도 (왼쪽: -, 오른쪽: +)
)
