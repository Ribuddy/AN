package net.ritirp.myapplication.presentation.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.label.LabelTextStyle
import net.ritirp.myapplication.data.model.LocationData
import net.ritirp.myapplication.data.model.MarkerData
import net.ritirp.myapplication.data.model.MarkerType
import net.ritirp.myapplication.data.model.RouteData

/**
 * 지도 관련 유틸리티 함수들
 */
object MapUtils {

    /**
     * 현재 위치 마커 추가/업데이트
     */
    fun addOrUpdateCurrentLocationMarker(map: KakaoMap?, location: LocationData) {
        if (map == null) {
            println("DEBUG: Map is null")
            return
        }
        val labelManager = map.labelManager
        if (labelManager == null) {
            println("DEBUG: LabelManager is null")
            return
        }

        println("DEBUG: Adding current location marker at ${location.latitude}, ${location.longitude}")

        try {
            // 기존 레이어 확인 및 라벨 제거
            val existingLayer = labelManager.getLayer("current_location_layer")
            if (existingLayer != null) {
                existingLayer.getLabel("current_location_marker")?.let { label ->
                    existingLayer.remove(label)
                }
            } else {
                // 새 레이어 생성
                val layerOptions = LabelLayerOptions.from("current_location_layer").setZOrder(10002)
                labelManager.addLayer(layerOptions)
            }

            val layer = labelManager.getLayer("current_location_layer")
            if (layer == null) {
                println("DEBUG: Failed to get or create current location layer")
                return
            }

            // 현재 위치 마커 생성
            val red = Color(0xFFFF0000).toArgb()
            val textBuilder = LabelTextBuilder().setTexts("📍")
            val textStyle = LabelTextStyle.from(48, red)
            val style = LabelStyle.from(textStyle)

            val options = LabelOptions.from("current_location_marker", location.toLatLng())
                .setStyles(style)
                .setTexts(textBuilder)

            val label = layer.addLabel(options)
            if (label != null) {
                println("DEBUG: Current location marker added successfully")
            } else {
                println("DEBUG: Failed to add current location marker")
            }

        } catch (e: Exception) {
            println("DEBUG: Exception while adding current location marker: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 목적지 마커 추가
     */
    fun addDestinationMarker(map: KakaoMap, location: LocationData) {
        val labelManager = map.labelManager
        if (labelManager == null) {
            println("DEBUG: LabelManager is null for destination marker")
            return
        }

        println("DEBUG: Adding destination marker at ${location.latitude}, ${location.longitude}")

        try {
            // 기존 destination_layer 완전히 제거하고 다시 생성
            labelManager.getLayer("destination_layer")?.let { existingLayer ->
                labelManager.remove(existingLayer)
                println("DEBUG: Removed existing destination layer")
            }

            // 새 레이어 생성
            val layerOptions = LabelLayerOptions.from("destination_layer").setZOrder(10001)
            val layer = labelManager.addLayer(layerOptions)

            if (layer == null) {
                println("DEBUG: Failed to create destination layer")
                return
            }

            // 목적지 마커 추가 - 더 큰 사이즈와 다른 이모지 사용
            val blue = Color(0xFF0066FF).toArgb()
            val textBuilder = LabelTextBuilder().setTexts("🚩") // 깃발 이모지로 변경
            val textStyle = LabelTextStyle.from(56, blue) // 48 → 56으로 크기 증가
            val style = LabelStyle.from(textStyle)

            val options = LabelOptions.from("destination_marker", location.toLatLng())
                .setStyles(style)
                .setTexts(textBuilder)

            val label = layer.addLabel(options)
            if (label != null) {
                println("DEBUG: Destination marker (🚩) added successfully with size 56")
            } else {
                println("DEBUG: Failed to add destination marker")
            }

        } catch (e: Exception) {
            println("DEBUG: Exception adding destination marker: ${e.message}")
            e.printStackTrace()

            // 백업 방식: 간단한 원형 마커
            try {
                val layer = labelManager.getLayer("destination_layer") ?: run {
                    val layerOptions = LabelLayerOptions.from("destination_layer")
                    labelManager.addLayer(layerOptions)
                }

                layer?.let { l ->
                    val red = Color(0xFFFF0000).toArgb()
                    val textBuilder = LabelTextBuilder().setTexts("⭕")
                    val textStyle = LabelTextStyle.from(40, red)
                    val style = LabelStyle.from(textStyle)

                    val options = LabelOptions.from("backup_destination", location.toLatLng())
                        .setStyles(style)
                        .setTexts(textBuilder)

                    val backupLabel = l.addLabel(options)
                    println("DEBUG: Backup destination marker created: ${backupLabel != null}")
                }
            } catch (backupException: Exception) {
                println("DEBUG: Backup destination marker also failed: ${backupException.message}")
            }
        }
    }

    /**
     * 팀원 마커들 추가
     */
    fun addTeamMarkers(map: KakaoMap, markers: List<MarkerData>) {
        val labelManager = map.labelManager ?: return

        println("DEBUG: Adding team markers, count: ${markers.size}")

        try {
            val layer = labelManager.getLayer("team_layer") ?: run {
                val layerOptions = LabelLayerOptions.from("team_layer").setZOrder(10000)
                labelManager.addLayer(layerOptions)
            }

            // 기존 마커들 제거
            layer?.removeAll()

            val teamMarkers = markers.filter { it.type == MarkerType.TEAM_MEMBER }
            println("DEBUG: Filtered team markers count: ${teamMarkers.size}")

            teamMarkers.forEach { marker ->
                val green = Color(0xFF00AA00).toArgb()
                val textBuilder = LabelTextBuilder().setTexts(marker.emoji)
                val textStyle = LabelTextStyle.from(40, green)
                val style = LabelStyle.from(textStyle)

                val options = LabelOptions.from(marker.id, marker.location.toLatLng())
                    .setStyles(style)
                    .setTexts(textBuilder)

                val label = layer?.addLabel(options)
                if (label != null) {
                    println("DEBUG: Team marker ${marker.id} added successfully")
                } else {
                    println("DEBUG: Failed to add team marker ${marker.id}")
                }
            }

        } catch (e: Exception) {
            println("DEBUG: Failed to add team markers: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 경로 표시 (점선)
     */
    fun drawRoute(map: KakaoMap, route: RouteData) {
        val labelManager = map.labelManager ?: return

        try {
            val layer = labelManager.getLayer("route_layer") ?: run {
                val layerOptions = LabelLayerOptions.from("route_layer").setZOrder(5000)
                labelManager.addLayer(layerOptions)
            }

            // 기존 경로 제거
            layer?.removeAll()

            // 경로 점들 표시
            route.routePoints.forEachIndexed { index, point ->
                val blue = Color(0xFF0066FF).toArgb()
                val textBuilder = LabelTextBuilder().setTexts("•")
                val textStyle = LabelTextStyle.from(12, blue)
                val style = LabelStyle.from(textStyle)

                val options = LabelOptions.from("route_point_$index", point.toLatLng())
                    .setStyles(style)
                    .setTexts(textBuilder)

                layer?.addLabel(options)
            }

        } catch (e: Exception) {
            println("DEBUG: Failed to draw route: ${e.message}")
        }
    }

    /**
     * 모든 마커와 경로 제거
     */
    fun clearAllMarkersAndRoutes(map: KakaoMap) {
        val labelManager = map.labelManager ?: return

        listOf("current_location_layer", "destination_layer", "team_layer", "route_layer").forEach { layerId ->
            labelManager.getLayer(layerId)?.removeAll()
        }
    }

    /**
     * 카메라를 특정 위치로 이동
     */
    fun moveCameraToLocation(map: KakaoMap?, location: LocationData, zoomLevel: Int = 13) {
        if (map == null) {
            println("DEBUG: Map is null, cannot move camera")
            return
        }

        try {
            val cameraPosition = com.kakao.vectormap.camera.CameraPosition.from(
                location.latitude,
                location.longitude,
                zoomLevel, 0.0, 0.0, 0.0
            )
            map.moveCamera(com.kakao.vectormap.camera.CameraUpdateFactory.newCameraPosition(cameraPosition))
            println("DEBUG: Camera moved to ${location.latitude}, ${location.longitude} with zoom $zoomLevel")
        } catch (e: Exception) {
            println("DEBUG: Exception while moving camera: ${e.message}")
            e.printStackTrace()
        }
    }
}
