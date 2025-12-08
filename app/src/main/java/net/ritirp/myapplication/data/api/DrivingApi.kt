package net.ritirp.myapplication.data.api

import net.ritirp.myapplication.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 라이딩 관련 API 인터페이스
 */
interface DrivingApi {
    /**
     * 팀 라이딩 시작
     */
    @POST("v1/driving/team/start/{teamId}")
    suspend fun startTeamRiding(
        @Header("Authorization") token: String,
        @Path("teamId") teamId: String,
        @Body request: StartTeamRidingRequest,
    ): Response<ApiResponse<String>> // ridingRecordId 반환

    /**
     * 내 위치 보고 & 팀 위치 조회
     */
    @POST("v1/driving/team/location/{ridingRecordId}")
    suspend fun updateLocationAndGetTeamLocations(
        @Header("Authorization") token: String,
        @Path("ridingRecordId") ridingRecordId: String,
        @Body location: LocationUpdateRequest,
    ): Response<ApiResponse<List<TeamMemberLocation>>>

    /**
     * 팀 라이딩 종료
     */
    @POST("v1/driving/team/end/{ridingRecordId}")
    suspend fun endTeamRiding(
        @Header("Authorization") token: String,
        @Path("ridingRecordId") ridingRecordId: String,
        @Body request: EndTeamRidingRequest,
    ): Response<ApiResponse<Unit>>

    /**
     * 주간 주행 통계 조회
     */
    @GET("v1/driving/statistics/weekly")
    suspend fun getWeeklyStatistics(
        @Query("startDate") startDate: String? = null,
    ): Response<ApiResponse<WeeklyStatisticsResponse>>

    /**
     * 월간 주행 통계 조회
     */
    @GET("v1/driving/statistics/monthly")
    suspend fun getMonthlyStatistics(
        @Query("year") year: Int? = null,
    ): Response<ApiResponse<MonthlyStatisticsResponse>>

    /**
     * 연간 주행 통계 조회
     */
    @GET("v1/driving/statistics/yearly")
    suspend fun getYearlyStatistics(): Response<ApiResponse<YearlyStatisticsResponse>>

    /**
     * 주행 리포트 조회
     */
    @GET("v1/driving/team/report")
    suspend fun getDrivingReport(
        @Query("ridingRecordId") ridingRecordId: String,
    ): Response<ApiResponse<RidingReportResponse>>
}
