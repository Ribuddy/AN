package net.ritirp.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraPosition
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelTextStyle
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.LatLng

/**
 * Kakao 지도 + 하단 탭 + 검색창/친구버튼/Follow 토글 + 현위치 마커.
 */
class MainActivity : ComponentActivity() {
    private lateinit var fusedClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        setContent { MapRootScreen(fusedClient) }
    }
}

private enum class BottomTab(val label: String) { MAP("지도"), REPORT("주행 리포트"), FRIEND("친구"), MY("MY") }

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun MapRootScreen(fusedClient: FusedLocationProviderClient) {
    var currentTab by rememberSaveable { mutableStateOf(BottomTab.MAP) }
    val finePermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(Unit) { if (!finePermission.status.isGranted) finePermission.launchPermissionRequest() }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                listOf(BottomTab.MAP to Icons.Default.Home,
                    BottomTab.REPORT to Icons.Default.Build,
                    BottomTab.FRIEND to Icons.Default.Group,
                    BottomTab.MY to Icons.Default.Person
                ).forEach { (tab, icon) ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { inner ->
        when (currentTab) {
            BottomTab.MAP -> MapScreen(
                modifier = Modifier.padding(inner),
                fusedClient = fusedClient,
                hasLocationPermission = finePermission.status.isGranted
            )
            else -> Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text("${currentTab.label} 준비중", fontSize = 18.sp)
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun MapScreen(
    modifier: Modifier = Modifier,
    fusedClient: FusedLocationProviderClient,
    hasLocationPermission: Boolean
) {
    val defaultLatLng = remember { LatLng.from(37.5666102, 126.9783881) } // 서울 기본
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var myLatLng by remember { mutableStateOf(defaultLatLng) }
    var destinationLatLng by remember { mutableStateOf<LatLng?>(null) } // 목적지 저장
    var followMyLocation by rememberSaveable { mutableStateOf(false) } // follow 기능 기본값을 false로 변경

    // 최초 1회 위치 가져오기
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) updateOnceLocation(fusedClient) { loc ->
            myLatLng = loc
            kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(loc))
            addOrUpdateMyMarker(kakaoMap, loc)
        }
    }

    // 목적지가 설정되면 경로 표시
    LaunchedEffect(destinationLatLng) {
        if (destinationLatLng != null && kakaoMap != null) {
            drawRoute(kakaoMap!!, myLatLng, destinationLatLng!!)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    start(object : MapLifeCycleCallback() {
                        override fun onMapDestroy() {}
                        override fun onMapError(e: Exception) { e.printStackTrace() }
                    }, object : KakaoMapReadyCallback() {
                        override fun onMapReady(map: KakaoMap) {
                            kakaoMap = map
                            // 매우 넓은 범위를 위해 줌 레벨을 15로 설정
                            val cameraPosition = CameraPosition.from(
                                defaultLatLng.latitude, defaultLatLng.longitude,
                                15, 0.0, 0.0, 0.0  // 15로 변경하여 매우 넓은 범위 (국가 레벨)
                            )
                            // 애니메이션 없이 즉시 이동
                            map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

                            // 추가로 줌 레벨을 확실하게 설정
                            map.moveCamera(CameraUpdateFactory.zoomTo(15))

                            // 지도 클릭 리스너 - 목적지 설정 (4개 파라미터로 수정)
                            map.setOnMapClickListener { _, position, _, _ ->
                                println("DEBUG: Map clicked at ${position.latitude}, ${position.longitude}")
                                addDestinationMarker(map, position)
                                destinationLatLng = position // 목적지 상태 업데이트
                            }

                            // 초기 마커들 추가
                            addOrUpdateMyMarker(map, myLatLng)
                            addTeamMembersMarkers(map) // 팀원들 마커 추가
                        }
                    })
                }
            }
        )

        // 상단 검색창 + 친구 버튼
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text("오늘은 어디를 달릴까요?", fontSize = 15.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                onClick = { /* TODO 친구 */ },
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 4.dp,
                color = Color(0xFF3E3E3E)
            ) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Text("👥", fontSize = 20.sp) }
            }
        }

        // Follow 토글 (우측 상단)
        Surface(
            onClick = { followMyLocation = !followMyLocation },
            shape = RoundedCornerShape(10.dp),
            shadowElevation = 4.dp,
            color = if (followMyLocation) Color(0xFF2E7DFF) else Color(0xFF3E3E3E),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 16.dp)
        ) {
            Box(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text(if (followMyLocation) "FOLLOW ON" else "FOLLOW OFF", fontSize = 11.sp, color = Color.White)
            }
        }

        // 우하단 현위치 버튼 (follow 해제된 상태에서도 강제 이동 가능)
        Surface(
            onClick = {
                kakaoMap?.let { map ->
                    map.moveCamera(CameraUpdateFactory.newCenterPosition(myLatLng))
                    addOrUpdateMyMarker(map, myLatLng)
                }
            },
            color = Color.White,
            shape = RoundedCornerShape(50),
            shadowElevation = 6.dp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 120.dp, end = 20.dp)
        ) {
            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (followMyLocation) "◎" else "●", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF2E7DFF))
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun updateOnceLocation(
    fusedClient: FusedLocationProviderClient,
    onLocation: (LatLng) -> Unit
) {
    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
        .addOnSuccessListener { loc ->
            if (loc != null) {
                val latLng = LatLng.from(loc.latitude, loc.longitude)

                // 에뮬레이터에서 캘리포니아 좌표 감지시 서울 좌표로 대체
                val finalLatLng = if (isEmulatorLocation(latLng)) {
                    // 서울 시청 좌표로 대체
                    LatLng.from(37.5666102, 126.9783881)
                } else {
                    latLng
                }

                onLocation(finalLatLng)
            }
        }
}

// 에뮬레이터 기본 위치(캘리포니아) 감지 함수
private fun isEmulatorLocation(latLng: LatLng): Boolean {
    // 캘리포니아 범위 (에뮬레이터 기본 위치)
    return latLng.latitude in 37.0..38.0 && latLng.longitude in -123.0..-121.0
}

private fun addOrUpdateMyMarker(map: KakaoMap?, latLng: LatLng) {
    if (map == null) {
        println("DEBUG: KakaoMap is null")
        return
    }
    val labelManager = map.labelManager
    if (labelManager == null) {
        println("DEBUG: LabelManager is null")
        return
    }

    println("DEBUG: Adding marker at ${latLng.latitude}, ${latLng.longitude}")

    try {
        // 기존 레이어 확인 및 라벨 제거
        val existingLayer = labelManager.getLayer("my_layer")
        if (existingLayer != null) {
            existingLayer.getLabel("location_marker")?.let { label ->
                existingLayer.remove(label)
            }
        } else {
            // 새 레이어 생성
            val layerOptions = LabelLayerOptions.from("my_layer").setZOrder(10002)
            labelManager.addLayer(layerOptions)
        }

        val layer = labelManager.getLayer("my_layer")
        if (layer == null) {
            println("DEBUG: Failed to get or create layer")
            return
        }

        // 빨간 핀 아이콘 사용한 마커 생성
        val red = Color(0xFFFF0000).toArgb()
        val textBuilder = LabelTextBuilder().setTexts("📍") // 핀 이모지 사용
        val textStyle = LabelTextStyle.from(48, red) // 32 → 48로 크기 증가
        val style = LabelStyle.from(textStyle)

        val options = LabelOptions.from("location_marker", latLng)
            .setStyles(style)
            .setTexts(textBuilder)

        val label = layer.addLabel(options)
        if (label != null) {
            println("DEBUG: Location marker added successfully")
        } else {
            println("DEBUG: Failed to add location marker")
        }

    } catch (e: Exception) {
        println("DEBUG: Exception while adding marker: ${e.message}")
        e.printStackTrace()

        // 대안: 간단한 텍스트 마커 생성
        try {
            val layer = labelManager.getLayer("my_layer") ?: run {
                val layerOptions = LabelLayerOptions.from("my_layer")
                labelManager.addLayer(layerOptions)
            }

            layer?.let { l ->
                val red = Color(0xFFFF0000).toArgb()
                val textStyle = LabelTextStyle.from(24, red)
                val style = LabelStyle.from(textStyle)

                val options = LabelOptions.from("simple_marker", latLng).setStyles(style)
                val textBuilder = LabelTextBuilder().setTexts("●")
                options.setTexts(textBuilder)

                val label = l.addLabel(options)
                println("DEBUG: Simple marker created: ${label != null}")
            }
        } catch (fallbackException: Exception) {
            println("DEBUG: Fallback marker creation also failed: ${fallbackException.message}")
        }
    }
}

// 목적지 마커 추가 함수
private fun addDestinationMarker(map: KakaoMap, position: LatLng) {
    val labelManager = map.labelManager ?: return

    try {
        // 기존 목적지 마커 제거
        val layer = labelManager.getLayer("destination_layer") ?: run {
            val layerOptions = LabelLayerOptions.from("destination_layer").setZOrder(10001)
            labelManager.addLayer(layerOptions)
        }

        // 기존 목적지 마커가 있다면 제거
        layer?.getLabel("destination_marker")?.let { existingLabel ->
            layer.remove(existingLabel)
        }

        // 새 목적지 마커 추가
        val blue = Color(0xFF0066FF).toArgb()
        val textBuilder = LabelTextBuilder().setTexts("🎯") // 목적지 이모지
        val textStyle = LabelTextStyle.from(32, blue)
        val style = LabelStyle.from(textStyle)

        val options = LabelOptions.from("destination_marker", position)
            .setStyles(style)
            .setTexts(textBuilder)

        layer?.addLabel(options)
        println("DEBUG: Destination marker added at ${position.latitude}, ${position.longitude}")

    } catch (e: Exception) {
        println("DEBUG: Failed to add destination marker: ${e.message}")
    }
}

// 팀원들 마커 추가 함수 (예시 데이터)
private fun addTeamMembersMarkers(map: KakaoMap) {
    val labelManager = map.labelManager ?: return

    try {
        val layer = labelManager.getLayer("team_layer") ?: run {
            val layerOptions = LabelLayerOptions.from("team_layer").setZOrder(10000)
            labelManager.addLayer(layerOptions)
        }

        // 예시 팀원 위치들 (서울 주변)
        val teamMembers = listOf(
            LatLng.from(37.5700, 126.9800) to "👤",  // 팀원 1
            LatLng.from(37.5600, 126.9700) to "👤",  // 팀원 2
            LatLng.from(37.5750, 126.9850) to "👤"   // 팀원 3
        )

        teamMembers.forEachIndexed { index, (position, emoji) ->
            val green = Color(0xFF00AA00).toArgb()
            val textBuilder = LabelTextBuilder().setTexts(emoji)
            val textStyle = LabelTextStyle.from(28, green)
            val style = LabelStyle.from(textStyle)

            val options = LabelOptions.from("team_member_$index", position)
                .setStyles(style)
                .setTexts(textBuilder)

            layer?.addLabel(options)
        }

        println("DEBUG: Team members markers added")

    } catch (e: Exception) {
        println("DEBUG: Failed to add team members markers: ${e.message}")
    }
}

// 경로 표시 함수 (간단한 직선 경로)
private fun drawRoute(map: KakaoMap, start: LatLng, destination: LatLng) {
    try {
        // 간단한 점선으로 경로 표시
        drawSimpleRoute(map, start, destination)

        // 경로에 맞게 카메라 조정 (제거 - 사용자가 수동으로 조정하도록)
        // adjustCameraToRoute(map, start, destination)

        println("DEBUG: Route drawn from ${start.latitude},${start.longitude} to ${destination.latitude},${destination.longitude}")

    } catch (e: Exception) {
        println("DEBUG: Failed to draw route: ${e.message}")
        e.printStackTrace()
    }
}

// 간단한 경로 표시 (점선으로 경로 표시)
private fun drawSimpleRoute(map: KakaoMap, start: LatLng, destination: LatLng) {
    try {
        val labelManager = map.labelManager ?: return

        val layer = labelManager.getLayer("route_line_layer") ?: run {
            val layerOptions = LabelLayerOptions.from("route_line_layer").setZOrder(5000)
            labelManager.addLayer(layerOptions)
        }

        // 기존 경로 라인 제거
        layer?.removeAll()

        // 중간 지점들을 생성해서 점선으로 경로 표시
        val numPoints = 10 // 점의 개수
        val routePoints = mutableListOf<LatLng>()

        for (i in 0..numPoints) {
            val ratio = i.toDouble() / numPoints
            val lat = start.latitude + (destination.latitude - start.latitude) * ratio
            val lng = start.longitude + (destination.longitude - start.longitude) * ratio
            routePoints.add(LatLng.from(lat, lng))
        }

        routePoints.forEachIndexed { index, point ->
            val blue = Color(0xFF0066FF).toArgb()
            val textBuilder = LabelTextBuilder().setTexts("•")
            val textStyle = LabelTextStyle.from(12, blue)
            val style = LabelStyle.from(textStyle)

            val options = LabelOptions.from("route_point_$index", point)
                .setStyles(style)
                .setTexts(textBuilder)

            layer?.addLabel(options)
        }

        println("DEBUG: Simple route drawn with ${routePoints.size} points")

    } catch (e: Exception) {
        println("DEBUG: Failed to draw simple route: ${e.message}")
    }
}

// 경로에 맞게 카메라 조정
private fun adjustCameraToRoute(map: KakaoMap, start: LatLng, destination: LatLng) {
    try {
        // 시작점과 목적지를 모두 포함하는 카메라 위치 계산
        val centerLat = (start.latitude + destination.latitude) / 2
        val centerLng = (start.longitude + destination.longitude) / 2
        val centerPoint = LatLng.from(centerLat, centerLng)

        // 거리에 따른 적절한 줌 레벨 계산
        val distance = calculateDistance(start, destination)
        val zoomLevel = when {
            distance < 1.0 -> 16 // 1km 미만
            distance < 5.0 -> 14 // 5km 미만
            distance < 10.0 -> 12 // 10km 미만
            distance < 50.0 -> 10 // 50km 미만
            else -> 8 // 50km 이상
        }

        val cameraPosition = CameraPosition.from(
            centerPoint.latitude, centerPoint.longitude,
            zoomLevel, 0.0, 0.0, 0.0
        )

        map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

        println("DEBUG: Camera adjusted for route, distance: ${distance}km, zoom: $zoomLevel")

    } catch (e: Exception) {
        println("DEBUG: Failed to adjust camera: ${e.message}")
    }
}

// 두 점 사이의 거리 계산 (단순 계산)
private fun calculateDistance(start: LatLng, destination: LatLng): Double {
    val latDiff = start.latitude - destination.latitude
    val lngDiff = start.longitude - destination.longitude
    return kotlin.math.sqrt(latDiff * latDiff + lngDiff * lngDiff) * 111.0 // 대략적인 km 변환
}
