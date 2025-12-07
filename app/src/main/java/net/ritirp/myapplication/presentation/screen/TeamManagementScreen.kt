package net.ritirp.myapplication.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import net.ritirp.myapplication.R
import net.ritirp.myapplication.data.model.TeamInfo
import net.ritirp.myapplication.presentation.viewmodel.TeamViewModel

/**
 * 팀 관리 메인 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamManagementScreen(
    viewModel: TeamViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    // 에러 스낵바
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
            // 성공 메시지가 있으면 다이얼로그 닫기
            showCreateDialog = false
            showJoinDialog = false
        }
    }

    // selectedTeam 변경 감지
    LaunchedEffect(uiState.selectedTeam) {
        android.util.Log.d("TeamManagementScreen", "selectedTeam 변경: ${uiState.selectedTeam?.name}")
    }

    // 선택된 팀이 있으면 팀 상세 화면 표시
    if (uiState.selectedTeam != null) {
        android.util.Log.d("TeamManagementScreen", "TeamDetailScreen 표시: ${uiState.selectedTeam!!.name}")
        TeamDetailScreen(
            team = uiState.selectedTeam!!,
            joinCode = uiState.teamJoinCode,
            onBack = { viewModel.clearSelectedTeam() },
            onGetJoinCode = { viewModel.getTeamJoinCode(uiState.selectedTeam!!.id) },
            snackbarHostState = snackbarHostState,
            onStartRiding = { teamId -> viewModel.startRiding(teamId) },
            onEndRiding = { viewModel.endRiding() },
            ridingStatus = uiState.ridingStatus,
            teamMemberLocations = uiState.teamMemberLocations,
            onNavigateToMap = onNavigateToMap,
            onLeaveTeam = { viewModel.leaveTeam(uiState.selectedTeam!!.id) },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("팀 관리") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadTeamList() }) {
                        Icon(Icons.Default.Refresh, "새로고침")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FloatingActionButton(
                    onClick = { showJoinDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondary,
                ) {
                    Icon(Icons.Default.GroupAdd, "팀 참여")
                }
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                ) {
                    Icon(Icons.Default.Add, "팀 생성")
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.teams.isEmpty() -> {
                    EmptyTeamListView(
                        onCreateTeam = { showCreateDialog = true },
                        onJoinTeam = { showJoinDialog = true },
                    )
                }
                else -> {
                    TeamListView(
                        teams = uiState.teams,
                        onTeamClick = { team ->
                            viewModel.getTeamInfo(team.id)
                        },
                        onLeaveTeam = { team ->
                            viewModel.leaveTeam(team.id)
                        },
                    )
                }
            }
        }

        // 팀 생성 다이얼로그
        if (showCreateDialog) {
            CreateTeamDialog(
                onDismiss = { showCreateDialog = false },
                onConfirm = { name, description ->
                    // 다이얼로그를 닫지 않고 API 호출만 실행
                    // successMessage가 오면 LaunchedEffect에서 자동으로 다이얼로그 닫힘
                    viewModel.createTeam(name, description)
                },
            )
        }

        // 팀 참여 다이얼로그
        if (showJoinDialog) {
            JoinTeamDialog(
                onDismiss = { showJoinDialog = false },
                onConfirm = { teamId ->
                    // 다이얼로그를 닫지 않고 API 호출만 실행
                    // successMessage가 오면 LaunchedEffect에서 자동으로 다이얼로그 닫힘
                    viewModel.joinTeam(teamId)
                },
            )
        }
    }
}

/**
 * 빈 팀 목록 뷰
 */
@Composable
fun EmptyTeamListView(
    onCreateTeam: () -> Unit,
    onJoinTeam: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Groups,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.Gray,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "참여 중인 팀이 없습니다",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "새로운 팀을 만들거나 기존 팀에 참여하세요",
            fontSize = 14.sp,
            color = Color.Gray,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onCreateTeam) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("팀 생성")
            }
            OutlinedButton(onClick = onJoinTeam) {
                Icon(Icons.Default.GroupAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("팀 참여")
            }
        }
    }
}

/**
 * 팀 목록 뷰
 */
@Composable
fun TeamListView(
    teams: List<TeamInfo>,
    onTeamClick: (TeamInfo) -> Unit,
    onLeaveTeam: (TeamInfo) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(teams) { team ->
            TeamCard(
                team = team,
                onClick = { onTeamClick(team) },
                onLeave = { onLeaveTeam(team) },
            )
        }
    }
}

/**
 * 팀 카드
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamCard(
    team: TeamInfo,
    onClick: () -> Unit,
    onLeave: () -> Unit,
) {
    var showLeaveDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = if (team.isCrew) Icons.Default.Star else Icons.Default.Groups,
                        contentDescription = null,
                        tint = if (team.isCrew) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = team.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                team.description?.let { desc ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = desc,
                        fontSize = 14.sp,
                        color = Color.Gray,
                    )
                }
                team.members?.let { members ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "${members.size}명",
                            fontSize = 12.sp,
                            color = Color.Gray,
                        )
                    }
                }
            }
            IconButton(onClick = { showLeaveDialog = true }) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "팀 탈퇴",
                    tint = Color.Red,
                )
            }
        }
    }

    // 팀 탈퇴 확인 다이얼로그
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("팀 탈퇴") },
            text = { Text("정말 '${team.name}' 팀에서 탈퇴하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLeave()
                        showLeaveDialog = false
                    },
                ) {
                    Text("탈퇴", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("취소")
                }
            },
        )
    }
}

/**
 * 팀 생성 다이얼로그
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTeamDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String?) -> Unit,
) {
    var teamName by remember { mutableStateOf("") }
    var teamDescription by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF8FBFF),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "새 팀 만들기",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 팀 이름 입력
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "팀 이름",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4285F4),
                    )
                    OutlinedTextField(
                        value = teamName,
                        onValueChange = { teamName = it },
                        placeholder = { Text("팀 이름을 입력하세요", color = Color.Gray) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4285F4),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = Color(0xFFF8F9FA),
                            unfocusedContainerColor = Color(0xFFF8F9FA),
                        ),
                    )
                }

                // 팀 설명 입력
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "팀 설명 (선택사항)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF4285F4),
                        )
                        Text(
                            text = "선택",
                            fontSize = 12.sp,
                            color = Color(0xFF4285F4),
                            modifier = Modifier
                                .background(
                                    Color(0xFFE8F0FE),
                                    RoundedCornerShape(4.dp),
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    OutlinedTextField(
                        value = teamDescription,
                        onValueChange = { teamDescription = it },
                        placeholder = { Text("팀 설명을 입력하세요", color = Color.Gray) },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4285F4),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = Color(0xFFF8F9FA),
                            unfocusedContainerColor = Color(0xFFF8F9FA),
                        ),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (teamName.isNotBlank()) {
                        onConfirm(
                            teamName.trim(),
                            teamDescription.trim().ifBlank { null },
                        )
                    }
                },
                enabled = teamName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4285F4),
                    disabledContainerColor = Color(0xFFE0E0E0),
                ),
            ) {
                Text(
                    text = "생성",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(
                    text = "취소",
                    fontSize = 16.sp,
                    color = Color.Gray,
                )
            }
        },
    )
}

/**
 * 팀 참여 다이얼로그
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinTeamDialog(
    onDismiss: () -> Unit,
    onConfirm: (teamId: String) -> Unit,
) {
    var teamId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "팀 ID로 참여하기",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                )
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        text = "취소",
                        fontSize = 14.sp,
                        color = Color.Gray,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = teamId,
                    onValueChange = { teamId = it },
                    placeholder = { Text("아이디 검색", color = Color.Gray, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE0E0E0),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                    ),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (teamId.isNotBlank()) {
                            onConfirm(teamId.trim())
                        }
                    },
                    enabled = teamId.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4285F4),
                        disabledContainerColor = Color(0xFFE0E0E0),
                    ),
                ) {
                    Text(
                        text = "검색",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {},
    )
}

/**
 * 팀 상세 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailScreen(
    team: TeamInfo,
    joinCode: String?,
    onBack: () -> Unit,
    onGetJoinCode: () -> Unit,
    onStartRiding: (teamId: String) -> Unit,
    onEndRiding: () -> Unit,
    ridingStatus: net.ritirp.myapplication.data.model.RidingStatus,
    teamMemberLocations: List<net.ritirp.myapplication.data.model.TeamMemberLocation>,
    snackbarHostState: SnackbarHostState,
    onNavigateToMap: () -> Unit = {},
    onLeaveTeam: () -> Unit = {},
) {
    var showJoinCodeDialog by remember { mutableStateOf(false) }
    var showInviteFriendDialog by remember { mutableStateOf(false) }
    var showLeaveTeamDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = team.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기")
                    }
                },
                actions = {
                    IconButton(onClick = { showLeaveTeamDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "팀 탈퇴",
                            tint = Color(0xFFEF5350),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {

            FloatingActionButton(
                onClick = {
                    showInviteFriendDialog = true
                },
                containerColor = Color(0xFF4285F4),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "팀원 초대",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = Color(0xFFF5F5F5),
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 팀 ID 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFEFF3F8),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        text = "팀 ID",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = team.id,
                        fontSize = 16.sp,
                        color = Color.Gray,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 팀 설명 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFEFF3F8),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        text = "팀 설명",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = team.description ?: "설명이 없습니다",
                        fontSize = 16.sp,
                        color = Color.Gray,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 팀원 목록 헤더
            Text(
                text = "팀원 (${team.members?.size ?: 0}명)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 팀원 목록
            if (team.members.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "팀원이 없습니다",
                        fontSize = 14.sp,
                        color = Color.Gray,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(team.members) { member ->
                        TeamMemberItem(member = member)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 라이딩 시작 버튼 (항상 표시, 클릭 시 지도 화면으로 이동)
            Button(
                onClick = {
                    onStartRiding(team.id)
                    onNavigateToMap()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF34A853),
                ),
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "팀 라이딩 시작",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 팀 참여 코드 다이얼로그
        if (showJoinCodeDialog) {
            TeamJoinCodeDialog(
                teamName = team.name,
                joinCode = joinCode,
                onDismiss = { showJoinCodeDialog = false },
            )
        }

        // 친구 초대 다이얼로그
        if (showInviteFriendDialog) {
            InviteFriendToTeamDialog(
                teamId = team.id,
                teamName = team.name,
                onDismiss = { showInviteFriendDialog = false },
            )
        }

        // 팀 탈퇴 확인 다이얼로그
        if (showLeaveTeamDialog) {
            AlertDialog(
                onDismissRequest = { showLeaveTeamDialog = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        text = "팀 탈퇴",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = {
                    Text(
                        text = "정말 '${team.name}' 팀에서 탈퇴하시겠습니까?",
                        fontSize = 16.sp,
                        color = Color(0xFF666666),
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onLeaveTeam()
                            showLeaveTeamDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF5350),
                        ),
                    ) {
                        Text("탈퇴", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLeaveTeamDialog = false }) {
                        Text("취소", color = Color(0xFF666666))
                    }
                },
            )
        }
    }
}

/**
 * 팀원 아이템 (간단한 버전)
 */
@Composable
fun TeamMemberItem(member: net.ritirp.myapplication.data.model.TeamMember) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 프로필 아이콘 (ic_buddy.xml 사용)
        Icon(
            painter = painterResource(id = R.drawable.ic_buddy),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = Color.Unspecified, // 원본 색상 유지
        )

        // 이름
        Text(
            text = member.name ?: member.nickname ?: "이름 없음",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
        )
    }
}

/**
 * 팀원 카드
 */
@Composable
fun TeamMemberCard(member: net.ritirp.myapplication.data.model.TeamMember) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 프로필 아이콘
            Surface(
                modifier = Modifier.size(40.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            // 이름과 닉네임
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name ?: "이름 없음",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
                member.nickname?.let { nickname ->
                    Text(
                        text = nickname,
                        fontSize = 12.sp,
                        color = Color.Gray,
                    )
                }
            }
        }
    }
}

/**
 * 팀 참여 코드 다이얼로그
 */
@Composable
fun TeamJoinCodeDialog(
    teamName: String,
    joinCode: String?,
    onDismiss: () -> Unit,
) {
    var showTimeout by remember { mutableStateOf(false) }

    // 5초 후에도 코드가 안 오면 타임아웃 메시지 표시
    LaunchedEffect(joinCode) {
        if (joinCode == null) {
            kotlinx.coroutines.delay(5000)
            if (joinCode == null) {
                showTimeout = true
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "팀원 초대",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "'$teamName' 팀에 친구를 초대하세요",
                    fontSize = 14.sp,
                    color = Color.Gray,
                )

                when {
                    joinCode != null -> {
                        // 참여 코드가 있을 때
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
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
                                        text = "팀 참여 코드",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    Text(
                                        text = joinCode,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "복사",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                        Text(
                            text = "이 코드를 친구에게 공유하면 팀에 참여할 수 있습니다",
                            fontSize = 12.sp,
                            color = Color.Gray,
                        )
                    }
                    showTimeout -> {
                        // 타임아웃 시
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(48.dp),
                            )
                            Text(
                                text = "참여 코드를 불러올 수 없습니다",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "팀 ID를 직접 공유하거나\n나중에 다시 시도해주세요",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                    else -> {
                        // 로딩 중
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인")
            }
        },
    )
}

/**
 * 팀 라이딩 지도 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamRidingMapScreen(
    teamName: String,
    teamMemberLocations: List<net.ritirp.myapplication.data.model.TeamMemberLocation>,
    ridingMetrics: net.ritirp.myapplication.data.model.RidingMetrics? = null,
    onBack: () -> Unit,
    onEndRiding: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    var showEndRidingDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$teamName 팀 라이딩") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            // 지도 영역 (임시로 배경색만 설정)
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFFECEFF1)),
            ) {
                // 실제 지도는 여기서 표시
                // 현재는 팀원 위치를 표시하는 마커만 임시로 추가
                teamMemberLocations.forEach { memberLocation ->
                    // TODO: 실제 지도 위에 마커로 표시
                    Text(
                        text = "${memberLocation.memberName} (${memberLocation.distance}km)",
                        modifier = Modifier.padding(8.dp),
                    )
                }

                // 주행 통계 바를 상단에 오버레이
                ridingMetrics?.let { metrics ->
                    net.ritirp.myapplication.presentation.components.RidingMetricsBar(
                        metrics = metrics,
                        modifier =
                            Modifier
                                .align(Alignment.TopCenter)
                                .padding(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 팀원 위치 목록
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(teamMemberLocations) { memberLocation ->
                    TeamMemberLocationCard(memberLocation)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 라이딩 종료 버튼
            Button(
                onClick = { showEndRidingDialog = true },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF5350),
                    ),
            ) {
                Text(
                    text = "라이딩 종료",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }

    // 라이딩 종료 확인 다이얼로그
    if (showEndRidingDialog) {
        AlertDialog(
            onDismissRequest = { showEndRidingDialog = false },
            title = { Text("라이딩 종료") },
            text = { Text("정말로 라이딩을 종료하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEndRiding()
                        showEndRidingDialog = false
                    },
                ) {
                    Text("종료", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndRidingDialog = false }) {
                    Text("취소")
                }
            },
        )
    }
}

/**
 * 팀원 위치 카드
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamMemberLocationCard(memberLocation: net.ritirp.myapplication.data.model.TeamMemberLocation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 프로필 아이콘
            Surface(
                modifier = Modifier.size(40.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            // 이름, 거리 및 상태
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = memberLocation.memberName ?: "이름 없음",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${memberLocation.distance} km",
                    fontSize = 14.sp,
                    color = Color.Gray,
                )
            }

            // 라이딩 중 상태 아이콘
            if (memberLocation.isRiding) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                    contentDescription = "라이딩 중",
                    tint = Color(0xFF4CAF50),
                )
            }
        }
    }
}

/**
 * 팀원 추가 다이얼로그 (친구 목록에서 선택)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteFriendToTeamDialog(
    teamId: String,
    teamName: String,
    onDismiss: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val friendRepository = remember { net.ritirp.myapplication.GlobalApplication.getFriendRepository(context) }
    val scope = rememberCoroutineScope()

    var friends by remember { mutableStateOf<List<net.ritirp.myapplication.data.model.FriendInfo>>(emptyList()) }
    var selectedFriends by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // 친구 목록 로드
    LaunchedEffect(Unit) {
        friendRepository.getFriendList()
            .onSuccess { friendList ->
                friends = friendList.friends
                isLoading = false
            }
            .onFailure {
                isLoading = false
            }
    }

    // 검색 필터링
    val filteredFriends = remember(friends, searchQuery) {
        if (searchQuery.isEmpty()) {
            friends
        } else {
            friends.filter { friend ->
                friend.name.contains(searchQuery, ignoreCase = true) ||
                friend.nickname?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "팀원 추가",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 검색창
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("이름, 아이디 검색", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4285F4),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedContainerColor = Color(0xFFF8F9FA),
                        unfocusedContainerColor = Color(0xFFF8F9FA),
                    ),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "검색",
                            tint = Color.Gray,
                        )
                    },
                )

                HorizontalDivider(color = Color(0xFFE0E0E0))

                Text(
                    text = "친구",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Color(0xFF4285F4))
                    }
                } else if (filteredFriends.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (searchQuery.isEmpty()) "친구가 없습니다" else "검색 결과가 없습니다",
                            fontSize = 14.sp,
                            color = Color.Gray,
                        )
                    }
                } else {
                    // 친구 목록
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filteredFriends) { friend ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedFriends = if (selectedFriends.contains(friend.userId)) {
                                            selectedFriends - friend.userId
                                        } else {
                                            selectedFriends + friend.userId
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                // 프로필 아이콘
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_buddy),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = Color.Unspecified,
                                )

                                // 이름
                                Text(
                                    text = friend.nickname ?: friend.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black,
                                    modifier = Modifier.weight(1f),
                                )

                                // 체크박스
                                RadioButton(
                                    selected = selectedFriends.contains(friend.userId),
                                    onClick = {
                                        selectedFriends = if (selectedFriends.contains(friend.userId)) {
                                            selectedFriends - friend.userId
                                        } else {
                                            selectedFriends + friend.userId
                                        }
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF4285F4),
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        // TODO: 선택한 친구들을 팀에 초대하는 API 호출
                        // 현재는 팀 ID와 선택된 친구 ID들만 로그 출력
                        android.util.Log.d("InviteFriend", "팀 ID: $teamId, 선택된 친구: ${selectedFriends.joinToString()}")
                        onDismiss()
                    }
                },
                enabled = selectedFriends.isNotEmpty() && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4285F4),
                    disabledContainerColor = Color(0xFFE0E0E0),
                ),
            ) {
                Text(
                    text = "팀원 초대하기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(
                    text = "취소",
                    fontSize = 16.sp,
                    color = Color.Gray,
                )
            }
        },
    )
}
