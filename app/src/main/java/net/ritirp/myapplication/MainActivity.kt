package net.ritirp.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraPosition
import com.kakao.vectormap.camera.CameraUpdateFactory
import net.ritirp.myapplication.data.model.LocationData
import net.ritirp.myapplication.data.repository.MapRepository
import net.ritirp.myapplication.presentation.components.*
import net.ritirp.myapplication.presentation.utils.MapUtils
import net.ritirp.myapplication.presentation.viewmodel.BottomTab
import net.ritirp.myapplication.presentation.viewmodel.MapViewModel
import net.ritirp.myapplication.presentation.viewmodel.MapViewModelFactory

/**
 * MVVM 패턴을 적용한 메인 액티비티
 */
class MainActivity : ComponentActivity() {

    private val mapRepository by lazy {
        MapRepository(LocationServices.getFusedLocationProviderClient(this))
    }

    private val viewModel: MapViewModel by viewModels {
        MapViewModelFactory(mapRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MapApp(viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapApp(viewModel: MapViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    // 권한 요청
    LaunchedEffect(Unit) {
        if (!locationPermission.status.isGranted) {
            locationPermission.launchPermissionRequest()
        } else {
            viewModel.onLocationPermissionGranted()
        }
    }

    // 권한이 승인되면 ViewModel에 알림
    LaunchedEffect(locationPermission.status.isGranted) {
        if (locationPermission.status.isGranted && !uiState.isLocationPermissionGranted) {
            viewModel.onLocationPermissionGranted()
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentTab = uiState.currentTab,
                onTabSelected = viewModel::selectTab
            )
        }
    ) { paddingValues ->
        when (uiState.currentTab) {
            BottomTab.MAP -> {
                MapScreen(
                    uiState = uiState,
                    onMapClick = viewModel::onMapClicked,
                    onFollowToggle = viewModel::toggleFollowLocation,
                    onCurrentLocationClick = viewModel::getCurrentLocation,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                PlaceholderScreen(
                    tabName = uiState.currentTab.label,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
fun MapScreen(
    uiState: net.ritirp.myapplication.presentation.viewmodel.MapUiState,
    onMapClick: (LocationData) -> Unit,
    onFollowToggle: () -> Unit,
    onCurrentLocationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var isMapReady by remember { mutableStateOf(false) }

    // 지도가 준비되고 나서 초기 마커들 표시
    LaunchedEffect(kakaoMap, isMapReady) {
        if (kakaoMap != null && isMapReady) {
            kakaoMap?.let { map ->
                // 현재 위치 마커 추가
                MapUtils.addOrUpdateCurrentLocationMarker(map, uiState.currentLocation)
                // 팀 마커들 추가
                MapUtils.addTeamMarkers(map, uiState.markers)
            }
        }
    }

    // 지도 상태 변화 감지 및 업데이트 (지도가 준비된 후에만)
    LaunchedEffect(uiState.currentLocation, isMapReady) {
        if (isMapReady) {
            kakaoMap?.let { map ->
                MapUtils.addOrUpdateCurrentLocationMarker(map, uiState.currentLocation)
            }
        }
    }

    LaunchedEffect(uiState.destination, isMapReady) {
        if (isMapReady) {
            kakaoMap?.let { map ->
                uiState.destination?.let { dest ->
                    println("DEBUG: UI State destination changed, calling MapUtils.addDestinationMarker")
                    MapUtils.addDestinationMarker(map, dest)
                } ?: run {
                    println("DEBUG: UI State destination is null, no marker to add")
                }
            }
        } else {
            println("DEBUG: Map not ready yet, cannot add destination marker")
        }
    }

    LaunchedEffect(uiState.route, isMapReady) {
        if (isMapReady) {
            kakaoMap?.let { map ->
                uiState.route?.let { route ->
                    MapUtils.drawRoute(map, route)
                }
            }
        }
    }

    LaunchedEffect(uiState.markers, isMapReady) {
        if (isMapReady) {
            kakaoMap?.let { map ->
                MapUtils.addTeamMarkers(map, uiState.markers)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 카카오 지도
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                MapView(context).apply {
                    start(
                        object : MapLifeCycleCallback() {
                            override fun onMapDestroy() {
                                isMapReady = false
                            }
                            override fun onMapError(e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        object : KakaoMapReadyCallback() {
                            override fun onMapReady(map: KakaoMap) {
                                kakaoMap = map
                                setupMap(map, uiState.currentLocation)

                                // 지도 클릭 리스너
                                map.setOnMapClickListener { _, position, _, _ ->
                                    println("DEBUG: Map clicked at ${position.latitude}, ${position.longitude}")
                                    onMapClick(LocationData.fromLatLng(position))
                                }

                                // 지도 준비 완료 표시
                                isMapReady = true
                                println("DEBUG: Map is ready, setting isMapReady = true")
                            }
                        }
                    )
                }
            }
        )

        // UI 오버레이들
        TopSearchBar(
            onFriendClick = { /* TODO: 친구 기능 */ }
        )

        FollowToggleButton(
            isFollowing = uiState.isFollowingLocation,
            onToggle = onFollowToggle,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 16.dp)
        )

        CurrentLocationButton(
            isFollowing = uiState.isFollowingLocation,
            onClick = onCurrentLocationClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 120.dp, end = 20.dp)
        )

        // 로딩 상태
        if (uiState.isLoading) {
            LoadingIndicator()
        }
    }
}

@Composable
fun PlaceholderScreen(
    tabName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("$tabName 준비중", fontSize = 18.sp)
    }
}

private fun setupMap(map: KakaoMap, defaultLocation: LocationData) {
    val cameraPosition = CameraPosition.from(
        defaultLocation.latitude,
        defaultLocation.longitude,
        10, 0.0, 0.0, 0.0  // 줌 레벨을 10으로 조정 (더 넓은 범위)
    )
    map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    map.moveCamera(CameraUpdateFactory.zoomTo(15))
    println("DEBUG: Map setup completed with zoom level 10 at ${defaultLocation.latitude}, ${defaultLocation.longitude}")
}

// 프리뷰용 컴포넌트들
@Preview(showBackground = true, name = "지도 앱 프리뷰")
@Composable
fun MapAppPreview() {
    MapAppPreviewContent()
}

@Composable
private fun MapAppPreviewContent() {
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentTab = BottomTab.MAP,
                onTabSelected = { }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            // 지도 영역 (프리뷰용 회색 배경)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Text("지도 영역", fontSize = 18.sp, color = Color.Gray)
            }

            // UI 오버레이들
            TopSearchBar()

            FollowToggleButton(
                isFollowing = false,
                onToggle = { },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 16.dp)
            )

            CurrentLocationButton(
                isFollowing = false,
                onClick = { },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 120.dp, end = 20.dp)
            )
        }
    }
}
