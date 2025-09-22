package net.ritirp.myapplication.ui.screens

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import kotlinx.coroutines.launch
import net.ritirp.myapplication.services.LocationService

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    onNavigationClick: (LatLng, LatLng) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var destinationLocation by remember { mutableStateOf<LatLng?>(null) }
    var showNavigationButton by remember { mutableStateOf(false) }
    val locationService = remember { LocationService(context) }

    // 위치 권한 요청
    val locationPermissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
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
                        start(object : KakaoMapReadyCallback() {
                            override fun onMapReady(map: KakaoMap) {
                                kakaoMap = map

                                // 서울 시청을 기본 위치로 설정
                                val seoulCityHall = LatLng.from(37.5666805, 126.9784147)
                                map.moveCamera(
                                    CameraUpdateFactory.newCenterPosition(seoulCityHall, 15)
                                )

                                // 지도 클릭 이벤트 처리
                                map.setOnMapClickListener { _, latLng, _, _ ->
                                    destinationLocation = latLng
                                    showNavigationButton = currentLocation != null
                                }
                            }
                        })
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // UI 컨트롤
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 현재 위치 버튼
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            val location = locationService.getCurrentLocation()
                            location?.let {
                                currentLocation = LatLng.from(it.latitude, it.longitude)
                                kakaoMap?.moveCamera(
                                    CameraUpdateFactory.newCenterPosition(
                                        LatLng.from(it.latitude, it.longitude),
                                        15
                                    )
                                )
                                showNavigationButton = destinationLocation != null
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "현재 위치")
                }

                // 네비게이션 버튼
                if (showNavigationButton) {
                    FloatingActionButton(
                        onClick = {
                            currentLocation?.let { current ->
                                destinationLocation?.let { destination ->
                                    onNavigationClick(current, destination)
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.DirectionsRun, contentDescription = "길찾기")
                    }
                }
            }
        } else {
            // 권한이 필요하다는 메시지
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "지도를 사용하려면 위치 권한이 필요합니다.",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { locationPermissions.launchMultiplePermissionRequest() }
                    ) {
                        Text("권한 허용")
                    }
                }
            }
        }
    }
}
