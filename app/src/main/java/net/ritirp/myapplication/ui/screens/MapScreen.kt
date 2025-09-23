package net.ritirp.myapplication.ui.screens

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory
import kotlinx.coroutines.launch
import net.ritirp.myapplication.services.LocationService

// MapScreen 컨트롤러 인터페이스
interface MapScreenController {
    fun moveToCurrentLocation()
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    onNavigationClick: (LatLng, LatLng) -> Unit = { _, _ -> },
    showFloatingButtons: Boolean = true,
    onMapControllerReady: ((MapScreenController) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var destinationLocation by remember { mutableStateOf<LatLng?>(null) }
    var showNavigationButton by remember { mutableStateOf(false) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    val locationService = remember { LocationService(context) }

    // 현재 위치로 이동하는 함수
    val moveToCurrentLocationFunc = {
        coroutineScope.launch {
            isLoadingLocation = true
            val location = locationService.getCurrentLocation()
            location?.let {
                val currentPos = LatLng.from(it.latitude, it.longitude)
                currentLocation = currentPos

                // 현재 위치로 카메라 이동
                kakaoMap?.moveCamera(
                    CameraUpdateFactory.newCenterPosition(currentPos, 15)
                )

                showNavigationButton = destinationLocation != null
                println("현재 위치 갱신: ${it.latitude}, ${it.longitude}")
            }
            isLoadingLocation = false
        }
    }

    // 컨트롤러 생성 및 전달
    val controller = remember {
        object : MapScreenController {
            override fun moveToCurrentLocation() {
                moveToCurrentLocationFunc()
            }
        }
    }

    // 컨트롤러를 MainScreen에 전달
    LaunchedEffect(controller) {
        onMapControllerReady?.invoke(controller)
    }

    // 위치 권한 요청
    val locationPermissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ),
    )

    LaunchedEffect(Unit) {
        locationPermissions.launchMultiplePermissionRequest()
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (locationPermissions.allPermissionsGranted) {
            // 카카오맵 표시
            AndroidView(
                factory = { context ->
                    MapView(context).apply {
                        // 하드웨어 가속 활성화
                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                        start(
                            object : MapLifeCycleCallback() {
                                override fun onMapDestroy() {
                                    // 지도 리소스 정리
                                    println("지도가 파괴됨")
                                }

                                override fun onMapError(exception: Exception?) {
                                    println("카카오맵 오류: ${exception?.message}")
                                }

                                override fun onMapResumed() {
                                    println("지도 재시작됨")
                                }

                                override fun onMapPaused() {
                                    println("지도 일시정지됨")
                                }
                            },
                            object : KakaoMapReadyCallback() {
                                override fun onMapReady(map: KakaoMap) {
                                    kakaoMap = map
                                    println("지도 준비 완료")

                                    // 지도 설정 최적화
                                    try {
                                        // 서울 시청을 기본 위치로 설정
                                        val seoulCityHall = LatLng.from(37.5666805, 126.9784147)
                                        map.moveCamera(
                                            CameraUpdateFactory.newCenterPosition(seoulCityHall, 15),
                                        )

                                        // 지도 클릭 이벤트 처리 - 목적지 설정
                                        map.setOnMapClickListener { _, latLng, _, _ ->
                                            destinationLocation = latLng
                                            showNavigationButton = currentLocation != null
                                            println("목적지 설정: ${latLng.latitude}, ${latLng.longitude}")
                                        }

                                        // 자동으로 현재 위치 가져오기는 제거하고, 버튼 클릭시에만 실행
                                        println("지도 초기화 완료 - 📍 버튼을 눌러 현재 위치를 확인하세요")
                                    } catch (e: Exception) {
                                        println("지도 설정 중 오류 발생: ${e.message}")
                                    }
                                }

                                override fun getPosition(): LatLng {
                                    return LatLng.from(37.5666805, 126.9784147)
                                }
                            },
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { mapView ->
                    // MapView 업데이트시 하드웨어 가속 유지
                    mapView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                }
            )

            // UI 컨트롤
            if (showFloatingButtons) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)  // 실제 앱과 동일하게 오른쪽 중앙으로
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // 현재 위치 버튼
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                isLoadingLocation = true
                                val location = locationService.getCurrentLocation()
                                location?.let {
                                    val currentPos = LatLng.from(it.latitude, it.longitude)
                                    currentLocation = currentPos

                                    // 현재 위치로 카메라 이동
                                    kakaoMap?.moveCamera(
                                        CameraUpdateFactory.newCenterPosition(currentPos, 15)
                                    )

                                    showNavigationButton = destinationLocation != null
                                    println("현재 위치 갱신: ${it.latitude}, ${it.longitude}")
                                }
                                isLoadingLocation = false
                            }
                        },
                    ) {
                        if (isLoadingLocation) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.LocationOn, contentDescription = "현재 위치")
                        }
                    }

                    // 네비게이션 버튼
                    if (showNavigationButton) {
                        FloatingActionButton(
                            onClick = {
                                currentLocation?.let { current ->
                                    destinationLocation?.let { destination ->
                                        println("길찾기 시작: ${current.latitude}, ${current.longitude} -> ${destination.latitude}, ${destination.longitude}")
                                        onNavigationClick(current, destination)
                                    }
                                }
                            },
                        ) {
                            Icon(Icons.Default.Place, contentDescription = "길찾기")
                        }
                    }
                }
            }

            // 위치 정보 표시 카드
            currentLocation?.let { location ->
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = "현재 위치: ${location.latitude.toString().take(8)}, ${location.longitude.toString().take(9)}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

        } else {
            // 권한 요청 UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "위치 권한이 필요합니다",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(
                    onClick = {
                        locationPermissions.launchMultiplePermissionRequest()
                    }
                ) {
                    Text("권한 허용")
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Map Screen - Default")
@Composable
fun MapScreenPreview() {
    // Preview에서는 실제 지도 대신 플레이스홀더 표시
    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 지도 영역 플레이스홀더
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Map",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "카카오맵 영역",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "실제 앱에서는 지도가 표시됩니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 플로팅 버튼들
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)  // 실제 앱과 동일하게 오른쪽 중앙으로
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(onClick = {}) {
                    Icon(Icons.Default.LocationOn, contentDescription = "현재 위치")
                }
                FloatingActionButton(onClick = {}) {
                    Icon(Icons.Default.Place, contentDescription = "길찾기")
                }
            }

            // 위치 정보 카드
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "현재 위치: 37.56668, 126.97841",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Map Screen - Loading State")
@Composable
fun MapScreenLoadingPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 지도 영역 플레이스홀더
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Map",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "카카오맵 영역",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 로딩 상태의 플로팅 버튼
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(onClick = {}) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Map Screen - Permission Denied")
@Composable
fun MapScreenPermissionDeniedPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "위치 권한이 필요합니다",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(onClick = {}) {
                Text("권한 허용")
            }
        }
    }
}

@Preview(showBackground = true, name = "Map Screen - Dark Theme", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MapScreenDarkPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 지도 영역 플레이스홀더 (다크 테마)
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Map",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "카카오맵 영역 (다크 모드)",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 플로팅 버튼들
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(onClick = {}) {
                    Icon(Icons.Default.LocationOn, contentDescription = "현재 위치")
                }
                FloatingActionButton(onClick = {}) {
                    Icon(Icons.Default.Place, contentDescription = "길찾기")
                }
            }

            // 위치 정보 카드
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "현재 위치: 37.56668, 126.97841",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Map Screen - Tablet", device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun MapScreenTabletPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 지도 영역 플레이스홀더
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Map",
                        modifier = Modifier.size(96.dp), // 태블릿용 큰 아이콘
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "카카오맵 영역 (태블릿)",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 플로팅 버튼들 (태블릿용 큰 사이즈)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingActionButton(
                    onClick = {},
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "현재 위치",
                        modifier = Modifier.size(32.dp)
                    )
                }
                FloatingActionButton(
                    onClick = {},
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = "길찾기",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
