package net.ritirp.myapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 주행 기록 엔티티
 */
@Entity(tableName = "riding_records")
data class RidingRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val totalDistance: Double, // 미터
    val topSpeed: Float, // m/s
    val averageSpeed: Float, // m/s
    val maxLeanAngle: Float, // 도
    val totalClimb: Double, // 미터
    val totalFall: Double, // 미터
    val duration: Long, // 밀리초
    val teamName: String? = null, // 팀 라이딩인 경우 팀 이름
)
