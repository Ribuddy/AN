package net.ritirp.myapplication.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        }
    }

    // 선택된 팀이 있으면 팀 상세 화면 표시
    if (uiState.selectedTeam != null) {
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
                    viewModel.createTeam(name, description)
                    showCreateDialog = false
                },
            )
        }

        // 팀 참여 다이얼로그
        if (showJoinDialog) {
            JoinTeamDialog(
                onDismiss = { showJoinDialog = false },
                onConfirm = { teamId ->
                    viewModel.joinTeam(teamId)
                    showJoinDialog = false
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
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
    var isCrew by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 팀 만들기") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("팀 이름") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = teamDescription,
                    onValueChange = { teamDescription = it },
                    label = { Text("팀 설명 (선택사항)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = isCrew,
                        onCheckedChange = { isCrew = it },
                    )
                    Text("크루로 설정")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (teamName.isNotBlank()) {
                        onConfirm(
                            teamName.trim(),
                            teamDescription.trim().ifBlank { null },
                        )
                    }
                },
                enabled = teamName.isNotBlank(),
            ) {
                Text("생성")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
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
        title = { Text("팀 참여하기") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "참여할 팀의 ID를 입력하세요",
                    fontSize = 14.sp,
                    color = Color.Gray,
                )
                OutlinedTextField(
                    value = teamId,
                    onValueChange = { teamId = it },
                    label = { Text("팀 ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (teamId.isNotBlank()) {
                        onConfirm(teamId.trim())
                    }
                },
                enabled = teamId.isNotBlank(),
            ) {
                Text("참여")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
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
) {
    var showJoinCodeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(team.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showJoinCodeDialog = true
                    onGetJoinCode()
                },
            ) {
                Icon(Icons.Default.Share, "팀원 초대")
            }
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            // 팀 정보 카드
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = if (team.isCrew) Icons.Default.Star else Icons.Default.Groups,
                            contentDescription = null,
                            tint = if (team.isCrew) Color(0xFFFFD700) else MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = team.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    team.description?.let { desc ->
                        Text(
                            text = desc,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            // 팀 라이딩 시작/종료 버튼
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            when (ridingStatus) {
                                net.ritirp.myapplication.data.model.RidingStatus.RIDING -> Color(0xFF4CAF50)
                                else -> MaterialTheme.colorScheme.primary
                            },
                    ),
            ) {
                Button(
                    onClick = {
                        when (ridingStatus) {
                            net.ritirp.myapplication.data.model.RidingStatus.IDLE,
                            net.ritirp.myapplication.data.model.RidingStatus.ENDED,
                            -> {
                                onStartRiding(team.id)
                                // 라이딩 시작하면 메인 화면(지도)으로 이동
                                onNavigateToMap()
                            }
                            net.ritirp.myapplication.data.model.RidingStatus.RIDING -> {
                                onEndRiding()
                            }
                            else -> {}
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                when (ridingStatus) {
                                    net.ritirp.myapplication.data.model.RidingStatus.RIDING -> Color(0xFFEF5350)
                                    else -> MaterialTheme.colorScheme.primary
                                },
                        ),
                ) {
                    Icon(
                        imageVector =
                            when (ridingStatus) {
                                net.ritirp.myapplication.data.model.RidingStatus.RIDING -> Icons.Default.Stop
                                else -> Icons.Default.DirectionsBike
                            },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text =
                            when (ridingStatus) {
                                net.ritirp.myapplication.data.model.RidingStatus.RIDING -> "팀 라이딩 종료"
                                else -> "팀 라이딩 시작"
                            },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // 팀원 목록 헤더
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "팀원 (${team.members?.size ?: 0}명)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // 팀원 목록
            if (team.members.isNullOrEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "팀원이 없습니다",
                        color = Color.Gray,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(team.members) { member ->
                        TeamMemberCard(member = member)
                    }
                }
            }
        }

        // 팀 참여 코드 다이얼로그
        if (showJoinCodeDialog) {
            TeamJoinCodeDialog(
                teamName = team.name,
                joinCode = joinCode,
                onDismiss = { showJoinCodeDialog = false },
            )
        }
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
                        modifier = Modifier
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
                    imageVector = Icons.Default.DirectionsBike,
                    contentDescription = "라이딩 중",
                    tint = Color(0xFF4CAF50),
                )
            }
        }
    }
}
