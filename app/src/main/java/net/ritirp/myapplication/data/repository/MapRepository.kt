package net.ritirp.myapplication.data.repository

import android.annotation.SuppressLint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import net.ritirp.myapplication.data.model.LocationData
import net.ritirp.myapplication.data.model.MarkerData
import net.ritirp.myapplication.data.model.MarkerType
import net.ritirp.myapplication.data.model.RouteData
import kotlin.coroutines.resume

/**
 * 위치 및 지도 관련 데이터를 관리하는 Repository
 */
class MapRepository(
    private val fusedLocationClient: FusedLocationProviderClient,
) {
    private val _currentLocation = MutableStateFlow(LocationData.DEFAULT_SEOUL)
    val currentLocation: Flow<LocationData> = _currentLocation.asStateFlow()

    private val _destination = MutableStateFlow<LocationData?>(null)
    val destination: Flow<LocationData?> = _destination.asStateFlow()

    private val _route = MutableStateFlow<RouteData?>(null)
    val route: Flow<RouteData?> = _route.asStateFlow()

    private val _markers = MutableStateFlow<List<MarkerData>>(emptyList())
    val markers: Flow<List<MarkerData>> = _markers.asStateFlow()

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

    fun setDestination(location: LocationData) {
        println("DEBUG: Setting destination in repository: ${location.latitude}, ${location.longitude}")
        _destination.value = location
        updateRoute()
    }

    private fun updateRoute() {
        val current = _currentLocation.value
        val dest = _destination.value

        println(
            "DEBUG: Updating route. Current: ${current.latitude}, ${current.longitude}, Dest: ${dest?.let {
                "${it.latitude}, ${it.longitude}"
            } ?: "null"}",
        )

        if (dest != null) {
            // 간단한 직선 경로 생성
            val routePoints = generateRoutePoints(current, dest)
            _route.value = RouteData(current, dest, routePoints)
            println("DEBUG: Route created with ${routePoints.size} points")
        } else {
            _route.value = null
            println("DEBUG: Route cleared")
        }
    }

    private fun generateRoutePoints(
        start: LocationData,
        end: LocationData,
    ): List<LocationData> {
        val points = mutableListOf<LocationData>()
        val numPoints = 10

        for (i in 0..numPoints) {
            val ratio = i.toDouble() / numPoints
            val lat = start.latitude + (end.latitude - start.latitude) * ratio
            val lng = start.longitude + (end.longitude - start.longitude) * ratio
            points.add(LocationData(lat, lng))
        }

        return points
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

    private fun isEmulatorLocation(location: LocationData): Boolean =
        location.latitude in 37.0..38.0 && location.longitude in -123.0..-121.0
}
