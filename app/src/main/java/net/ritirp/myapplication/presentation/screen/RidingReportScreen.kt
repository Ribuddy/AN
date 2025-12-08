
package net.ritirp.myapplication.presentation.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.ritirp.myapplication.R
import net.ritirp.myapplication.data.local.entity.RidingRecordEntity
import net.ritirp.myapplication.data.model.ImprovementCategory
import net.ritirp.myapplication.data.model.ImprovementPoint
import net.ritirp.myapplication.data.model.PeriodFilter
import net.ritirp.myapplication.data.model.RidingScore
import net.ritirp.myapplication.data.model.RidingSummary
import net.ritirp.myapplication.data.model.ScoreFilter
import net.ritirp.myapplication.presentation.viewmodel.RidingReportViewModel
import java.util.Calendar

// 색상 정의
private val PrimaryBlue = Color(0xFF4A90D9)
private val ChartBlue = Color(0xFF5B9BD5)
private val ScoreBlue = Color(0xFF4A90D9)
private val TagBlue = Color(0xFFE3F2FD)
private val TagTextBlue = Color(0xFF1976D2)

/**
 * 주행 리포트 메인 화면 - 요약 화면
 */
@Composable
fun RidingReportScreen(
    viewModel: RidingReportViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showRecordList by remember { mutableStateOf(false) }
    var selectedRecordId by remember { mutableStateOf<Long?>(null) }
    val records by viewModel.records.collectAsStateWithLifecycle()

    // 상세 화면이 선택되면 상세 화면 표시
    if (selectedRecordId != null) {
        val selectedRecord = records.find { it.id == selectedRecordId }
        if (selectedRecord != null) {
            RidingReportDetailScreen(
                record = selectedRecord,
                viewModel = viewModel,
                onBack = { selectedRecordId = null },
                modifier = modifier,
            )
            return
        }
    }

    // 리스트 화면이 선택되면 리스트 표시
    if (showRecordList) {
        RidingRecordListScreen(
            records = records,
            onRecordClick = { selectedRecordId = it },
            onBack = { showRecordList = false },
            modifier = modifier,
        )
        return
    }

    // 메인 요약 화면
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState()),
    ) {
        // 상단 타이틀
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "주행 리포트",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
            )
        }

        // 배너 섹션 (오토바이 라이더 이미지 + 메시지)
        BannerSection()

        Spacer(modifier = Modifier.height(16.dp))

        // 내 주행 섹션
        MyRidingSection(
            summary = uiState.ridingSummary,
            selectedFilter = uiState.periodFilter,
            onFilterSelected = { viewModel.setPeriodFilter(it) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 최근 주행 기록 섹션
        RecentRidingRecordsSection(
            records = records.take(3),
            onSeeAllClick = { showRecordList = true },
            onRecordClick = { selectedRecordId = it },
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * 주행 기록 리스트 아이템
 */
@Composable
private fun RidingRecordListItem(
    record: RidingRecordEntity,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // 날짜 및 시간
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatDateTime(record.startTime.time),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.Gray,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 주행 통계 요약
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                RecordStatItem(
                    icon = "📏",
                    label = "거리",
                    value = String.format("%.2f km", record.distanceMeters / 1000.0),
                )
                RecordStatItem(
                    icon = "⏱️",
                    label = "시간",
                    value = formatDuration(record.durationMillis / 60000),
                )
                RecordStatItem(
                    icon = "🏍️",
                    label = "최고속도",
                    value = String.format("%.0f km/h", record.maxSpeedKmh),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 추가 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                RecordStatItem(
                    icon = "📐",
                    label = "최대기울기",
                    value = String.format("%.1f°", record.maxLeanAngleDegrees),
                )
                RecordStatItem(
                    icon = "⛰️",
                    label = "상승",
                    value = String.format("%.0f m", record.totalClimbMeters),
                )
                RecordStatItem(
                    icon = "⬇️",
                    label = "하강",
                    value = String.format("%.0f m", record.totalFallMeters),
                )
            }
        }
    }
}

/**
 * 기록 통계 아이템
 */
@Composable
private fun RecordStatItem(
    icon: String,
    label: String,
    value: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray,
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
        )
    }
}

/**
 * 주행 기록 리스트 화면
 */
@Composable
private fun RidingRecordListScreen(
    records: List<RidingRecordEntity>,
    onRecordClick: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        // 상단 타이틀 (뒤로가기 버튼 포함)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Text(
                    text = "←",
                    fontSize = 24.sp,
                    color = Color.Black,
                )
            }
            Text(
                text = "주행 기록",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (records.isEmpty()) {
            // 빈 상태
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "🏍️",
                        fontSize = 64.sp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "아직 주행 기록이 없습니다",
                        fontSize = 16.sp,
                        color = Color.Gray,
                    )
                }
            }
        } else {
            // 주행 기록 리스트
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                records.forEach { record ->
                    RidingRecordListItem(
                        record = record,
                        onClick = { onRecordClick(record.id) },
                    )
                }
            }
        }
    }
}

/**
 * 최근 주행 기록 섹션
 */
@Composable
private fun RecentRidingRecordsSection(
    records: List<RidingRecordEntity>,
    onSeeAllClick: () -> Unit,
    onRecordClick: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
    ) {
        // 섹션 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "최근 주행 기록",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
            )
            TextButton(onClick = onSeeAllClick) {
                Text(
                    text = "See All >",
                    fontSize = 14.sp,
                    color = PrimaryBlue,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (records.isEmpty()) {
            // 빈 상태
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "주행 기록이 없습니다",
                    fontSize = 14.sp,
                    color = Color.Gray,
                )
            }
        } else {
            // 최근 기록 리스트 (최대 3개)
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                records.forEach { record ->
                    RecentRecordItem(
                        record = record,
                        onClick = { onRecordClick(record.id) },
                    )
                }
            }
        }
    }
}

/**
 * 최근 기록 아이템 (간단한 버전)
 */
@Composable
private fun RecentRecordItem(
    record: RidingRecordEntity,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = formatDate(record.startTime.time),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTime(record.startTime.time),
                    fontSize = 12.sp,
                    color = Color.Gray,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray,
            )
        }
    }
}

/**
 * 주행 리포트 상세 화면
 */
@Composable
private fun RidingReportDetailScreen(
    record: RidingRecordEntity,
    viewModel: RidingReportViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        // 상단 타이틀 (뒤로가기 버튼 포함)
        DetailTopAppBar(
            onBack = onBack,
            title = formatDateTime(record.startTime.time),
        )

        // 스크롤 가능한 콘텐츠
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // 배너 섹션 (오토바이 라이더 이미지 + 메시지)
            BannerSection()

            Spacer(modifier = Modifier.height(24.dp))

            // 내 주행 섹션
            MyRidingSection(
                summary = uiState.ridingSummary,
                selectedFilter = uiState.periodFilter,
                onFilterSelected = { viewModel.setPeriodFilter(it) },
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 주행 점수 섹션
            RidingScoreSection(
                score = uiState.ridingScore,
                selectedFilter = uiState.scoreFilter,
                onFilterSelected = { viewModel.setScoreFilter(it) },
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 개선 포인트 섹션
            ImprovementSection(
                improvements = uiState.improvementPoints,
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 상세 화면 상단 앱바 (뒤로가기 버튼 포함)
 */
@Composable
private fun DetailTopAppBar(
    onBack: () -> Unit,
    title: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Text(
                text = "←",
                fontSize = 24.sp,
                color = Color.White,
            )
        }
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/**
 * 날짜/시간 포맷팅
 */
private fun formatDateTime(timeMillis: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMillis
    return String.format(
        "%04d-%02d-%02d %02d:%02d",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH),
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
    )
}

/**
 * 날짜 포맷팅
 */
private fun formatDate(timeMillis: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMillis
    return String.format(
        "%04d년 %02d월 %02d일",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH),
    )
}

/**
 * 시간 포맷팅
 */
private fun formatTime(timeMillis: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMillis
    return String.format(
        "%02d:%02d",
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
    )
}

@Composable
private fun BannerSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
    ) {
        // 배경 이미지
        Image(
            painter = painterResource(id = R.drawable.background_riding_report),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // 메시지 말풍선 (선택적으로 표시)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
        ) {
            // "오늘도 안전 운전 하세요!" 텍스트는 PNG 이미지에 포함되어 있으므로 제거
        }
    }
}

@Composable
private fun MyRidingSection(
    summary: RidingSummary,
    selectedFilter: PeriodFilter,
    onFilterSelected: (PeriodFilter) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        // 섹션 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "내 주행",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                // 기간 필터 탭
                PeriodFilterTabs(
                    selectedFilter = selectedFilter,
                    onFilterSelected = onFilterSelected,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 거리/시간 요약
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    SummaryItem(
                        icon = "🏍️",
                        value = String.format("%.0f", summary.totalDistanceKm) + "km",
                        color = PrimaryBlue,
                    )
                    SummaryItem(
                        icon = "⏱️",
                        value = formatDuration(summary.totalDurationMinutes),
                        color = Color.Gray,
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 바 차트
                WeeklyBarChart(dailyDistances = summary.dailyDistances)
            }
        }
    }
}

@Composable
private fun PeriodFilterTabs(
    selectedFilter: PeriodFilter,
    onFilterSelected: (PeriodFilter) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        PeriodFilter.entries.forEachIndexed { index, filter ->
            val isSelected = filter == selectedFilter
            val isFirst = index == 0
            val isLast = index == PeriodFilter.entries.size - 1

            Box(
                modifier = Modifier
                    .clip(
                        when {
                            isFirst -> RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                            isLast -> RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                            else -> RoundedCornerShape(0.dp)
                        }
                    )
                    .background(
                        if (isSelected) Color.Black else Color(0xFFF5F5F5),
                    )
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    text = when (filter) {
                        PeriodFilter.WEEK -> "Week"
                        PeriodFilter.MONTH -> "Month"
                        PeriodFilter.YEAR -> "Year"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color.White else Color.Gray,
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    icon: String,
    value: String,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = icon, fontSize = 20.sp)
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun WeeklyBarChart(
    dailyDistances: List<net.ritirp.myapplication.data.model.DailyDistance>,
) {
    val maxDistance = dailyDistances.maxOfOrNull { it.distanceKm }?.coerceAtLeast(1.0) ?: 300.0
    val chartHeight = 120.dp

    Column {
        // Y축 라벨과 차트
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Y축 라벨
            Column(
                modifier = Modifier.height(chartHeight),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "300", fontSize = 10.sp, color = Color.Gray)
                Text(text = "200", fontSize = 10.sp, color = Color.Gray)
                Text(text = "100", fontSize = 10.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 바 차트
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(chartHeight),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                dailyDistances.forEach { daily ->
                    val barHeight = if (maxDistance > 0) {
                        (daily.distanceKm / 300.0 * chartHeight.value).coerceAtLeast(4.0)
                    } else {
                        4.0
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(barHeight.dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(ChartBlue),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // X축 라벨
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            dailyDistances.forEach { daily ->
                Text(
                    text = daily.dayOfWeek,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.width(24.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun RidingScoreSection(
    score: RidingScore,
    selectedFilter: ScoreFilter,
    onFilterSelected: (ScoreFilter) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        // 섹션 헤더
        Text(
            text = "주행 점수",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 점수 필터 탭
                ScoreFilterTabs(
                    selectedFilter = selectedFilter,
                    onFilterSelected = onFilterSelected,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 원형 점수 게이지
                CircularScoreGauge(
                    score = score.totalScore,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 세부 점수 리스트
                ScoreDetailItem(
                    label = "조작 안전",
                    score = score.operationSafetyScore,
                )
                Spacer(modifier = Modifier.height(12.dp))
                ScoreDetailItem(
                    label = "속도 안전",
                    score = score.speedSafetyScore,
                )
                Spacer(modifier = Modifier.height(12.dp))
                ScoreDetailItem(
                    label = "기울기 안정성",
                    score = score.leanStabilityScore,
                )
            }
        }
    }
}

@Composable
private fun ScoreFilterTabs(
    selectedFilter: ScoreFilter,
    onFilterSelected: (ScoreFilter) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScoreFilter.entries.forEach { filter ->
            val isSelected = filter == selectedFilter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) Color.Black else Color(0xFFF5F5F5),
                    )
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = when (filter) {
                        ScoreFilter.WEEK -> "Week"
                        ScoreFilter.TOTAL -> "Total"
                        ScoreFilter.MONTH -> "Month"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color.White else Color.Gray,
                )
            }
        }
    }
}

@Composable
private fun CircularScoreGauge(
    score: Int,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(180.dp),
    ) {
        Canvas(
            modifier = Modifier.size(180.dp),
        ) {
            val strokeWidth = 16.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            // 배경 원
            drawCircle(
                color = Color(0xFFE0E0E0),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // 점수에 따른 호
            val sweepAngle = (score / 100f) * 360f
            drawArc(
                color = ScoreBlue,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }

        // 점수 텍스트
        Text(
            text = "${score}점",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreBlue,
        )
    }
}

@Composable
private fun ScoreDetailItem(
    label: String,
    score: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 아이콘
        Icon(
            imageVector = Icons.Default.Navigation,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier
                .size(20.dp)
                .padding(end = 4.dp),
        )

        // 라벨
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.width(90.dp),
        )

        // 진행 바
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE0E0E0)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(score / 100f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF90CAF9),
                                PrimaryBlue,
                            ),
                        ),
                    ),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 점수
        Text(
            text = "${score}점",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
        )
    }
}

@Composable
private fun ImprovementSection(
    improvements: List<ImprovementPoint>,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        // 섹션 헤더
        Text(
            text = "개선 포인트",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 개선 포인트 카드들
        improvements.forEach { improvement ->
            ImprovementCard(improvement = improvement)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ImprovementCard(
    improvement: ImprovementPoint,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // 카테고리 태그
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(TagBlue)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = when (improvement.category) {
                        ImprovementCategory.OPERATION -> "조작"
                        ImprovementCategory.SPEED -> "속도"
                        ImprovementCategory.LEAN_STABILITY -> "기울기 안정성"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TagTextBlue,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 제목
            Text(
                text = improvement.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 설명
            Text(
                text = improvement.description,
                fontSize = 14.sp,
                color = Color.Gray,
            )
        }
    }
}

/**
 * 시간 포맷팅 (분 -> 시간h 분m)
 */
private fun formatDuration(minutes: Long): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return "${hours}h ${mins}m"
}
