package net.ritirp.myapplication.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.ritirp.myapplication.data.local.entity.RidingRecordEntity
import net.ritirp.myapplication.presentation.viewmodel.RidingReportViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 주행 리포트 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RidingReportScreen(
    viewModel: RidingReportViewModel,
    modifier: Modifier = Modifier,
) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    var selectedRecord by remember { mutableStateOf<RidingRecordEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf<RidingRecordEntity?>(null) }

    if (selectedRecord != null) {
        // 상세 화면
        RidingRecordDetailScreen(
            record = selectedRecord!!,
            onBack = { selectedRecord = null },
            onDelete = { record ->
                showDeleteDialog = record
            },
        )
    } else {
        // 목록 화면
        Column(
            modifier = modifier.fillMaxSize(),
        ) {
            // 헤더
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "주행 리포트",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "총 ${records.size}개의 주행 기록",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            // 리스트
            if (records.isEmpty()) {
                // 빈 상태
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBike,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color.Gray,
                        )
                        Text(
                            text = "주행 기록이 없습니다",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray,
                        )
                        Text(
                            text = "첫 라이딩을 시작해보세요!",
                            fontSize = 14.sp,
                            color = Color.Gray,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(records) { record ->
                        RidingRecordCard(
                            record = record,
                            onClick = { selectedRecord = record },
                            onDelete = { showDeleteDialog = record },
                        )
                    }
                }
            }
        }
    }

    // 삭제 확인 다이얼로그
    showDeleteDialog?.let { record ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("주행 기록 삭제") },
            text = { Text("이 주행 기록을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecord(record)
                        showDeleteDialog = null
                        if (selectedRecord?.id == record.id) {
                            selectedRecord = null
                        }
                    },
                ) {
                    Text("삭제", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("취소")
                }
            },
        )
    }
}

/**
 * 주행 기록 카드
 */
@Composable
fun RidingRecordCard(
    record: RidingRecordEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // 날짜 및 시간
                Text(
                    text = formatDateTime(record.startTime),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 주요 통계
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    StatItem(
                        icon = Icons.Default.Place,
                        value = String.format(java.util.Locale.getDefault(), "%.2f km", record.distanceMeters / 1000),
                    )
                    StatItem(
                        icon = Icons.Default.Timer,
                        value = formatDuration(record.durationMillis / 1000),
                    )
                    StatItem(
                        icon = Icons.Default.Speed,
                        value = String.format(java.util.Locale.getDefault(), "%.1f km/h", record.maxSpeedKmh),
                    )
                }

                // 팀 이름 (있는 경우)
                record.teamName?.let { teamName ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = teamName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "삭제",
                    tint = Color.Red,
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color.Gray,
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color.Gray,
        )
    }
}

/**
 * 주행 기록 상세 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RidingRecordDetailScreen(
    record: RidingRecordEntity,
    onBack: () -> Unit,
    onDelete: (RidingRecordEntity) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("주행 기록 상세") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로가기")
                    }
                },
                actions = {
                    IconButton(onClick = { onDelete(record) }) {
                        Icon(Icons.Default.Delete, "삭제", tint = Color.Red)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 날짜 및 시간
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Text(
                            text = formatDate(record.startTime),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "${formatTime(record.startTime)} - ${record.endTime?.let { formatTime(it) } ?: "진행중"}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        record.teamName?.let { teamName ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = teamName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }
            }

            // 통계
            item {
                Text(
                    text = "주행 통계",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        DetailStatRow("거리", String.format(java.util.Locale.getDefault(), "%.2f km", record.distanceMeters / 1000))
                        DetailStatRow("시간", formatDuration(record.durationMillis / 1000))
                        DetailStatRow("최고 속도", String.format(java.util.Locale.getDefault(), "%.1f km/h", record.maxSpeedKmh))
                        DetailStatRow("평균 속도", String.format(java.util.Locale.getDefault(), "%.1f km/h", record.averageSpeedKmh))
                        DetailStatRow("최대 기울기", String.format(java.util.Locale.getDefault(), "%.1f°", record.maxLeanAngleDegrees))
                        DetailStatRow("상승", String.format(java.util.Locale.getDefault(), "%.1f m", record.totalClimbMeters))
                        DetailStatRow("하강", String.format(java.util.Locale.getDefault(), "%.1f m", record.totalFallMeters))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailStatRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = Color.Gray,
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// Utility functions
private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREA)
    return sdf.format(Date(timestamp))
}

private fun formatDate(date: Date): String {
    val sdf = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA)
    return sdf.format(date)
}

private fun formatTime(date: Date): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.KOREA)
    return sdf.format(date)
}

private fun formatDateTime(date: Date): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)
    return sdf.format(date)
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        hours > 0 -> String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", hours, minutes, secs)
        else -> String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, secs)
    }
}
