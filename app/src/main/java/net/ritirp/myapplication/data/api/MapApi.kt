package net.ritirp.myapplication.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
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

    /**
     * 장소 검색 API
     * @param request 검색 요청 데이터
     * @return 검색 결과 (JSON)
     */
    @POST("/v1/map/search")
    suspend fun searchPoi(
        @Body request: PoiSearchRequest,
    ): Response<PoiSearchResponse>
}

/**
 * POI 검색 요청 데이터
 */
data class PoiSearchRequest(
    val searchKeyword: String,
    val centerLon: Double? = null,
    val centerLat: Double? = null,
)

/**
 * POI 검색 응답 데이터 (서버 Wrapper)
 */
data class PoiSearchResponse(
    val isSuccess: Boolean? = null,
    val code: String? = null,
    val message: String? = null,
    val result: PoiSearchResult? = null,
    val error: Any? = null,
)

data class PoiSearchResult(
    val searchPoiInfo: SearchPoiInfo? = null,
)

data class SearchPoiInfo(
    val totalCount: String? = null,
    val count: String? = null,
    val page: String? = null,
    val pois: Pois? = null,
)

data class Pois(
    val poi: List<Poi>? = null,
)

data class Poi(
    val id: String? = null,
    val name: String? = null,
    val telNo: String? = null,
    val frontLat: String? = null,
    val frontLon: String? = null,
    val noorLat: String? = null,
    val noorLon: String? = null,
    val upperAddrName: String? = null,
    val middleAddrName: String? = null,
    val lowerAddrName: String? = null,
    val detailAddrName: String? = null,
    val mlClass: String? = null,
    val firstNo: String? = null,
    val secondNo: String? = null,
    val roadName: String? = null,
    val buildingNo1: String? = null,
    val buildingNo2: String? = null,
    val rpFlag: String? = null,
    val parkFlag: String? = null,
    val detailBldgName: String? = null,
    val desc: String? = null,
) {
    fun getFullAddress(): String {
        val parts = listOfNotNull(
            upperAddrName,
            middleAddrName,
            lowerAddrName,
            detailAddrName,
        ).filter { it.isNotBlank() }
        return parts.joinToString(" ")
    }

    fun getLatitude(): Double? = frontLat?.toDoubleOrNull()
    fun getLongitude(): Double? = frontLon?.toDoubleOrNull()
}
