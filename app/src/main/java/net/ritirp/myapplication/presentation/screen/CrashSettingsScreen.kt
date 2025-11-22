package net.ritirp.myapplication.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.ritirp.myapplication.data.model.SensitivityLevel
import net.ritirp.myapplication.presentation.viewmodel.CrashSettingsViewModel

private val CrashPrimary = Color(0xFF4285F4)
private val ActivationEnabled = Color(0xFFDBE8FF)
private val ActivationDisabled = Color(0xFFF3F6FF)
private val ThresholdBackground = Color(0xFFDBE8FF)
private val TipsBackground = Color(0xFFACC6F0)
private val SensitivitySelectedBackground = Color(0x144285F4)

/**
 * 사고 감지 설정 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashSettingsScreen(
    viewModel: CrashSettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("사고 감지 설정") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로가기", tint = Color(0xFF1F1F1F))
                    }
                },
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF1F1F1F),
                        navigationIconContentColor = Color(0xFF1F1F1F),
                    ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // 감지 활성화/비활성화 스위치
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            if (uiState.isDetectionEnabled) {
                                ActivationEnabled
                            } else {
                                ActivationDisabled
                            },
                    ),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "사고 감지 활성화",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text =
                                if (uiState.isDetectionEnabled) {
                                    "앱 사용 중 사고를 자동으로 감지합니다"
                                } else {
                                    "사고 감지 기능이 비활성화되었습니다"
                                },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = uiState.isDetectionEnabled,
                        onCheckedChange = { viewModel.toggleDetection(it) },
                        colors =
                            SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CrashPrimary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFCFD8DC),
                            ),
                    )
                }
            }

            // 민감도 설정
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = CrashPrimary,
                        )
                        Text(
                            text = "감지 민감도",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Text(
                        text = "높을수록 작은 충격도 감지하지만 오탐률이 증가할 수 있습니다",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Divider()

                    // 민감도 옵션들
                    SensitivityLevel.entries.forEach { level ->
                        SensitivityOption(
                            level = level,
                            isSelected = uiState.sensitivityLevel == level,
                            onSelect = { viewModel.setSensitivity(level) },
                            enabled = uiState.isDetectionEnabled,
                        )
                    }
                }
            }

            // 현재 설정 요약
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ThresholdBackground),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "📊 현재 임계값",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Divider()

                    val level = uiState.sensitivityLevel
                    ThresholdInfo("충격 강도", "${level.impactThreshold}g")
                    ThresholdInfo("회전 강도", "${level.rotationThreshold} rad/s")
                    ThresholdInfo("자유낙하 인식", "${level.freeFallThreshold}g 이하")
                }
            }

            // 도움말
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TipsBackground),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "ℹ️ 사용 팁",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text =
                            "• 라이딩 중에는 앱을 포어그라운드로 유지해주세요\n" +
                                "• 백그라운드(홈 화면, 다른 앱)에서는 감지하지 않습니다\n" +
                                "• 오탐이 자주 발생하면 민감도를 낮춰보세요\n" +
                                "• 감지되지 않으면 민감도를 높여보세요",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFF1E2A44),
                    )
                }
            }
        }
    }
}

@Composable
private fun SensitivityOption(
    level: SensitivityLevel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val (title, description) =
        when (level) {
            SensitivityLevel.LOW -> "낮음" to "강한 충격만 감지 (오탐 최소)"
            SensitivityLevel.MEDIUM -> "중간" to "일반적인 사고 감지 (권장)"
            SensitivityLevel.HIGH -> "높음" to "작은 충격도 감지 (민감)"
        }

    Surface(
        onClick = onSelect,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color =
            if (isSelected) {
                SensitivitySelectedBackground
            } else {
                Color.White
            },
        border =
            if (isSelected) {
                androidx.compose.foundation.BorderStroke(2.dp, CrashPrimary)
            } else {
                null
            },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                enabled = enabled,
                colors = RadioButtonDefaults.colors(selectedColor = CrashPrimary),
            )
        }
    }
}

@Composable
private fun ThresholdInfo(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF1E2A44),
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E2A44),
        )
    }
}
