package net.ritirp.myapplication.data.model

import com.google.gson.annotations.SerializedName

/**
 * 라이딩 관련 데이터 모델
 */

// 팀 라이딩 시작 요청
data class StartTeamRidingRequest(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("ele") val ele: Double? = null,
    @SerializedName("name") val name: String? = null,
)

// 위치 업데이트 요청
data class LocationUpdateRequest(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("ele") val ele: Double? = null,
    @SerializedName("gravityForce") val gravityForce: Double? = null, // 중력 가속도 (G 단위)
    @SerializedName("leanAngle") val leanAngle: Double? = null, // 기울기 각도 (도 단위)
)

// 사고 발생 보고 요청
data class AccidentReportRequest(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("ele") val ele: Double? = null,
    @SerializedName("gravityForce") val gravityForce: Double? = null, // 중력 가속도 (G 단위)
    @SerializedName("leanAngle") val leanAngle: Double? = null, // 기울기 각도 (도 단위)
    @SerializedName("ridingRecordId") val ridingRecordId: String,
    @SerializedName("timestamp") val timestamp: String? = null,
)

// 급가속/급정거 보고 요청
data class SuddenSpeedChangeReportRequest(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("ele") val ele: Double? = null,
    @SerializedName("gravityForce") val gravityForce: Double? = null, // 중력 가속도 (G 단위)
    @SerializedName("leanAngle") val leanAngle: Double? = null, // 기울기 각도 (도 단위)
    @SerializedName("ridingRecordId") val ridingRecordId: String,
    @SerializedName("timestamp") val timestamp: String? = null,
)

// 팀 라이딩 종료 요청
data class EndTeamRidingRequest(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("ele") val ele: Double? = null,
    @SerializedName("name") val name: String? = null,
)

// 팀원 위치 정보
data class TeamMemberLocation(
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("nickname") val nickname: String? = null,
    @SerializedName("profileImage") val profileImage: String? = null,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("ele") val ele: Double? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
) {
    // UI 표시를 위한 추가 필드
    val memberName: String get() = nickname ?: name ?: "사용자 $userId"
    val distance: String get() = "0.0" // TODO: 실제 거리 계산
    val isRiding: Boolean get() = true // TODO: 실제 라이딩 상태 확인
}

// 라이딩 상태
enum class RidingStatus {
    IDLE, // 대기 중
    RIDING, // 라이딩 중
    PAUSED, // 일시 정지
    ENDED, // 종료됨
}

// 위치 업데이트 응답 (팀 위치, 사고 정보, 경로 등 포함)
data class LocationUpdateResponse(
    @SerializedName("teamMemberLocations") val teamMemberLocations: List<TeamMemberLocation>? = null,
    @SerializedName("accidents") val accidents: List<AccidentInfo>? = null,
    @SerializedName("route") val route: RouteInfo? = null,
)

// 사고 정보
data class AccidentInfo(
    @SerializedName("userId") val userId: String,
    @SerializedName("userName") val userName: String? = null,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("timestamp") val timestamp: String? = null,
)

// 경로 정보
data class RouteInfo(
    @SerializedName("teamId") val teamId: String? = null,
    @SerializedName("path") val path: List<PathPoint>? = null,
)

// 경로 포인트
data class PathPoint(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
)
