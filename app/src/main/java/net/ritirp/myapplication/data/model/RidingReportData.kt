package net.ritirp.myapplication.data.model

/**
 * 주행 리포트 관련 데이터 모델
 */

/**
 * 주행 통계 요약
 */
data class RidingSummary(
    val totalDistanceKm: Double = 0.0,
    val totalDurationMinutes: Long = 0,
    val dailyDistances: List<DailyDistance> = emptyList(),
)

/**
 * 일별 주행 거리
 */
data class DailyDistance(
    val dayOfWeek: String, // Mo, Tu, We, Th, Fr, Sa, Su
    val distanceKm: Double,
)

/**
 * 주행 점수
 */
data class RidingScore(
    val totalScore: Int = 0,
    val operationSafetyScore: Int = 0, // 조작 안전
    val speedSafetyScore: Int = 0, // 속도 안전
    val leanStabilityScore: Int = 0, // 기울기 안정성
)

/**
 * 개선 포인트
 */
data class ImprovementPoint(
    val category: ImprovementCategory,
    val title: String,
    val description: String,
)

enum class ImprovementCategory {
    OPERATION, // 조작
    SPEED, // 속도
    LEAN_STABILITY, // 기울기 안정성
}

/**
 * 기간 필터
 */
enum class PeriodFilter {
    WEEK,
    MONTH,
    YEAR,
}

/**
 * 점수 필터
 */
enum class ScoreFilter {
    WEEK,
    TOTAL,
    MONTH,
}

/**
 * API 응답 모델 - 주간 통계
 */
data class WeeklyStatisticsResponse(
    val startDate: String,
    val endDate: String,
    val totalDistance: Double,
    val totalDuration: Long,
    val totalRideCount: Int,
    val dailyStats: List<DailyStatistics>,
)

/**
 * API 응답 모델 - 월간 통계
 */
data class MonthlyStatisticsResponse(
    val year: Int,
    val totalDistance: Double,
    val totalDuration: Long,
    val totalRideCount: Int,
    val monthlyStats: List<MonthlyStatistics>,
)

/**
 * API 응답 모델 - 연간 통계
 */
data class YearlyStatisticsResponse(
    val totalDistance: Double,
    val totalDuration: Long,
    val totalRideCount: Int,
    val yearlyStats: List<YearlyStatistics>,
)

/**
 * 일별 통계
 */
data class DailyStatistics(
    val date: String,
    val dayOfWeek: String,
    val distance: Double,
    val duration: Long,
    val rideCount: Int,
)

/**
 * 월별 통계
 */
data class MonthlyStatistics(
    val month: String,
    val distance: Double,
    val duration: Long,
    val rideCount: Int,
)

/**
 * 연도별 통계
 */
data class YearlyStatistics(
    val year: Int,
    val distance: Double,
    val duration: Long,
    val rideCount: Int,
)

/**
 * 주행 리포트 응답 (서버에서 받아오는 개별 주행 데이터)
 */
data class RidingReportResponse(
    val ridingRecordId: String,
    val startTime: String,
    val endTime: String?,
    val startLocation: LocationInfo?,
    val endLocation: LocationInfo?,
    val distanceMeters: Double,
    val durationMillis: Long,
    val maxSpeedKmh: Double,
    val avgSpeedKmh: Double,
    val maxLeanAngleDegrees: Double,
    val maxGravityForce: Double,
    val totalClimbMeters: Double,
    val totalFallMeters: Double,
    val routeCoordinates: List<RouteCoordinate>?,
)

/**
 * 위치 정보
 */
data class LocationInfo(
    val name: String?,
    val lat: Double,
    val lon: Double,
)

/**
 * 경로 좌표
 */
data class RouteCoordinate(
    val lat: Double,
    val lon: Double,
    val ele: Double?,
    val timestamp: String?,
)
