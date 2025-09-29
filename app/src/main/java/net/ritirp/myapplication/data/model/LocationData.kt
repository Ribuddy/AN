package net.ritirp.myapplication.data.model

import com.kakao.vectormap.LatLng

/**
 * 위치 관련 데이터 모델
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double
) {
    fun toLatLng(): LatLng = LatLng.from(latitude, longitude)

    companion object {
        fun fromLatLng(latLng: LatLng): LocationData {
            return LocationData(latLng.latitude, latLng.longitude)
        }

        // 서울 시청 기본 위치
        val DEFAULT_SEOUL = LocationData(37.5666102, 126.9783881)
    }
}

/**
 * 경로 데이터
 */
data class RouteData(
    val start: LocationData,
    val destination: LocationData,
    val routePoints: List<LocationData> = emptyList()
)

/**
 * 마커 데이터
 */
data class MarkerData(
    val id: String,
    val location: LocationData,
    val title: String,
    val emoji: String,
    val type: MarkerType
)

enum class MarkerType {
    CURRENT_LOCATION,
    DESTINATION,
    TEAM_MEMBER
}
