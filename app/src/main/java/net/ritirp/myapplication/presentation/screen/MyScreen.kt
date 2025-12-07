package net.ritirp.myapplication.presentation.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import net.ritirp.myapplication.R
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
    var showEditDialog by remember { mutableStateOf(false) }

    // 화면이 다시 표시될 때(RESUME) 자동으로 데이터 새로고침
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Log.d("MyScreen", "🔄 ON_RESUME 이벤트 감지 - 프로필 새로고침 시작")
                viewModel.refreshProfile()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            Log.d("MyScreen", "🗑️ LifecycleObserver 해제")
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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

    // 라이버디 ID 변경 성공 시 다이얼로그 닫기
    LaunchedEffect(uiState.updateSuccess) {
        if (uiState.updateSuccess) {
            showEditDialog = false
            viewModel.clearUpdateStatus()
        }
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
                    onEditClick = { showEditDialog = true },
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

        // 라이버디 ID 변경 다이얼로그
        if (showEditDialog) {
            EditRibuddyIdDialog(
                currentRibuddyId = uiState.userProfile?.ribuddyId ?: "",
                isLoading = uiState.isUpdatingRibuddyId,
                error = uiState.updateError,
                onDismiss = {
                    showEditDialog = false
                    viewModel.clearUpdateStatus()
                },
                onConfirm = { newId ->
                    viewModel.updateRibuddyId(newId)
                }
            )
        }
    }
}

@Composable
private fun ProfileSection(
    name: String,
    ribuddyId: String,
    onEditClick: () -> Unit = {},
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


        // 오른쪽: 메뉴 버튼들 (빈 공간 가운데 배치)
        Column(
            modifier = Modifier.height(140.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.Start,
        ) {
            MenuButton(iconRes = R.drawable.ic_bell, label = "알림", onClick = {})
            MenuButton(iconRes = R.drawable.ic_modify, label = "수정", onClick = onEditClick)
            MenuButton(iconRes = R.drawable.ic_help, label = "도움말", onClick = {})
        }
    }
}

@Composable
private fun MenuButton(
    iconRes: Int,
    label: String,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = Color(0xFF666666),
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            fontSize = 16.sp,
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
        horizontalArrangement = Arrangement.SpaceBetween,
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

/**
 * 라이버디 ID 변경 다이얼로그
 */
@Composable
private fun EditRibuddyIdDialog(
    currentRibuddyId: String,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var newRibuddyId by remember { mutableStateOf(currentRibuddyId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "라이버디 ID 변경",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "새로운 라이버디 ID를 입력하세요",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                )

                OutlinedTextField(
                    value = newRibuddyId,
                    onValueChange = { newRibuddyId = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    placeholder = { Text("예: ribuddy_user") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4285F4),
                        unfocusedBorderColor = Color(0xFFCCCCCC),
                    ),
                )

                if (error != null) {
                    Text(
                        text = error,
                        color = Color.Red,
                        fontSize = 12.sp,
                    )
                }

                if (isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF4285F4),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newRibuddyId.isNotBlank() && newRibuddyId != currentRibuddyId) {
                        onConfirm(newRibuddyId)
                    }
                },
                enabled = !isLoading && newRibuddyId.isNotBlank() && newRibuddyId != currentRibuddyId,
            ) {
                Text(
                    text = "변경",
                    color = if (!isLoading && newRibuddyId.isNotBlank() && newRibuddyId != currentRibuddyId) {
                        Color(0xFF4285F4)
                    } else {
                        Color(0xFFCCCCCC)
                    },
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
            ) {
                Text(
                    text = "취소",
                    color = if (!isLoading) Color(0xFF666666) else Color(0xFFCCCCCC),
                )
            }
        },
        containerColor = Color.White,
    )
}
