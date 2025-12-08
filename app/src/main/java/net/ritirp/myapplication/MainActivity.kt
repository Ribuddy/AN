package net.ritirp.myapplication

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraPosition
import com.kakao.vectormap.camera.CameraUpdateFactory
import net.ritirp.myapplication.data.model.AuthState
import net.ritirp.myapplication.data.model.CrashEvent
import net.ritirp.myapplication.data.model.LocationData
import net.ritirp.myapplication.data.model.RidingMetrics
import net.ritirp.myapplication.data.repository.MapRepository
import net.ritirp.myapplication.presentation.components.*
import net.ritirp.myapplication.presentation.screen.CrashAlertScreen
import net.ritirp.myapplication.presentation.screen.LoginScreen
import net.ritirp.myapplication.presentation.screen.SplashScreen
import net.ritirp.myapplication.presentation.utils.MapUtils
import net.ritirp.myapplication.presentation.viewmodel.BottomTab
import net.ritirp.myapplication.presentation.viewmodel.LoginViewModel
import net.ritirp.myapplication.presentation.viewmodel.MapViewModel
import net.ritirp.myapplication.presentation.viewmodel.MapViewModelFactory

/**
 * MVVM 패턴을 적용한 메인 액티비티
 */
class MainActivity : ComponentActivity() {
    private val mapRepository: MapRepository by lazy {
        // GlobalApplication의 MapRepository 사용 (싱글톤)
        GlobalApplication.getMapRepository(this)
    }

    private val ridingMetricsTracker: net.ritirp.myapplication.service.RidingMetricsTracker by lazy {
        GlobalApplication.getRidingMetricsTracker(this)
    }

    private val mapViewModel: MapViewModel by viewModels {
        MapViewModelFactory(mapRepository, ridingMetricsTracker)
    }

    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppNavigation(
                mapViewModel = mapViewModel,
                loginViewModel = loginViewModel,
                mapRepository = mapRepository,
            )
        }
    }
}

@Composable
fun AppNavigation(
    mapViewModel: MapViewModel,
    loginViewModel: LoginViewModel,
    mapRepository: MapRepository,
) {
    val navController = rememberNavController()
    val authState by loginViewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 사고 감지 이벤트 수신
    var currentCrashEvent by remember { mutableStateOf<CrashEvent?>(null) }

    LaunchedEffect(Unit) {
        val crashDetector = GlobalApplication.getCrashDetector(context)

        // CrashDetector에 위치 제공자 설정
        crashDetector.setLocationProvider {
            // MapViewModel의 현재 위치 사용
            val currentLocation = mapViewModel.uiState.value.currentLocation
            Triple(
                currentLocation.latitude,
                currentLocation.longitude,
                null // 고도 정보는 현재 없음
            )
        }

        crashDetector.crashEvents.collect { event ->
            Log.e("AppNavigation", "🚨 Crash event received, navigating to crash screen")
            Log.d("AppNavigation", "사고 위치: lat=${event.lat}, lon=${event.lon}, ele=${event.ele}, angle=${event.leanAngle}")
            currentCrashEvent = event
            navController.navigate("crash") {
                // 백스택에 추가하되, 중복 방지
                launchSingleTop = true
            }
        }
    }

    // 로그인 상태에 따른 자동 네비게이션
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                navController.navigate("main") {
                    popUpTo(0) { inclusive = true }
                }
            }
            else -> {}
        }
    }

    NavHost(
        navController = navController,
        startDestination = "splash",
    ) {
        // 스플래시 화면
        composable("splash") {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
            )
        }

        // 로그인 화면
        composable("login") {
            LoginScreen(
                onGoogleLoginSuccess = { idToken, userName, userEmail ->
                    loginViewModel.handleOAuthCallback(idToken, userName, userEmail)
                },
                authState = authState,
                onLoginError = { errorMessage ->
                    loginViewModel.setError(errorMessage)
                },
            )
        }

        // 메인 화면 (지도)
        composable("main") {
            MapApp(
                viewModel = mapViewModel,
                onNavigateToCrashSettings = {
                    navController.navigate("crash_settings")
                },
                onNavigateToTeamManagement = {
                    navController.navigate("team_management")
                },
                onLogout = {
                    Log.d("AppNavigation", "로그아웃 - 로그인 화면으로 이동")
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true } // 모든 백스택 제거
                    }
                },
            )
        }

        // 사고 감지 경고 화면
        composable("crash") {
            currentCrashEvent?.let { event ->
                val drivingRepository = GlobalApplication.getDrivingRepository(context)
                val coroutineScope = rememberCoroutineScope()

                CrashAlertScreen(
                    crashEvent = event,
                    onConfirm = {
                        // TODO: 긴급 연락 전송 로직
                        Log.d("AppNavigation", "Emergency contact sent")
                        navController.popBackStack()
                    },
                    onCancel = {
                        Log.d("AppNavigation", "User is OK, dismissing alert")
                        navController.popBackStack()
                    },
                    onReportAccident = { crashEvent ->
                        // 서버에 사고 보고
                        coroutineScope.launch {
                            val ridingRecordId = drivingRepository.currentRidingRecordId.value
                            if (ridingRecordId != null && crashEvent.lat != null && crashEvent.lon != null) {
                                Log.d("AppNavigation", "서버에 사고 보고 중...")

                                // timestamp를 ISO 8601 형식으로 변환
                                val isoTimestamp = java.text.SimpleDateFormat(
                                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                                    java.util.Locale.US
                                ).apply {
                                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                                }.format(java.util.Date(crashEvent.timestamp))

                                drivingRepository.reportAccident(
                                    ridingRecordId = ridingRecordId,
                                    lat = crashEvent.lat,
                                    lon = crashEvent.lon,
                                    ele = crashEvent.ele,
                                    gravityForce = crashEvent.impactMagnitude.toDouble(),
                                    leanAngle = crashEvent.leanAngle,
                                    timestamp = isoTimestamp
                                ).onSuccess {
                                    Log.d("AppNavigation", "사고 보고 성공")
                                }.onFailure { error ->
                                    Log.e("AppNavigation", "사고 보고 실패: ${error.message}")
                                }
                            } else {
                                Log.w("AppNavigation", "사고 보고 실패: ridingRecordId 또는 위치 정보 없음")
                            }
                        }
                    },
                )
            }
        }

        // 사고 감지 설정 화면
        composable("crash_settings") {
            val crashSettingsRepository = GlobalApplication.getCrashSettingsRepository(context)
            val crashSettingsViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel<net.ritirp.myapplication.presentation.viewmodel.CrashSettingsViewModel>(
                    factory = net.ritirp.myapplication.presentation.viewmodel.CrashSettingsViewModelFactory(crashSettingsRepository),
                )
            net.ritirp.myapplication.presentation.screen.CrashSettingsScreen(
                viewModel = crashSettingsViewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // 팀 관리 화면
        composable("team_management") {
            val teamRepository = GlobalApplication.getTeamRepository(context)
            val drivingRepository = GlobalApplication.getDrivingRepository(context)
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val ridingMetricsTracker = GlobalApplication.getRidingMetricsTracker(context)
            val ridingRecordRepository = GlobalApplication.getRidingRecordRepository(context)
            val teamViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel<net.ritirp.myapplication.presentation.viewmodel.TeamViewModel>(
                    factory =
                        net.ritirp.myapplication.presentation.viewmodel.TeamViewModelFactory(
                            teamRepository,
                            drivingRepository,
                            fusedLocationClient,
                            mapRepository,
                            ridingMetricsTracker,
                            ridingRecordRepository,
                        ),
                )
            net.ritirp.myapplication.presentation.screen.TeamManagementScreen(
                viewModel = teamViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMap = {
                    navController.navigate("main") {
                        popUpTo("main") { inclusive = false }
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapApp(
    viewModel: MapViewModel,
    onNavigateToCrashSettings: () -> Unit = {},
    onNavigateToTeamManagement: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val context = LocalContext.current // context 추가

    // TeamViewModel 가져오기
    val teamRepository = GlobalApplication.getTeamRepository(context)
    val drivingRepository = GlobalApplication.getDrivingRepository(context)
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val mapRepository = GlobalApplication.getMapRepository(context)
    val ridingMetricsTracker = GlobalApplication.getRidingMetricsTracker(context)
    val ridingRecordRepository = GlobalApplication.getRidingRecordRepository(context)
    val teamViewModel = androidx.lifecycle.viewmodel.compose.viewModel<net.ritirp.myapplication.presentation.viewmodel.TeamViewModel>(
        factory = net.ritirp.myapplication.presentation.viewmodel.TeamViewModelFactory(
            teamRepository,
            drivingRepository,
            fusedLocationClient,
            mapRepository,
            ridingMetricsTracker,
            ridingRecordRepository,
        ),
    )

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
                onTabSelected = viewModel::selectTab,
            )
        },
    ) { paddingValues ->
        when (uiState.currentTab) {
            BottomTab.MAP -> {
                MapScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    teamViewModel = teamViewModel,
                    onMapClick = viewModel::onMapClicked,
                    onFollowToggle = viewModel::toggleFollowLocation,
                    onCurrentLocationClick = viewModel::getCurrentLocation,
                    modifier = Modifier.padding(paddingValues),
                )
            }
            BottomTab.REPORT -> {
                val localRidingRecordRepository = GlobalApplication.getLocalRidingRecordRepository(context)
                val ridingStatisticsRepository = GlobalApplication.getRidingStatisticsRepository(context)
                val ridingReportViewModel =
                    androidx.lifecycle.viewmodel.compose.viewModel<net.ritirp.myapplication.presentation.viewmodel.RidingReportViewModel>(
                        factory = net.ritirp.myapplication.presentation.viewmodel.RidingReportViewModelFactory(
                            localRidingRecordRepository,
                            ridingStatisticsRepository
                        ),
                    )
                net.ritirp.myapplication.presentation.screen.RidingReportScreen(
                    viewModel = ridingReportViewModel,
                    modifier = Modifier.padding(paddingValues),
                )
            }
            BottomTab.BUDDY -> {
                val friendRepository = GlobalApplication.getFriendRepository(context)
                val authRepository = GlobalApplication.getAuthRepository(context)
                val friendViewModel =
                    androidx.lifecycle.viewmodel.compose.viewModel<net.ritirp.myapplication.presentation.viewmodel.FriendViewModel>(
                        factory = net.ritirp.myapplication.presentation.viewmodel.FriendViewModelFactory(friendRepository, authRepository),
                    )
                net.ritirp.myapplication.presentation.screen.FriendScreen(
                    viewModel = friendViewModel,
                    modifier = Modifier.padding(paddingValues),
                )
            }
            BottomTab.MY -> {
                net.ritirp.myapplication.presentation.screen.MyScreen(
                    onNavigateToCrashSettings = onNavigateToCrashSettings,
                    onNavigateToTeamManagement = onNavigateToTeamManagement,
                    onLogout = onLogout,
                    modifier = Modifier.padding(paddingValues),
                )
            }
            else -> {
                PlaceholderScreen(
                    tabName = uiState.currentTab.label,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

@Composable
fun MapScreen(
    uiState: net.ritirp.myapplication.presentation.viewmodel.MapUiState,
    viewModel: MapViewModel,
    teamViewModel: net.ritirp.myapplication.presentation.viewmodel.TeamViewModel,
    onMapClick: (LocationData) -> Unit,
    onFollowToggle: () -> Unit,
    onCurrentLocationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MapScreenContent(
        uiState = uiState,
        viewModel = viewModel,
        teamViewModel = teamViewModel,
        onMapClick = onMapClick,
        onFollowToggle = onFollowToggle,
        onCurrentLocationClick = onCurrentLocationClick,
        isPreview = false, // 실제 앱에서는 false
        modifier = modifier,
    )
}

@Composable
fun PlaceholderScreen(
    tabName: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text("$tabName 준비중", fontSize = 18.sp)
    }
}

// 프리뷰용 컴포넌트들
@Preview(showBackground = true, name = "지도 앱 프리뷰")
@Composable
fun MapAppPreview() {
    // 프리뷰용 가짜 UI 상태 생성
    val previewUiState =
        net.ritirp.myapplication.presentation.viewmodel.MapUiState(
            currentLocation = LocationData.DEFAULT_SEOUL,
            destination = LocationData(37.5700, 126.9800), // 예시 목적지
            isFollowingLocation = false,
            currentTab = BottomTab.MAP,
            markers =
                listOf(
                    net.ritirp.myapplication.data.model.MarkerData(
                        id = "team_1",
                        location = LocationData(37.5700, 126.9800),
                        title = "팀원 1",
                        emoji = "👤",
                        type = net.ritirp.myapplication.data.model.MarkerType.TEAM_MEMBER,
                    ),
                ),
        )

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentTab = previewUiState.currentTab,
                onTabSelected = { },
            )
        },
    ) { paddingValues ->
        // 실제 MapScreen 컴포넌트를 호출하되, 지도만 프리뷰용으로 대체
        MapScreenContent(
            uiState = previewUiState,
            onMapClick = { },
            onFollowToggle = { },
            onCurrentLocationClick = { },
            isPreview = true, // 프리뷰 모드 플래그
            modifier = Modifier.padding(paddingValues),
        )
    }
}

@Composable
private fun MapScreenContent(
    uiState: net.ritirp.myapplication.presentation.viewmodel.MapUiState,
    viewModel: MapViewModel? = null, // ViewModel 매개변수 추가
    teamViewModel: net.ritirp.myapplication.presentation.viewmodel.TeamViewModel? = null,
    onMapClick: (LocationData) -> Unit,
    onFollowToggle: () -> Unit,
    onCurrentLocationClick: () -> Unit,
    isPreview: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // 경로 입력 다이얼로그 상태
    var showRouteDialog by remember { mutableStateOf(false) }
    // 선택된 출발지와 도착지 위치 저장
    var selectedDeparture by remember { mutableStateOf<LocationData?>(null) }
    var selectedDestination by remember { mutableStateOf<LocationData?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        if (isPreview) {
            // 프리뷰용 지도 영역
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
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
                onMapClick = onMapClick,
            )
        }

        // 공통 UI 오버레이들
        TopSearchBar(
            onFriendClick = { /* TODO: 친구 기능 */ },
            onSearchBarClick = { showRouteDialog = true },
        )

        CurrentLocationButton(
            isFollowing = uiState.isFollowingLocation,
            onClick = onCurrentLocationClick,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 120.dp, end = 20.dp),
        )

        // 기울기 캘리브레이션 버튼 - 화면 상단 오른쪽
        Button(
            onClick = { viewModel?.calibrateLeanAngle() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 120.dp, end = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5C6BC0),
            ),
        ) {
            Text("📐 기울기 초기화", color = Color.White, fontSize = 12.sp)
        }

        // 팀 라이딩 중단 버튼 (라이딩 중일 때만 표시) - 화면 상단 오른쪽 아래
        if (teamViewModel != null) {
            val teamUiState by teamViewModel.uiState.collectAsStateWithLifecycle()

            if (teamUiState.ridingStatus == net.ritirp.myapplication.data.model.RidingStatus.RIDING) {
                Button(
                    onClick = { teamViewModel.endRiding() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 176.dp, end = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF5350),
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = null,
                        tint = Color.White,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("라이딩 중단", color = Color.White)
                }
            }
        }

        // 주행 통계 바 (라이딩 중일 때만 표시) - 화면 하단
        viewModel?.let { vm ->
            RidingMetricsOverlay(
                viewModel = vm,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
            )
        }

        // 로딩 상태
        if (uiState.isLoading) {
            LoadingIndicator()
        }
    }

    // 경로 입력 다이얼로그
    if (showRouteDialog) {
        RouteInputDialog(
            currentLocationName = "내 현재 위치",
            onDismiss = {
                showRouteDialog = false
                selectedDeparture = null
                selectedDestination = null
                viewModel?.clearSearchResults()
            },
            onConfirm = { departure: String, destination: String ->
                Log.d("MainActivity", "경로 설정: 출발지=$departure, 도착지=$destination")

                // 출발지가 "내 현재 위치"인 경우 현재 위치 사용, 아니면 선택된 출발지 사용
                val departureLocation = if (departure == "내 현재 위치" || selectedDeparture == null) {
                    uiState.currentLocation
                } else {
                    selectedDeparture!!
                }

                // 도착지는 선택된 위치 사용
                selectedDestination?.let { dest ->
                    Log.d("MainActivity", "경로 검색 시작: 출발지=(${departureLocation.latitude}, ${departureLocation.longitude}), 도착지=(${dest.latitude}, ${dest.longitude})")
                    viewModel?.setRoute(departureLocation, dest)
                }

                showRouteDialog = false
                selectedDeparture = null
                selectedDestination = null
                viewModel?.clearSearchResults()
            },
            searchResults = uiState.searchResults,
            onSearch = { keyword: String ->
                viewModel?.searchPlace(keyword)
            },
            onSelectSearchResult = { poi: net.ritirp.myapplication.data.api.Poi, isDeparture: Boolean ->
                val lat = poi.getLatitude()
                val lon = poi.getLongitude()
                if (lat != null && lon != null) {
                    val location = LocationData(lat, lon)
                    if (isDeparture) {
                        // 출발지 선택
                        selectedDeparture = location
                        Log.d("MainActivity", "출발지 선택됨: ${poi.name} (${lat}, ${lon})")
                    } else {
                        // 도착지 선택
                        selectedDestination = location
                        Log.d("MainActivity", "도착지 선택됨: ${poi.name} (${lat}, ${lon})")
                    }
                }
            },
            isSearching = uiState.isSearching,
        )
    }
}

@Composable
private fun RidingMetricsOverlay(
    viewModel: MapViewModel,
    modifier: Modifier = Modifier,
) {
    val ridingMetrics: RidingMetrics by viewModel.ridingMetrics.collectAsState()
    // 주행 중일 때만 표시
    if (ridingMetrics.isRiding) {
        RidingMetricsBar(
            metrics = ridingMetrics,
            modifier = modifier,
        )
    }
}

@Composable
private fun MapContent(
    uiState: net.ritirp.myapplication.presentation.viewmodel.MapUiState,
    viewModel: MapViewModel? = null, // ViewModel 매개변수 추가
    onMapClick: (LocationData) -> Unit,
) {
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var isMapReady by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // 카메라 이동 이벤트 감지
    viewModel?.let { vm ->
        val cameraUpdateEvent by vm.cameraUpdateEvent.collectAsStateWithLifecycle()

        LaunchedEffect(cameraUpdateEvent) {
            cameraUpdateEvent?.let { location ->
                if (kakaoMap != null && isMapReady) {
                    println("DEBUG: Moving camera to current location: ${location.latitude}, ${location.longitude}")
                    MapUtils.moveCameraToLocation(kakaoMap, location, 11) // 줌 레벨을 11로 변경
                    // 이벤트 처리 후 초기화 (무한 루프 방지)
                    vm.clearCameraUpdateEvent()
                }
            }
        }
    }

    // 내 위치 라벨 업데이트 (독립적)
    LaunchedEffect(uiState.currentLocation, isMapReady) {
        if (kakaoMap != null && isMapReady) {
            kakaoMap?.let { map ->
                MapUtils.addOrUpdateCurrentLocationMarker(map, uiState.currentLocation, context)
            }
        }
    }

    // 사고 정보 수집
    val drivingRepository = GlobalApplication.getDrivingRepository(context)
    val accidents = drivingRepository.accidents.collectAsState().value
    val accidentUserIds = remember(accidents) {
        accidents.map { it.userId }.toSet()
    }

    // 친구 라벨 업데이트 (독립적) - 사고 정보 포함
    LaunchedEffect(uiState.markers, accidentUserIds, isMapReady) {
        if (kakaoMap != null && isMapReady) {
            kakaoMap?.let { map ->
                MapUtils.addTeamMarkers(map, uiState.markers, context, accidentUserIds)
            }
        }
    }

    // 사고 알림 표시 (스낵바)
    LaunchedEffect(accidents) {
        if (accidents.isNotEmpty()) {
            accidents.forEach { accident ->
                val userName = accident.userName ?: "팀원"
                snackbarHostState.showSnackbar(
                    message = "⚠️ $userName 님이 사고를 당했습니다!",
                    duration = SnackbarDuration.Long
                )
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

    // 경로 라벨 업데이트 (독립적) - RouteLine 방식
    LaunchedEffect(uiState.routePoints, isMapReady) {
        if (isMapReady && uiState.routePoints.isNotEmpty()) {
            kakaoMap?.let { map ->
                println("DEBUG: Drawing route line with ${uiState.routePoints.size} points")
                MapUtils.drawRouteLine(map, uiState.routePoints)
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
                            isMapReady = true

                            // 초기 카메라 위치 설정 (줌 레벨 11로 명시적 설정)
                            val initialPosition = CameraPosition.from(
                                uiState.currentLocation.latitude,
                                uiState.currentLocation.longitude,
                                11, // 줌 레벨 11
                                0.0,
                                0.0,
                                0.0
                            )
                            map.moveCamera(CameraUpdateFactory.newCameraPosition(initialPosition))

                            // 지도 클릭 리스너 설정
                            map.setOnMapClickListener { _, latLng, _, _ ->
                                val clickedLocation = LocationData(latLng.latitude, latLng.longitude)
                                onMapClick(clickedLocation)
                            }

                            // 초기 마커 표시 (경로는 LaunchedEffect에서 처리)
                            MapUtils.addOrUpdateCurrentLocationMarker(map, uiState.currentLocation, context)
                            MapUtils.addTeamMarkers(map, uiState.markers, context)
                            uiState.destination?.let { dest ->
                                MapUtils.addDestinationMarker(map, dest)
                            }
                        }
                    },
                )
            }
        },
    )

    // 스낵바 호스트 - 화면 하단에 표시
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = Color(0xFFEF5350),
                contentColor = Color.White,
                shape = MaterialTheme.shapes.medium
            )
        }
    }
}
