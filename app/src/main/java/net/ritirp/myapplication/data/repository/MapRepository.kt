package net.ritirp.myapplication.data.repository

import android.annotation.SuppressLint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.kakao.vectormap.LatLng
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import net.ritirp.myapplication.data.api.MapApi
import net.ritirp.myapplication.data.api.Poi
import net.ritirp.myapplication.data.api.PoiSearchRequest
import net.ritirp.myapplication.data.model.LocationData
import net.ritirp.myapplication.data.model.MarkerData
import net.ritirp.myapplication.data.model.MarkerType
import net.ritirp.myapplication.data.model.RouteData
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * 위치 및 지도 관련 데이터를 관리하는 Repository
 */
class MapRepository(
    private val fusedLocationClient: FusedLocationProviderClient,
    private val mapApi: MapApi,
) {
    private val _currentLocation = MutableStateFlow(LocationData.DEFAULT_SEOUL)
    val currentLocation: Flow<LocationData> = _currentLocation.asStateFlow()

    private val _destination = MutableStateFlow<LocationData?>(null)
    val destination: Flow<LocationData?> = _destination.asStateFlow()

    private val _route = MutableStateFlow<RouteData?>(null)
    val route: Flow<RouteData?> = _route.asStateFlow()

    private val _markers = MutableStateFlow<List<MarkerData>>(emptyList())
    val markers: Flow<List<MarkerData>> = _markers.asStateFlow()

    /**
     * 팀원 위치를 마커로 업데이트
     */
    fun updateTeamMemberMarkers(teamMemberLocations: List<net.ritirp.myapplication.data.model.TeamMemberLocation>) {
        val teamMarkers =
            teamMemberLocations.map { member ->
                MarkerData(
                    id = "team_${member.userId}",
                    location = LocationData(member.lat, member.lon),
                    title = member.memberName,
                    emoji = "👤",
                    type = MarkerType.TEAM_MEMBER,
                )
            }
        _markers.value = teamMarkers
        println("DEBUG: Updated team markers: ${teamMarkers.size} members")
    }

    /**
     * 팀 마커 초기화 (라이딩 종료 시)
     */
    fun clearTeamMarkers() {
        _markers.value = emptyList()
        println("DEBUG: Cleared team markers")
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationData =
        suspendCancellableCoroutine { continuation ->
            fusedLocationClient
                .getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token,
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        val locationData = LocationData(location.latitude, location.longitude)

                        // 에뮬레이터 위치 감지 및 서울로 대체
                        val finalLocation =
                            if (isEmulatorLocation(locationData)) {
                                LocationData.DEFAULT_SEOUL
                            } else {
                                locationData
                            }

                        _currentLocation.value = finalLocation
                        continuation.resume(finalLocation)
                    } else {
                        continuation.resume(LocationData.DEFAULT_SEOUL)
                    }
                }.addOnFailureListener {
                    continuation.resume(LocationData.DEFAULT_SEOUL)
                }
    }

    /**
     * 서버에서 경로 조회
     * @param start 출발지 위치
     * @param end 도착지 위치
     * @return LatLng 리스트 (경로 좌표들)
     */
    suspend fun getRoute(start: LocationData, end: LocationData): List<LatLng> {
        return try {
            println("DEBUG: Requesting route from server - start: (${start.latitude}, ${start.longitude}), end: (${end.latitude}, ${end.longitude})")

            val response = mapApi.getRoute(
                startX = start.longitude, // longitude -> X
                startY = start.latitude,  // latitude -> Y
                endX = end.longitude,
                endY = end.latitude
            )

            if (response.isSuccessful && response.body() != null) {
                val jsonString = response.body()!!.string()
                println("DEBUG: Route response: $jsonString")

                val routePoints = parseRouteResponse(jsonString)
                println("DEBUG: Parsed ${routePoints.size} route points from server")
                routePoints
            } else {
                println("DEBUG: Route request failed: ${response.code()} - ${response.message()}")
                // 실패 시 직선 경로 반환
                generateStraightRoute(start, end)
            }
        } catch (e: Exception) {
            println("DEBUG: Exception getting route from server: ${e.message}")
            e.printStackTrace()
            // 예외 발생 시 직선 경로 반환
            generateStraightRoute(start, end)
        }
    }

    /**
     * JSON 응답에서 경로 좌표 파싱 (Tmap API 형식)
     */
    private fun parseRouteResponse(jsonString: String): List<LatLng> {
        val routePoints = mutableListOf<LatLng>()

        try {
            val jsonObject = JSONObject(jsonString)

            // result.features 배열에서 LineString 타입의 coordinates 추출
            if (jsonObject.has("result")) {
                val result = jsonObject.getJSONObject("result")
                if (result.has("features")) {
                    val features = result.getJSONArray("features")

                    for (i in 0 until features.length()) {
                        val feature = features.getJSONObject(i)
                        val geometry = feature.getJSONObject("geometry")
                        val geometryType = geometry.getString("type")

                        // LineString 타입만 경로로 사용
                        if (geometryType == "LineString") {
                            val coordinates = geometry.getJSONArray("coordinates")
                            for (j in 0 until coordinates.length()) {
                                val point = coordinates.getJSONArray(j)
                                val lon = point.getDouble(0)
                                val lat = point.getDouble(1)
                                routePoints.add(LatLng.from(lat, lon))
                            }
                        }
                    }
                }
            }

            println("DEBUG: Successfully parsed ${routePoints.size} route points from Tmap API response")
        } catch (e: Exception) {
            println("DEBUG: Error parsing route response: ${e.message}")
            e.printStackTrace()
        }

        return routePoints
    }

    /**
     * 직선 경로 생성 (fallback)
     */
    private fun generateStraightRoute(start: LocationData, end: LocationData): List<LatLng> {
        val points = mutableListOf<LatLng>()
        val numPoints = 10

        for (i in 0..numPoints) {
            val ratio = i.toDouble() / numPoints
            val lat = start.latitude + (end.latitude - start.latitude) * ratio
            val lng = start.longitude + (end.longitude - start.longitude) * ratio
            points.add(LatLng.from(lat, lng))
        }

        println("DEBUG: Generated ${points.size} straight route points as fallback")
        return points
    }

    fun setDestination(location: LocationData) {
        println("DEBUG: Setting destination in repository: ${location.latitude}, ${location.longitude}")
        _destination.value = location
    }

    fun initializeTeamMarkers() {
        val teamMarkers =
            listOf(
                MarkerData(
                    id = "team_1",
                    location = LocationData(37.5700, 126.9800),
                    title = "팀원 1",
                    emoji = "👤",
                    type = MarkerType.TEAM_MEMBER,
                ),
                MarkerData(
                    id = "team_2",
                    location = LocationData(37.5600, 126.9700),
                    title = "팀원 2",
                    emoji = "👤",
                    type = MarkerType.TEAM_MEMBER,
                ),
                MarkerData(
                    id = "team_3",
                    location = LocationData(37.5750, 126.9850),
                    title = "팀원 3",
                    emoji = "👤",
                    type = MarkerType.TEAM_MEMBER,
                ),
            )
        _markers.value = teamMarkers
    }


    /**
     * 장소 검색
     * @param keyword 검색 키워드
     * @param centerLat 중심 위도 (반경 검색용)
     * @param centerLon 중심 경도 (반경 검색용)
     * @return 검색된 장소 목록
     */
    suspend fun searchPlace(
        keyword: String,
        centerLat: Double? = null,
        centerLon: Double? = null,
    ): List<Poi> {
        return try {
            println("DEBUG: Searching place with keyword: $keyword, center: ($centerLat, $centerLon)")

            val request = PoiSearchRequest(
                searchKeyword = keyword,
                centerLat = centerLat,
                centerLon = centerLon,
            )

            val response = mapApi.searchPoi(request)

            if (response.isSuccessful && response.body() != null) {
                val searchResponse = response.body()!!
                println("DEBUG: Response isSuccess: ${searchResponse.isSuccess}")
                println("DEBUG: Response code: ${searchResponse.code}")

                // result.searchPoiInfo.pois.poi 경로로 데이터 접근
                val pois = searchResponse.result?.searchPoiInfo?.pois?.poi ?: emptyList()
                println("DEBUG: Found ${pois.size} places")

                // 디버깅: 첫 번째 결과 출력
                if (pois.isNotEmpty()) {
                    val first = pois.first()
                    println("DEBUG: First place: ${first.name} at (${first.frontLat}, ${first.frontLon})")
                }

                pois
            } else {
                println("DEBUG: Search request failed: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            println("DEBUG: Exception searching place: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    private fun isEmulatorLocation(location: LocationData): Boolean =
        location.latitude in 37.0..38.0 && location.longitude in -123.0..-121.0
}
