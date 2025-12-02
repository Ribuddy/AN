package net.ritirp.myapplication.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 지도 및 경로 관련 API
 */
interface MapApi {
    /**
     * 경로 조회 API
     * @param startX 출발지 경도 (longitude)
     * @param startY 출발지 위도 (latitude)
     * @param endX 도착지 경도 (longitude)
     * @param endY 도착지 위도 (latitude)
     * @return 경로 정보 (JSON)
     */
    @POST("/v1/map/routes")
    suspend fun getRoute(
        @Query("startX") startX: Double,
        @Query("startY") startY: Double,
        @Query("endX") endX: Double,
        @Query("endY") endY: Double,
    ): Response<ResponseBody>
}
