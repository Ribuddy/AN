package net.ritirp.myapplication.data.api

import net.ritirp.myapplication.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 친구 관련 API 인터페이스
 */
interface FriendApi {
    /**
     * 라이버디 ID로 친구 추가
     */
    @POST("v1/users/friend")
    suspend fun addFriendByRibuddyId(
        @Header("Authorization") token: String,
        @Body request: AddFriendByRibuddyIdRequest
    ): Response<ApiResponse<Unit>>

    /**
     * 친구 삭제
     */
    @HTTP(method = "DELETE", path = "v1/users/friend", hasBody = true)
    suspend fun deleteFriend(
        @Header("Authorization") token: String,
        @Body request: DeleteFriendRequest
    ): Response<ApiResponse<Unit>>

    /**
     * 친구 즐겨찾기 설정/해제
     */
    @PATCH("v1/users/friend")
    suspend fun editFriendStatus(
        @Header("Authorization") token: String,
        @Body request: EditFriendStatusRequest
    ): Response<ApiResponse<Unit>>

    /**
     * 친구 목록 조회
     */
    @GET("v1/users/friend/list")
    suspend fun getFriendList(
        @Header("Authorization") token: String
    ): Response<ApiResponse<List<FriendInfo>>>  // List로 직접 반환
}
