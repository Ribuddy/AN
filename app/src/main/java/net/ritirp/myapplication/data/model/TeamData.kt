package net.ritirp.myapplication.data.model

import com.google.gson.annotations.SerializedName

/**
 * 팀 관련 데이터 모델
 */

// 팀 생성 요청
data class CreateTeamRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("members") val members: List<String>,
    @SerializedName("isCrew") val isCrew: Boolean = false,
)

// 팀 참여/탈퇴 요청
data class JoinOrLeaveTeamRequest(
    @SerializedName("id") val id: String,
)

// 팀 정보
data class TeamInfo(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("isCrew") val isCrew: Boolean = false,
    @SerializedName("members") val members: List<TeamMember>? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
)

// 팀 멤버 정보
data class TeamMember(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("nickname") val nickname: String? = null,
    @SerializedName("profileImage") val profileImage: String? = null,
)

// 팀 목록 응답
data class TeamListResponse(
    @SerializedName("teams") val teams: List<TeamInfo>,
)

// 팀 생성 응답
data class CreateTeamResponse(
    @SerializedName("teamId") val teamId: String,
)
