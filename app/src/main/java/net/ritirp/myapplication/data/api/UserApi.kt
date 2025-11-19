package net.ritirp.myapplication.data.api

import net.ritirp.myapplication.data.model.ApiResponse
import retrofit2.Response
import retrofit2.http.GET

/**
 * 사용자 API 인터페이스
 */
interface UserApi {
    /**
     * 내 정보 조회
     */
    @GET("/v1/users/me")
    suspend fun getMyProfile(): Response<ApiResponse<UserProfileResponse>>
}

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
    val ridingRecords: Int?,
)
