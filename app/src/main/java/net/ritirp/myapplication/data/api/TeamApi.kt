package net.ritirp.myapplication.data.api

import net.ritirp.myapplication.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 팀 관련 API 인터페이스
 */
interface TeamApi {
    /**
     * 팀 생성
     */
    @POST("v1/users/team")
    suspend fun createTeam(
        @Header("Authorization") token: String,
        @Body request: CreateTeamRequest,
    ): Response<ApiResponse<String>> // String으로 변경 (teamId를 직접 반환)

    /**
     * 내가 속한 팀 목록 조회
     */
    @GET("v1/users/team/list")
    suspend fun getTeamList(
        @Header("Authorization") token: String,
    ): Response<ApiResponse<List<TeamInfo>>> // List로 변경 (배열을 직접 반환)

    /**
     * 팀 참여하기
     */
    @POST("v1/users/team/join")
    suspend fun joinTeam(
        @Header("Authorization") token: String,
        @Body request: JoinOrLeaveTeamRequest,
    ): Response<ApiResponse<Unit>>

    /**
     * 팀 탈퇴하기
     */
    @HTTP(method = "DELETE", path = "v1/users/team", hasBody = true)
    suspend fun leaveTeam(
        @Header("Authorization") token: String,
        @Body request: JoinOrLeaveTeamRequest,
    ): Response<ApiResponse<Unit>>

    /**
     * 팀 정보 조회
     */
    @GET("v1/users/team/info/{id}")
    suspend fun getTeamInfo(
        @Header("Authorization") token: String,
        @Path("id") teamId: String,
    ): Response<ApiResponse<TeamInfo>>

    /**
     * 팀 참여 코드 조회
     */
    @GET("v1/users/team/join-code/{id}")
    suspend fun getTeamJoinCode(
        @Header("Authorization") token: String,
        @Path("id") teamId: String,
    ): Response<ApiResponse<String>>
}
