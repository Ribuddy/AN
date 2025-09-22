package net.ritirp.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kakao.vectormap.LatLng

data class NavigationStep(
    val instruction: String,
    val distance: String,
    val duration: String,
    val type: String // "walk", "car", "subway" etc.
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(
    startLocation: LatLng,
    endLocation: LatLng,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var navigationSteps by remember { mutableStateOf<List<NavigationStep>>(emptyList()) }
    var totalDistance by remember { mutableStateOf("") }
    var totalDuration by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // 네비게이션 데이터 로드
    LaunchedEffect(startLocation, endLocation) {
        kotlinx.coroutines.delay(2000) // API 호출 시뮬레이션

        navigationSteps = listOf(
            NavigationStep(
                instruction = "출발지에서 동쪽으로 200m 직진",
                distance = "200m",
                duration = "3분",
                type = "walk"
            ),
            NavigationStep(
                instruction = "교차로에서 우회전",
                distance = "150m",
                duration = "2분",
                type = "walk"
            ),
            NavigationStep(
                instruction = "지하철 2호선 강남역 승차",
                distance = "5.2km",
                duration = "15분",
                type = "subway"
            ),
            NavigationStep(
                instruction = "역삼역에서 하차 후 3번 출구",
                distance = "100m",
                duration = "2분",
                type = "walk"
            ),
            NavigationStep(
                instruction = "목적지 도착",
                distance = "50m",
                duration = "1분",
                type = "walk"
            )
        )

        totalDistance = "5.7km"
        totalDuration = "약 23분"
        isLoading = false
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 상단 앱바
        TopAppBar(
            title = { Text("길찾기") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("경로를 찾는 중...")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 경로 요약 정보
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = totalDistance,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text("총 거리")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = totalDuration,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text("예상 시간")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 네비게이션 단계별 안내
                Text(
                    text = "상세 경로",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(navigationSteps.withIndex().toList()) { (index, step) ->
                        NavigationStepCard(
                            step = step,
                            stepNumber = index + 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationStepCard(
    step: NavigationStep,
    stepNumber: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 단계 번호
            Surface(
                modifier = Modifier.size(32.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 교통수단 아이콘
            Icon(
                imageVector = when (step.type) {
                    "car" -> Icons.Default.Place
                    "subway" -> Icons.Default.LocationOn
                    "walk" -> Icons.Default.Person
                    else -> Icons.Default.Person
                },
                contentDescription = step.type,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 안내 정보
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = step.instruction,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = step.distance,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = step.duration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
