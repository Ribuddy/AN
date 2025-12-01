package net.ritirp.myapplication.data.api

import net.ritirp.myapplication.data.model.ApiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * 사용자 API 인터페이스
 */
interface UserApi {
    /**
     * 내 정보 조회
     */
    @GET("/v1/users/me")
    suspend fun getMyProfile(): Response<ApiResponse<UserProfileResponse>>

    /**
     * 친구 목록 조회
     */
    @GET("/v1/users/friends")
    suspend fun getFriendList(): Response<ApiResponse<List<FriendResponse>>>

    /**
     * 팀 목록 조회
     */
    @GET("/v1/users/teams")
    suspend fun getTeamList(): Response<ApiResponse<List<TeamResponse>>>

    /**
     * 사용자 정보 수정
     */
    @POST("/v1/users/edit")
    suspend fun editUserProfile(@Body request: EditUserProfileRequest): Response<ApiResponse<Unit>>
}

/**
 * 사용자 정보 수정 요청 DTO
 */
data class EditUserProfileRequest(
    val name: String? = null,
    val nickname: String? = null,
    val oneLineIntroduction: String? = null,
    val ribuddyId: String? = null,
    val profileImage: String? = null,
)

/**
 * 사용자 프로필 응답 DTO
 */
data class UserProfileResponse(
    val id: String,
    val name: String,
    val nickname: String?,
    val introduction: String?,
    val ribuddyId: String,
    val profileImageUrl: String?,
    val createdAt: String?,
    val teams: List<String>?,
    val teamCount: Int?,
    val ridingRecords: Int?,
    val friends: List<FriendResponse>?,
)

/**
 * 친구 응답 DTO
 */
data class FriendResponse(
    val id: String,
    val name: String,
    val nickname: String?,
    val ribuddyId: String,
    val profileImageUrl: String?,
    val isFavorite: Boolean?,
)

/**
 * 팀 응답 DTO
 */
data class TeamResponse(
    val id: String,
    val name: String,
    val description: String?,
    val memberCount: Int?,
)
