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
                    viewModel = viewModel, // ViewModel 전달
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
    viewModel: MapViewModel,
    onMapClick: (LocationData) -> Unit,
    onFollowToggle: () -> Unit,
    onCurrentLocationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MapScreenContent(
        uiState = uiState,
        viewModel = viewModel,
        onMapClick = onMapClick,
        onFollowToggle = onFollowToggle,
        onCurrentLocationClick = onCurrentLocationClick,
        isPreview = false, // 실제 앱에서는 false
        modifier = modifier
    )
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
        13, 0.0, 0.0, 0.0  // 줌 레벨을 13으로 조정
    )
    map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    map.moveCamera(CameraUpdateFactory.zoomTo(13))  // 줌 레벨을 13으로 변경
    println("DEBUG: Map setup completed with zoom level 13 at ${defaultLocation.latitude}, ${defaultLocation.longitude}")
}

// 프리뷰용 컴포넌트들
@Preview(showBackground = true, name = "지도 앱 프리뷰")
@Composable
fun MapAppPreview() {
    // 프리뷰용 가짜 UI 상태 생성
    val previewUiState = net.ritirp.myapplication.presentation.viewmodel.MapUiState(
        currentLocation = LocationData.DEFAULT_SEOUL,
        destination = LocationData(37.5700, 126.9800), // 예시 목적지
        isFollowingLocation = false,
        currentTab = BottomTab.MAP,
        markers = listOf(
            net.ritirp.myapplication.data.model.MarkerData(
                id = "team_1",
                location = LocationData(37.5700, 126.9800),
                title = "팀원 1",
                emoji = "👤",
                type = net.ritirp.myapplication.data.model.MarkerType.TEAM_MEMBER
            )
        )
    )

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentTab = previewUiState.currentTab,
                onTabSelected = { }
            )
        }
    ) { paddingValues ->
        // 실제 MapScreen 컴포넌트를 호출하되, 지도만 프리뷰용으로 대체
        MapScreenContent(
            uiState = previewUiState,
            onMapClick = { },
            onFollowToggle = { },
            onCurrentLocationClick = { },
            isPreview = true, // 프리뷰 모드 플래그
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun MapScreenContent(
    uiState: net.ritirp.myapplication.presentation.viewmodel.MapUiState,
    viewModel: MapViewModel? = null, // ViewModel 매개변수 추가
    onMapClick: (LocationData) -> Unit,
    onFollowToggle: () -> Unit,
    onCurrentLocationClick: () -> Unit,
    isPreview: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (isPreview) {
            // 프리뷰용 지도 영역
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("지도 영역", fontSize = 18.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("📍 현재위치", fontSize = 14.sp, color = Color.Red)
                    if (uiState.destination != null) {
                        Text("🚩 목적지", fontSize = 14.sp, color = Color.Blue)
                    }
                    Text("👤 팀원 ${uiState.markers.size}명", fontSize = 14.sp, color = Color.Green)
                }
            }
        } else {
            // 실제 카카오 지도
            MapContent(
                uiState = uiState,
                viewModel = viewModel,
                onMapClick = onMapClick
            )
        }

        // 공통 UI 오버레이들
        TopSearchBar(
            onFriendClick = { /* TODO: 친구 기능 */ }
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
private fun MapContent(
    uiState: net.ritirp.myapplication.presentation.viewmodel.MapUiState,
    viewModel: MapViewModel? = null, // ViewModel 매개변수 추가
    onMapClick: (LocationData) -> Unit
) {
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var isMapReady by remember { mutableStateOf(false) }

    // 카메라 이동 이벤트 감지
    viewModel?.let { vm ->
        val cameraUpdateEvent by vm.cameraUpdateEvent.collectAsStateWithLifecycle()

        LaunchedEffect(cameraUpdateEvent) {
            cameraUpdateEvent?.let { location ->
                if (kakaoMap != null && isMapReady) {
                    println("DEBUG: Moving camera to current location: ${location.latitude}, ${location.longitude}")
                    MapUtils.moveCameraToLocation(kakaoMap, location, 13)  // 줌 레벨을 13으로 변경
                    // 이벤트 처리 후 초기화 (무한 루프 방지)
                    vm.clearCameraUpdateEvent()
                }
            }
        }
    }

    // 지도 상태 변화 감지 및 업데이트
    LaunchedEffect(kakaoMap, isMapReady, uiState.currentLocation) {
        if (kakaoMap != null && isMapReady) {
            kakaoMap?.let { map ->
                MapUtils.addOrUpdateCurrentLocationMarker(map, uiState.currentLocation)
                MapUtils.addTeamMarkers(map, uiState.markers)
            }
        }
    }

    LaunchedEffect(uiState.destination, isMapReady) {
        if (isMapReady) {
            kakaoMap?.let { map ->
                uiState.destination?.let { dest ->
                    println("DEBUG: UI State destination changed, calling MapUtils.addDestinationMarker")
                    MapUtils.addDestinationMarker(map, dest)
                }
            }
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
}
