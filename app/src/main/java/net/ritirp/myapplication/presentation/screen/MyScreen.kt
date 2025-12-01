package net.ritirp.myapplication.presentation.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ritirp.myapplication.GlobalApplication
import net.ritirp.myapplication.presentation.viewmodel.MyViewModel
import net.ritirp.myapplication.presentation.viewmodel.MyViewModelFactory

/**
 * MY 탭 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen(
    modifier: Modifier = Modifier,
    onNavigateToCrashSettings: () -> Unit,
    onNavigateToTeamManagement: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    val context = LocalContext.current
    val userRepository = GlobalApplication.getUserRepository(context)
    val authRepository = GlobalApplication.getAuthRepository(context)
    val viewModel: MyViewModel =
        viewModel(
            factory = MyViewModelFactory(userRepository, authRepository),
        )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 로그아웃 완료 시 로그인 화면으로 이동
    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            Log.d("MyScreen", "로그아웃 완료, 로그인 화면으로 이동")
            onLogout()
        }
    }

    LaunchedEffect(uiState.userProfile) {
        Log.d("MyScreen", "UI 상태 변경: name=${uiState.userProfile?.name}, id=${uiState.userProfile?.ribuddyId}, loading=${uiState.isLoading}")
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.White),
    ) {
        // 상단 앱바
        TopAppBar(
            title = {
                Text(
                    text = "MY",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                )
            },
            actions = {
                IconButton(onClick = { viewModel.logout() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "로그아웃",
                        tint = Color.Black,
                    )
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                ),
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color(0xFF4285F4))
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // 프로필 섹션
                ProfileSection(
                    name = uiState.userProfile?.nickname ?: uiState.userProfile?.name ?: "로딩 중...",
                    ribuddyId = uiState.userProfile?.ribuddyId ?: "",
                )

                // 통계 섹션
                StatsSection(
                    friendCount = uiState.friendCount,
                    teamCount = uiState.teamCount,
                    ridingRecordCount = uiState.ridingRecordCount,
                )

                // 설정 섹션
                SettingsSection(
                    onNavigateToCrashSettings = onNavigateToCrashSettings,
                )
            }
        }
    }
}

@Composable
private fun ProfileSection(
    name: String,
    ribuddyId: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 왼쪽: 프로필 카드 (파란색)
        Card(
            modifier = Modifier
                .width(220.dp)
                .height(140.dp),
            shape = RoundedCornerShape(16.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = Color(0xFFEEF3FF),
                ),
            elevation =
                CardDefaults.cardElevation(
                    defaultElevation = 4.dp,
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 아바타 (색상 반전)
                Box(
                    modifier =
                        Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.dp, Color(0xFF4285F4), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(60.dp),
                    )
                }

                // 이름과 아이디
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                    )
                    if (ribuddyId.isNotEmpty()) {
                        Text(
                            text = "@$ribuddyId",
                            fontSize = 14.sp,
                            color = Color(0xFF4285F4),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        // 오른쪽: 메뉴 버튼들 (빈 공간 가운데 배치)
        Column(
            modifier = Modifier.height(140.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.Start,
        ) {
            MenuButton(icon = Icons.Default.Notifications, label = "알림")
            MenuButton(icon = Icons.Default.Build, label = "수정")
            MenuButton(icon = Icons.Default.Info, label = "도움말")
        }
    }
}

@Composable
private fun MenuButton(
    icon: ImageVector,
    label: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF666666),
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF666666),
        )
    }
}

@Composable
private fun StatsSection(
    friendCount: Int = 0,
    teamCount: Int = 0,
    ridingRecordCount: Int = 0,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatItemCard(
            icon = Icons.Default.Person,
            count = friendCount.toString(),
            label = "버디",
            iconColor = Color(0xFF4285F4),
        )
        StatItemCard(
            icon = Icons.Default.Group,
            count = teamCount.toString(),
            label = "팀",
            iconColor = Color(0xFF4285F4),
        )
        StatItemCard(
            icon = Icons.AutoMirrored.Filled.DirectionsBike,
            count = ridingRecordCount.toString(),
            label = "주행기록",
            iconColor = Color(0xFF4285F4),
        )
    }
}

@Composable
private fun StatItemCard(
    icon: ImageVector,
    count: String,
    label: String,
    iconColor: Color,
) {
    Card(
        modifier =
            Modifier
                .width(100.dp)
                .height(110.dp),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = Color.White,
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 4.dp,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = Color(0xFF666666),
            )
        }
    }
}

@Composable
private fun SettingsSection(onNavigateToCrashSettings: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "설정",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
        )

        // 사고 감지 설정 버튼
        Card(
            onClick = onNavigateToCrashSettings,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(
                        width = 2.dp,
                        color = Color(0xFF4285F4),
                        shape = RoundedCornerShape(12.dp),
                    ),
            shape = RoundedCornerShape(12.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = Color.White,
                ),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(24.dp),
                    )
                    Column {
                        Text(
                            text = "사고 감지 설정",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                        )
                        Text(
                            text = "민감도 및 활성화 설정",
                            fontSize = 13.sp,
                            color = Color(0xFF888888),
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "설정 열기",
                    tint = Color(0xFF4285F4),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
