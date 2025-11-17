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
    @SerializedName("name") val name: String? = null
)

// 위치 업데이트 요청
data class LocationUpdateRequest(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("ele") val ele: Double? = null
)

// 팀 라이딩 종료 요청
data class EndTeamRidingRequest(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("ele") val ele: Double? = null,
    @SerializedName("name") val name: String? = null
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
    @SerializedName("updatedAt") val updatedAt: String? = null
) {
    // UI 표시를 위한 추가 필드
    val memberName: String get() = nickname ?: name ?: "사용자 ${userId}"
    val distance: String get() = "0.0" // TODO: 실제 거리 계산
    val isRiding: Boolean get() = true // TODO: 실제 라이딩 상태 확인
}

// 라이딩 상태
enum class RidingStatus {
    IDLE,       // 대기 중
    RIDING,     // 라이딩 중
    PAUSED,     // 일시 정지
    ENDED       // 종료됨
}
