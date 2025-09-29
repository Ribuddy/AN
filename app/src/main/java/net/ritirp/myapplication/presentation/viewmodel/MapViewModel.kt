package net.ritirp.myapplication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.ritirp.myapplication.data.model.LocationData
import net.ritirp.myapplication.data.model.MarkerData
import net.ritirp.myapplication.data.model.RouteData
import net.ritirp.myapplication.data.repository.MapRepository

/**
 * 지도 화면의 UI 상태
 */
data class MapUiState(
    val currentLocation: LocationData = LocationData.DEFAULT_SEOUL,
    val destination: LocationData? = null,
    val route: RouteData? = null,
    val markers: List<MarkerData> = emptyList(),
    val isFollowingLocation: Boolean = false,
    val isLocationPermissionGranted: Boolean = false,
    val currentTab: BottomTab = BottomTab.MAP,
    val isLoading: Boolean = false
)

enum class BottomTab(val label: String) {
    MAP("지도"),
    REPORT("주행 리포트"),
    FRIEND("친구"),
    MY("MY")
}

/**
 * 지도 ViewModel
 */
class MapViewModel(
    private val mapRepository: MapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

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
                mapRepository.getCurrentLocation()
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun onMapClicked(location: LocationData) {
        println("DEBUG: ViewModel received map click: ${location.latitude}, ${location.longitude}")
        mapRepository.setDestination(location)
    }

    fun toggleFollowLocation() {
        _uiState.value = _uiState.value.copy(
            isFollowingLocation = !_uiState.value.isFollowingLocation
        )
    }

    fun selectTab(tab: BottomTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }
}

/**
 * ViewModel Factory
 */
class MapViewModelFactory(
    private val mapRepository: MapRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            return MapViewModel(mapRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
