package net.ritirp.myapplication.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ritirp.myapplication.data.model.RidingMetrics
import kotlin.math.roundToInt

/**
 * 주행 통계를 표시하는 카드 컴포넌트
 */
@Composable
fun RidingMetricsCard(
    metrics: RidingMetrics,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = "주행 통계",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 첫 번째 행: Distance & Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricItem(
                    label = "Distance",
                    value = String.format("%.2f", metrics.distanceInKm),
                    unit = "km",
                    modifier = Modifier.weight(1f),
                )

                Spacer(modifier = Modifier.width(16.dp))

                MetricItem(
                    label = "Duration",
                    value = formatDuration(metrics.durationInSeconds.toLong()),
                    unit = "",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 두 번째 행: Top Speed & Avg Speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricItem(
                    label = "Top Speed",
                    value = String.format("%.1f", metrics.topSpeedInKmh),
                    unit = "km/h",
                    modifier = Modifier.weight(1f),
                )

                Spacer(modifier = Modifier.width(16.dp))

                MetricItem(
                    label = "Current Speed",
                    value = String.format("%.1f", metrics.currentSpeedInKmh),
                    unit = "km/h",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 세 번째 행: Climb & Fall
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricItem(
                    label = "Climb",
                    value = String.format("%.1f", metrics.totalClimb),
                    unit = "m",
                    modifier = Modifier.weight(1f),
                    valueColor = Color(0xFF4CAF50), // Green
                )

                Spacer(modifier = Modifier.width(16.dp))

                MetricItem(
                    label = "Fall",
                    value = String.format("%.1f", metrics.totalFall),
                    unit = "m",
                    modifier = Modifier.weight(1f),
                    valueColor = Color(0xFFF44336), // Red
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 네 번째 행: Lean Angle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricItem(
                    label = "Max Lean Angle",
                    value = String.format("%.1f", metrics.maxLeanAngle),
                    unit = "°",
                    modifier = Modifier.weight(1f),
                    valueColor = Color(0xFFFF9800), // Orange
                )

                Spacer(modifier = Modifier.width(16.dp))

                MetricItem(
                    label = "Current Lean",
                    value = String.format("%.1f", metrics.currentLeanAngle),
                    unit = "°",
                    modifier = Modifier.weight(1f),
                    valueColor = Color(0xFFFF9800), // Orange
                )
            }
        }
    }
}

/**
 * 개별 통계 항목
 */
@Composable
private fun MetricItem(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color(0xFF1976D2),
) {
    Column(
        modifier = modifier
            .background(
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
            )

            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    fontSize = 14.sp,
                    color = Color.Gray,
                )
            }
        }
    }
}

/**
 * 시간을 포맷팅 (HH:MM:SS)
 */
private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, secs)
        else -> String.format("%02d:%02d", minutes, secs)
    }
}

/**
 * 컴팩트한 주행 통계 바 (지도 위에 오버레이)
 */
@Composable
fun RidingMetricsBar(
    metrics: RidingMetrics,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Distance
            CompactMetricItem(
                label = "거리",
                value = "${String.format("%.1f", metrics.distanceInKm)} km",
            )

            // Duration
            CompactMetricItem(
                label = "시간",
                value = formatDuration(metrics.durationInSeconds.toLong()),
            )

            // Speed
            CompactMetricItem(
                label = "속도",
                value = "${metrics.currentSpeedInKmh.roundToInt()} km/h",
            )

            // Lean Angle
            CompactMetricItem(
                label = "기울기",
                value = "${metrics.currentLeanAngle.roundToInt()}°",
            )
        }
    }
}

@Composable
private fun CompactMetricItem(
    label: String,
    value: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.Gray,
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2),
        )
    }
}
