package net.ritirp.myapplication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ritirp.myapplication.data.api.Poi
import net.ritirp.myapplication.data.model.LocationData
import net.ritirp.myapplication.data.model.MarkerData
import net.ritirp.myapplication.data.model.RidingMetrics
import net.ritirp.myapplication.data.model.RouteData
import net.ritirp.myapplication.data.repository.MapRepository
import net.ritirp.myapplication.service.RidingMetricsTracker

/**
 * 지도 화면의 UI 상태
 */
data class MapUiState(
    val currentLocation: LocationData = LocationData.DEFAULT_SEOUL,
    val destination: LocationData? = null,
    val departure: LocationData? = null, // 출발지 추가
    val route: RouteData? = null,
    val routePoints: List<com.kakao.vectormap.LatLng> = emptyList(), // 서버에서 받은 경로 좌표들
    val markers: List<MarkerData> = emptyList(),
    val isFollowingLocation: Boolean = false,
    val isLocationPermissionGranted: Boolean = false,
    val currentTab: BottomTab = BottomTab.MAP,
    val isLoading: Boolean = false,
    val searchResults: List<Poi> = emptyList(),
    val isSearching: Boolean = false,
)

enum class BottomTab(
    val label: String,
) {
    MAP("지도"),
    REPORT("주행 리포트"),
    BUDDY("버디"),
    MY("MY"),
}

/**
 * 지도 ViewModel
 */
class MapViewModel(
    private val mapRepository: MapRepository,
    private val ridingMetricsTracker: RidingMetricsTracker? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    // 카메라 이동 이벤트를 위한 StateFlow 추가
    private val _cameraUpdateEvent = MutableStateFlow<LocationData?>(null)
    val cameraUpdateEvent: StateFlow<LocationData?> = _cameraUpdateEvent.asStateFlow()

    // 주행 통계
    private val _ridingMetrics = MutableStateFlow(RidingMetrics())
    val ridingMetrics: StateFlow<RidingMetrics> = _ridingMetrics.asStateFlow()

    init {
        observeRepositoryData()
        initializeData()
    }

    private fun observeRepositoryData() {
        viewModelScope.launch {
            mapRepository.destination.collect { destination ->
                println("DEBUG: ViewModel received destination change: ${destination?.let { "${it.latitude}, ${it.longitude}" } ?: "null"}")
                _uiState.value = _uiState.value.copy(destination = destination)
            }
        }

        viewModelScope.launch {
            mapRepository.currentLocation.collect { currentLocation ->
                _uiState.value = _uiState.value.copy(currentLocation = currentLocation)
            }
        }

        viewModelScope.launch {
            mapRepository.route.collect { route ->
                _uiState.value = _uiState.value.copy(route = route)
            }
        }

        viewModelScope.launch {
            mapRepository.markers.collect { markers ->
                _uiState.value = _uiState.value.copy(markers = markers)
            }
        }

        // 주행 통계 관찰
        ridingMetricsTracker?.let { tracker ->
            viewModelScope.launch {
                tracker.metrics.collect { metrics ->
                    _ridingMetrics.value = metrics
                }
            }
        }
    }

    private fun initializeData() {
        mapRepository.initializeTeamMarkers()
    }

    fun onLocationPermissionGranted() {
        _uiState.value = _uiState.value.copy(isLocationPermissionGranted = true)
        getCurrentLocation()
    }

    fun getCurrentLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val newLocation = mapRepository.getCurrentLocation()
                // 현재 위치를 가져온 후 카메라를 해당 위치로 이동시키기 위한 이벤트 발생
                _cameraUpdateEvent.value = newLocation
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun clearCameraUpdateEvent() {
        _cameraUpdateEvent.value = null
    }

    fun onMapClicked(location: LocationData) {
        println("DEBUG: ViewModel received map click: ${location.latitude}, ${location.longitude}")

        // 목적지가 설정되면 서버에서 경로 조회
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val currentLocation = _uiState.value.currentLocation
                val routePoints = mapRepository.getRoute(currentLocation, location)
                _uiState.value = _uiState.value.copy(routePoints = routePoints)
                println("DEBUG: Updated routePoints in UI state: ${routePoints.size} points")
            } catch (e: Exception) {
                println("DEBUG: Error getting route: ${e.message}")
                e.printStackTrace()
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
        mapRepository.setDestination(location)
    }

    fun toggleFollowLocation() {
        _uiState.value =
            _uiState.value.copy(
                isFollowingLocation = !_uiState.value.isFollowingLocation,
            )
    }

    fun selectTab(tab: BottomTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    /**
     * 기울기 캘리브레이션
     */
    fun calibrateLeanAngle() {
        ridingMetricsTracker?.calibrateLeanAngle()
    }

    /**
     * 장소 검색
     */
    fun searchPlace(keyword: String) {
        if (keyword.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            try {
                val currentLocation = _uiState.value.currentLocation
                val results = mapRepository.searchPlace(
                    keyword = keyword,
                    centerLat = currentLocation.latitude,
                    centerLon = currentLocation.longitude
                )
                _uiState.value = _uiState.value.copy(searchResults = results)
            } catch (e: Exception) {
                println("DEBUG: Error searching place: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(searchResults = emptyList())
            } finally {
                _uiState.value = _uiState.value.copy(isSearching = false)
            }
        }
    }

    /**
     * 검색 결과 초기화
     */
    fun clearSearchResults() {
        _uiState.value = _uiState.value.copy(searchResults = emptyList())
    }

    /**
     * 출발지와 도착지를 설정하고 경로를 검색
     */
    fun setRoute(departure: LocationData, destination: LocationData) {
        println("DEBUG: ViewModel setRoute - departure: ${departure.latitude}, ${departure.longitude}, destination: ${destination.latitude}, ${destination.longitude}")

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                departure = departure,
                destination = destination
            )
            try {
                val routePoints = mapRepository.getRoute(departure, destination)
                _uiState.value = _uiState.value.copy(routePoints = routePoints)
                println("DEBUG: Updated routePoints in UI state: ${routePoints.size} points")

                // 목적지를 Repository에도 설정
                mapRepository.setDestination(destination)
            } catch (e: Exception) {
                println("DEBUG: Error getting route: ${e.message}")
                e.printStackTrace()
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}

/**
 * ViewModel Factory
 */
class MapViewModelFactory(
    private val mapRepository: MapRepository,
    private val ridingMetricsTracker: RidingMetricsTracker? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            return MapViewModel(mapRepository, ridingMetricsTracker) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
