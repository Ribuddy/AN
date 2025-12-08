package net.ritirp.myapplication.presentation.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.label.LabelTextStyle
import net.ritirp.myapplication.R
import net.ritirp.myapplication.data.model.LocationData
import net.ritirp.myapplication.data.model.MarkerData
import net.ritirp.myapplication.data.model.MarkerType
import net.ritirp.myapplication.data.model.RouteData

/**
 * 지도 관련 유틸리티 함수들
 */
object MapUtils {
    // 친구 목록 캐싱 (diff 계산용)
    private val previousBuddyIds = mutableSetOf<String>()

    // Bitmap 캐시 (lazy initialization)
    private var cachedBuddyBitmap: Bitmap? = null
    private var cachedAccidentBitmap: Bitmap? = null

    /**
     * VectorDrawable을 Bitmap으로 변환
     * @param context Context
     * @param drawableId Drawable 리소스 ID
     * @param sizeDp 크기 (dp) - 원본 비율을 유지하며 긴 쪽이 이 크기가 됨
     * @return Bitmap
     */
    fun vectorToBitmap(context: Context, drawableId: Int, sizeDp: Int): Bitmap? {
        return try {
            val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
            val density = context.resources.displayMetrics.density
            val sizePx = (sizeDp * density).toInt()

            // 원본 비율 계산
            val intrinsicWidth = drawable.intrinsicWidth
            val intrinsicHeight = drawable.intrinsicHeight

            // 원본 비율을 유지하면서 긴 쪽을 sizePx로 맞춤
            val (width, height) = if (intrinsicWidth > intrinsicHeight) {
                val ratio = intrinsicHeight.toFloat() / intrinsicWidth.toFloat()
                Pair(sizePx, (sizePx * ratio).toInt())
            } else {
                val ratio = intrinsicWidth.toFloat() / intrinsicHeight.toFloat()
                Pair((sizePx * ratio).toInt(), sizePx)
            }

            println("DEBUG: Converting vector to bitmap - original: ${intrinsicWidth}x${intrinsicHeight}, output: ${width}x${height}")
            drawable.toBitmap(width = width, height = height, config = Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            println("DEBUG: Failed to convert vector to bitmap: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Buddy 아이콘 Bitmap 가져오기 (캐싱)
     */
    private fun getBuddyBitmap(context: Context): Bitmap? {
        if (cachedBuddyBitmap == null) {
            cachedBuddyBitmap = vectorToBitmap(context, R.drawable.ic_buddy, 40)
            println("DEBUG: Buddy bitmap cached")
        }
        return cachedBuddyBitmap
    }

    /**
     * 사고 아이콘 Bitmap 가져오기 (캐싱)
     */
    private fun getAccidentBitmap(context: Context): Bitmap? {
        if (cachedAccidentBitmap == null) {
            cachedAccidentBitmap = vectorToBitmap(context, R.drawable.ic_accident_friend, 80)
            println("DEBUG: Accident bitmap cached")
        }
        return cachedAccidentBitmap
    }

    /**
     * 내 위치 라벨 추가/업데이트 (layer_me)
     * - 레이어: layer_me
     * - 라벨 ID: "me"
     * - 아이콘: Bitmap (R.drawable.ic_buddy 또는 사고 시 R.drawable.ic_accident_friend)
     * - 존재하면 삭제 후 재생성 (스타일 변경을 위해)
     * @param isAccident 사고 발생 여부
     */
    fun addOrUpdateCurrentLocationMarker(
        map: KakaoMap?,
        location: LocationData,
        context: Context,
        isAccident: Boolean = false,
    ) {
        if (map == null) {
            println("DEBUG: Map is null")
            return
        }
        val labelManager = map.labelManager
        if (labelManager == null) {
            println("DEBUG: LabelManager is null")
            return
        }

        println("DEBUG: Updating my location label at ${location.latitude}, ${location.longitude}, accident=$isAccident")

        try {
            // layer_me 레이어 가져오기 또는 생성 (Z-Order를 낮춰서 팀원 마커 아래에 표시)
            val layer = labelManager.getLayer("layer_me") ?: run {
                val layerOptions = LabelLayerOptions.from("layer_me").setZOrder(10001)
                labelManager.addLayer(layerOptions)
            }

            if (layer == null) {
                println("DEBUG: Failed to get or create layer_me")
                return
            }

            val latLng = LatLng.from(location.latitude, location.longitude)
            val existingLabel = layer.getLabel("me")

            // 적절한 비트맵 선택
            val bitmap = if (isAccident) {
                println("DEBUG: Using ACCIDENT bitmap for my location")
                getAccidentBitmap(context)
            } else {
                println("DEBUG: Using BUDDY bitmap for my location")
                getBuddyBitmap(context)
            }

            if (bitmap == null) {
                println("DEBUG: Failed to get bitmap for my location")
                return
            }

            // 기존 라벨이 있으면 삭제 (스타일 변경을 위해)
            if (existingLabel != null) {
                layer.remove(existingLabel)
                println("DEBUG: Removed existing my location label for recreation")
            }

            // 새로 생성
            val options = LabelOptions
                .from("me", latLng)
                .setStyles(bitmap)

            val label = layer.addLabel(options)
            if (label != null) {
                println("DEBUG: ✅ My location label created with ${if (isAccident) "ACCIDENT" else "BUDDY"} icon")
            } else {
                println("DEBUG: ❌ Failed to create my location label")
            }
        } catch (e: Exception) {
            println("DEBUG: Exception while updating my location label: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 목적지 마커 추가 (layer_destination)
     */
    fun addDestinationMarker(
        map: KakaoMap,
        location: LocationData,
    ) {
        val labelManager = map.labelManager
        if (labelManager == null) {
            println("DEBUG: LabelManager is null for destination marker")
            return
        }

        println("DEBUG: Adding destination marker at ${location.latitude}, ${location.longitude}")

        try {
            // layer_destination 레이어 가져오기 또는 생성
            val layer = labelManager.getLayer("layer_destination") ?: run {
                val layerOptions = LabelLayerOptions.from("layer_destination").setZOrder(10000)
                labelManager.addLayer(layerOptions)
            }

            if (layer == null) {
                println("DEBUG: Failed to get or create layer_destination")
                return
            }

            // 기존 목적지 마커 제거
            layer.removeAll()

            // 목적지 마커 추가
            val blue = android.graphics.Color.parseColor("#0066FF")
            val textBuilder = LabelTextBuilder().setTexts("🚩")
            val textStyle = LabelTextStyle.from(56, blue)
            val style = LabelStyle.from(textStyle)

            val latLng = LatLng.from(location.latitude, location.longitude)
            val options = LabelOptions
                .from("destination_marker", latLng)
                .setStyles(LabelStyles.from(style))
                .setTexts(textBuilder)

            val label = layer.addLabel(options)
            if (label != null) {
                println("DEBUG: Destination marker added successfully")
            } else {
                println("DEBUG: Failed to add destination marker")
            }
        } catch (e: Exception) {
            println("DEBUG: Exception adding destination marker: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 친구(팀원) 라벨들 추가/업데이트 (layer_buddies)
     * - 레이어: layer_buddies
     * - 라벨 ID: "buddy_${marker.id}"
     * - 아이콘: Bitmap (R.drawable.ic_buddy)
     * - 존재하면 moveTo로 업데이트, 없으면 생성
     * - markers에 없는 라벨은 삭제 (diff 관리)
     */
    fun addTeamMarkers(
        map: KakaoMap,
        markers: List<MarkerData>,
        context: Context,
        accidentUserIds: Set<String> = emptySet(), // 사고난 팀원 ID 목록
    ) {
        val labelManager = map.labelManager ?: return

        println("DEBUG: Updating team markers, count: ${markers.size}, accidents: ${accidentUserIds.size}")

        try {
            // layer_buddies 레이어 가져오기 또는 생성
            val layer = labelManager.getLayer("layer_buddies") ?: run {
                val layerOptions = LabelLayerOptions.from("layer_buddies").setZOrder(10003)
                labelManager.addLayer(layerOptions)
            }

            if (layer == null) {
                println("DEBUG: Failed to get or create layer_buddies")
                return
            }

            val teamMarkers = markers.filter { it.type == MarkerType.TEAM_MEMBER }
            println("DEBUG: Filtered team markers count: ${teamMarkers.size}")

            // 현재 마커 ID 세트
            val currentBuddyIds = teamMarkers.map { "buddy_${it.id}" }.toSet()

            // 1. 기존 라벨 업데이트 또는 새로 생성
            teamMarkers.forEach { marker ->
                val labelId = "buddy_${marker.id}"
                val latLng = LatLng.from(marker.location.latitude, marker.location.longitude)

                // 사고난 팀원인지 확인
                val isAccident = accidentUserIds.contains(marker.id)
                println("DEBUG: Processing marker ${marker.id}, isAccident=$isAccident, accidentUserIds=$accidentUserIds")

                // 적절한 비트맵 선택
                val bitmap = if (isAccident) {
                    println("DEBUG: Using ACCIDENT bitmap for ${marker.id}")
                    getAccidentBitmap(context)
                } else {
                    println("DEBUG: Using BUDDY bitmap for ${marker.id}")
                    getBuddyBitmap(context)
                }

                if (bitmap == null) {
                    println("DEBUG: Failed to get bitmap for $labelId")
                    return@forEach
                }

                // 기존 라벨이 있으면 항상 삭제 (스타일 변경을 위해)
                val existingLabel = layer.getLabel(labelId)
                if (existingLabel != null) {
                    layer.remove(existingLabel)
                    println("DEBUG: Removed existing label $labelId for recreation (accident: $isAccident)")
                }

                // 라벨 생성 (항상 새로 생성)
                val options = LabelOptions
                    .from(labelId, latLng)
                    .setStyles(bitmap)

                val label = layer.addLabel(options)
                if (label != null) {
                    println("DEBUG: ✅ Buddy label $labelId created with ${if (isAccident) "ACCIDENT" else "BUDDY"} icon")
                } else {
                    println("DEBUG: ❌ Failed to create buddy label $labelId")
                }
            }

            // 2. markers에 더 이상 없는 라벨 삭제 (diff)
            val labelsToRemove = previousBuddyIds - currentBuddyIds
            labelsToRemove.forEach { labelId ->
                layer.getLabel(labelId)?.let { label ->
                    layer.remove(label)
                    println("DEBUG: Removed buddy label $labelId (no longer in markers list)")
                }
            }

            // 3. 캐시 업데이트
            previousBuddyIds.clear()
            previousBuddyIds.addAll(currentBuddyIds)

        } catch (e: Exception) {
            println("DEBUG: Failed to update team markers: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 팀 마커 초기화 (라이딩 종료 시)
     */
    fun clearTeamMarkers(map: KakaoMap) {
        val labelManager = map.labelManager ?: return

        try {
            labelManager.getLayer("layer_buddies")?.removeAll()
            previousBuddyIds.clear()
            println("DEBUG: All buddy labels cleared")
        } catch (e: Exception) {
            println("DEBUG: Failed to clear team markers: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 경로 표시 (RouteLine 방식)
     * @param map KakaoMap 인스턴스
     * @param routePoints 경로 좌표 리스트 (LatLng)
     */
    fun drawRouteLine(
        map: KakaoMap,
        routePoints: List<LatLng>,
    ) {
        if (routePoints.isEmpty()) {
            println("DEBUG: No route points to draw")
            return
        }

        val routeLineManager = map.routeLineManager ?: run {
            println("DEBUG: RouteLineManager is null")
            return
        }

        try {
            // 기존 경로 제거
            routeLineManager.layer?.removeAll()

            // RouteLineSegment 생성
            val segment = com.kakao.vectormap.route.RouteLineSegment.from(routePoints)
                .setStyles(
                    com.kakao.vectormap.route.RouteLineStyle.from(
                        10f, // 선 두께
                        android.graphics.Color.parseColor("#0066FF") // 파란색
                    )
                )

            // RouteLineOptions 생성
            val options = com.kakao.vectormap.route.RouteLineOptions.from(segment)
                .setStylesSet(
                    com.kakao.vectormap.route.RouteLineStylesSet.from(
                        com.kakao.vectormap.route.RouteLineStyles.from(
                            com.kakao.vectormap.route.RouteLineStyle.from(
                                10f,
                                android.graphics.Color.parseColor("#0066FF")
                            )
                        )
                    )
                )

            // RouteLine 추가
            val routeLine = routeLineManager.layer?.addRouteLine(options)

            if (routeLine != null) {
                println("DEBUG: RouteLine drawn successfully with ${routePoints.size} points")
            } else {
                println("DEBUG: Failed to add RouteLine")
            }
        } catch (e: Exception) {
            println("DEBUG: Exception drawing RouteLine: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 경로 표시 (점선) - 레거시 방식 (사용 안 함)
     */
    @Deprecated("Use drawRouteLine instead")
    fun drawRoute(
        map: KakaoMap,
        route: RouteData,
    ) {
        val labelManager = map.labelManager ?: return

        try {
            val layer = labelManager.getLayer("layer_route") ?: run {
                val layerOptions = LabelLayerOptions.from("layer_route").setZOrder(5000)
                labelManager.addLayer(layerOptions)
            }

            if (layer == null) {
                println("DEBUG: Failed to get or create layer_route")
                return
            }

            // 기존 경로 제거
            layer.removeAll()

            // 경로 점들 표시
            route.routePoints.forEachIndexed { index, point ->
                val blue = android.graphics.Color.parseColor("#0066FF")
                val textBuilder = LabelTextBuilder().setTexts("•")
                val textStyle = LabelTextStyle.from(12, blue)
                val style = LabelStyle.from(textStyle)

                val latLng = LatLng.from(point.latitude, point.longitude)
                val options = LabelOptions
                    .from("route_point_$index", latLng)
                    .setStyles(LabelStyles.from(style))
                    .setTexts(textBuilder)

                layer.addLabel(options)
            }
            println("DEBUG: Route drawn with ${route.routePoints.size} points")
        } catch (e: Exception) {
            println("DEBUG: Failed to draw route: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 모든 마커와 경로 제거
     */
    fun clearAllMarkersAndRoutes(map: KakaoMap) {
        val labelManager = map.labelManager ?: return

        listOf("layer_me", "layer_buddies", "layer_destination", "layer_route").forEach { layerId ->
            labelManager.getLayer(layerId)?.removeAll()
        }
        previousBuddyIds.clear()
        println("DEBUG: All layers cleared")
    }

    /**
     * 카메라를 특정 위치로 이동
     */
    fun moveCameraToLocation(
        map: KakaoMap?,
        location: LocationData,
        zoomLevel: Int = 11,
    ) {
        if (map == null) {
            println("DEBUG: Map is null, cannot move camera")
            return
        }

        try {
            val cameraPosition =
                com.kakao.vectormap.camera.CameraPosition.from(
                    location.latitude,
                    location.longitude,
                    zoomLevel,
                    0.0,
                    0.0,
                    0.0,
                )
            map.moveCamera(
                com.kakao.vectormap.camera.CameraUpdateFactory
                    .newCameraPosition(cameraPosition),
            )
            println("DEBUG: Camera moved to ${location.latitude}, ${location.longitude} with zoom $zoomLevel")
        } catch (e: Exception) {
            println("DEBUG: Exception while moving camera: ${e.message}")
            e.printStackTrace()
        }
    }
}
