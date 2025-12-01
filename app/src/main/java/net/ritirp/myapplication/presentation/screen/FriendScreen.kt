package net.ritirp.myapplication.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import net.ritirp.myapplication.GlobalApplication
import net.ritirp.myapplication.R
import net.ritirp.myapplication.data.model.FriendInfo
import net.ritirp.myapplication.data.model.RidingStatus
import net.ritirp.myapplication.data.model.TeamInfo
import net.ritirp.myapplication.presentation.viewmodel.FriendViewModel

/**
 * 버디 탭 (친구/팀)
 */
enum class BuddyTab {
    FRIEND,
    TEAM,
}

/**
 * 버디 화면 - 친구와 팀 탭으로 구성
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendScreen(
    viewModel: FriendViewModel,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(BuddyTab.FRIEND) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showCreateTeamDialog by remember { mutableStateOf(false) }
    var showJoinTeamDialog by remember { mutableStateOf(false) }
    var selectedTeam by remember { mutableStateOf<TeamInfo?>(null) }

    val context = LocalContext.current
    val teamRepository = remember { GlobalApplication.getTeamRepository(context) }
    val drivingRepository = remember { GlobalApplication.getDrivingRepository(context) }
    val localRidingRecordRepository = remember { GlobalApplication.getLocalRidingRecordRepository(context) }
    val leanAngleSensorManager = remember { GlobalApplication.getLeanAngleSensorManager(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 라이딩 상태
    var ridingStatus by remember { mutableStateOf(RidingStatus.IDLE) }
    var currentRidingRecordId by remember { mutableStateOf<String?>(null) }
    var currentLocalRecordId by remember { mutableStateOf<Long?>(null) }
    var teamJoinCode by remember { mutableStateOf<String?>(null) }

    // 주행 중 데이터 수집
    LaunchedEffect(ridingStatus, currentLocalRecordId) {
        if (ridingStatus == RidingStatus.RIDING && currentLocalRecordId != null) {
            android.util.Log.d("FriendScreen", "주행 데이터 수집 시작")
        }
    }

    // 팀 상세 화면
    if (selectedTeam != null) {
        TeamDetailScreen(
            team = selectedTeam!!,
            joinCode = teamJoinCode,
            onBack = {
                selectedTeam = null
                teamJoinCode = null
            },
            onGetJoinCode = {
                scope.launch {
                    teamRepository.getTeamJoinCode(selectedTeam!!.id)
                        .onSuccess { code ->
                            teamJoinCode = code
                        }
                        .onFailure { error ->
                            scope.launch {
                                snackbarHostState.showSnackbar("참여 코드 조회 실패: ${error.message}")
                            }
                        }
                }
            },
            snackbarHostState = snackbarHostState,
            onLeaveTeam = {
                scope.launch {
                    selectedTeam?.let { team ->
                        teamRepository.leaveTeam(team.id)
                            .onSuccess {
                                selectedTeam = null
                                teamJoinCode = null
                                snackbarHostState.showSnackbar("'${team.name}' 팀에서 나갔습니다")
                            }
                            .onFailure { error ->
                                snackbarHostState.showSnackbar("팀 나가기 실패: ${error.message}")
                            }
                    }
                }
            },
            onStartRiding = { teamId ->
                scope.launch {
                    val lat = 37.5666102
                    val lon = 126.9783881

                    localRidingRecordRepository.startRiding(
                        teamId = teamId,
                        teamName = selectedTeam?.name,
                        startLat = lat,
                        startLon = lon,
                        startEle = null,
                        startLocationName = "시작 위치",
                    ).onSuccess { localRecordId ->
                        currentLocalRecordId = localRecordId
                        ridingStatus = RidingStatus.RIDING
                        leanAngleSensorManager.start()
                        snackbarHostState.showSnackbar("팀 라이딩이 시작되었습니다")

                        drivingRepository.startTeamRiding(teamId, lat, lon, null, "시작 위치")
                            .onSuccess { ridingRecordId ->
                                currentRidingRecordId = ridingRecordId
                            }
                    }.onFailure { error ->
                        snackbarHostState.showSnackbar("주행 시작 실패: ${error.message}")
                    }
                }
            },
            onEndRiding = {
                currentLocalRecordId?.let { localRecordId ->
                    scope.launch {
                        val lat = 37.5666102
                        val lon = 126.9783881

                        localRidingRecordRepository.endRiding(
                            recordId = localRecordId,
                            endLat = lat,
                            endLon = lon,
                            endEle = null,
                            endLocationName = "종료 위치",
                        ).onSuccess {
                            leanAngleSensorManager.stop()
                            currentLocalRecordId = null
                            ridingStatus = RidingStatus.IDLE
                            snackbarHostState.showSnackbar("팀 라이딩이 종료되었습니다")

                            currentRidingRecordId?.let { serverRecordId ->
                                drivingRepository.endTeamRiding(serverRecordId, lat, lon, null, "종료 위치")
                                    .onSuccess { currentRidingRecordId = null }
                            }
                        }.onFailure { error ->
                            snackbarHostState.showSnackbar("주행 종료 저장 실패: ${error.message}")
                        }
                    }
                }
            },
            ridingStatus = ridingStatus,
            teamMemberLocations = emptyList(),
            onNavigateToMap = { },
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "버디",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White,
            ),
        )

        CustomTabRow(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
        )

        SearchBar(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                BuddyTab.FRIEND -> {
                    FriendListContent(viewModel = viewModel)
                }
                BuddyTab.TEAM -> {
                    TeamListContent(
                        onTeamClick = { team ->
                            scope.launch {
                                teamRepository.getTeamInfo(team.id)
                                    .onSuccess { teamInfo ->
                                        selectedTeam = teamInfo
                                    }
                                    .onFailure { error ->
                                        snackbarHostState.showSnackbar("팀 정보 조회 실패: ${error.message}")
                                    }
                            }
                        },
                    )
                }
            }

            ExpandableFab(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                onAddFriendClick = { showAddFriendDialog = true },
                onCreateTeamClick = { showCreateTeamDialog = true },
                onJoinTeamClick = { showJoinTeamDialog = true },
            )
        }
    }

    // 다이얼로그
    if (showAddFriendDialog) {
        AddFriendDialog(
            onDismiss = { showAddFriendDialog = false },
            onConfirm = { ribuddyId ->
                viewModel.addFriend(ribuddyId)
                showAddFriendDialog = false
            },
        )
    }

    if (showCreateTeamDialog) {
        CreateTeamDialog(
            onDismiss = { showCreateTeamDialog = false },
            onConfirm = { teamName, teamDescription ->
                scope.launch {
                    teamRepository.createTeam(
                        name = teamName,
                        description = teamDescription,
                        members = emptyList(),
                        isCrew = false,
                    ).onSuccess {
                        showCreateTeamDialog = false
                        snackbarHostState.showSnackbar("팀이 생성되었습니다")
                    }.onFailure { error ->
                        snackbarHostState.showSnackbar("팀 생성 실패: ${error.message}")
                    }
                }
            },
        )
    }

    if (showJoinTeamDialog) {
        JoinTeamDialog(
            onDismiss = { showJoinTeamDialog = false },
            onConfirm = { teamCode: String ->
                scope.launch {
                    teamRepository.joinTeam(teamCode)
                        .onSuccess {
                            showJoinTeamDialog = false
                            snackbarHostState.showSnackbar("팀에 참여했습니다")
                            selectedTab = BuddyTab.TEAM
                        }
                        .onFailure { error ->
                            snackbarHostState.showSnackbar("팀 참여 실패: ${error.message}")
                        }
                }
            },
        )
    }

    SnackbarHost(hostState = snackbarHostState)
}

@Composable
fun CustomTabRow(
    selectedTab: BuddyTab,
    onTabSelected: (BuddyTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TabButton(
            text = "친구",
            isSelected = selectedTab == BuddyTab.FRIEND,
            onClick = { onTabSelected(BuddyTab.FRIEND) },
            modifier = Modifier.weight(1f),
        )
        TabButton(
            text = "팀",
            isSelected = selectedTab == BuddyTab.TEAM,
            onClick = { onTabSelected(BuddyTab.TEAM) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF4285F4) else Color(0xFFF5F5F5),
            contentColor = if (isSelected) Color.White else Color.Gray,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(modifier: Modifier = Modifier) {
    var searchText by remember { mutableStateOf("") }

    OutlinedTextField(
        value = searchText,
        onValueChange = { searchText = it },
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("이름, 아이디 검색", color = Color.Gray) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "검색",
                tint = Color.Gray,
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF4285F4),
            unfocusedBorderColor = Color(0xFFE0E0E0),
        ),
        singleLine = true,
    )
}

@Composable
fun FriendListContent(viewModel: FriendViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color(0xFF4285F4))
            }
        }
        uiState.friends.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray,
                    )
                    Text(
                        text = "친구가 없습니다",
                        fontSize = 16.sp,
                        color = Color.Gray,
                    )
                }
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.filteredFriends) { friend ->
                    FriendListItem(
                        friend = friend,
                        onToggleFavorite = {
                            viewModel.toggleFavorite(friend.userId, friend.isFavorite)
                        },
                        onDelete = {
                            viewModel.deleteFriend(friend.userId)
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun TeamListContent(onTeamClick: (TeamInfo) -> Unit) {
    val context = LocalContext.current
    val teamRepository = remember { GlobalApplication.getTeamRepository(context) }
    var teams by remember { mutableStateOf<List<TeamInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        teamRepository.getTeamList()
            .onSuccess { teamList ->
                teams = teamList
                isLoading = false
            }
            .onFailure {
                isLoading = false
            }
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color(0xFF4285F4))
            }
        }
        teams.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray,
                    )
                    Text(
                        text = "팀이 없습니다",
                        fontSize = 16.sp,
                        color = Color.Gray,
                    )
                }
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(teams) { team ->
                    TeamListItem(
                        team = team,
                        onClick = { onTeamClick(team) },
                    )
                }
            }
        }
    }
}

@Composable
fun FriendListItem(
    friend: FriendInfo,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_buddy),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.Unspecified,
            )
            Column {
                Text(
                    text = friend.nickname ?: friend.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                )
                friend.nickname?.let {
                    Text(
                        text = friend.name,
                        fontSize = 13.sp,
                        color = Color.Gray,
                    )
                }
            }
        }
    }
}

@Composable
fun TeamListItem(
    team: TeamInfo,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_group),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Color.Unspecified,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = team.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
            )
            Text(
                text = "${team.members?.size ?: 0}명",
                fontSize = 13.sp,
                color = Color.Gray,
            )
        }
    }
}

@Composable
fun ExpandableFab(
    modifier: Modifier = Modifier,
    onAddFriendClick: () -> Unit,
    onCreateTeamClick: () -> Unit,
    onJoinTeamClick: () -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "rotation",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 친구 추가
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            FabButton(
                label = "친구 추가",
                icon = Icons.Default.PersonAddAlt,
                onClick = {
                    onAddFriendClick()
                    isExpanded = false
                },
            )
        }

        // 팀 생성
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            FabButton(
                label = "팀 생성",
                icon = Icons.Default.Groups,
                onClick = {
                    onCreateTeamClick()
                    isExpanded = false
                },
            )
        }

        // 팀 참여
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            FabButton(
                label = "팀 참여",
                icon = Icons.Default.GroupAdd,
                onClick = {
                    onJoinTeamClick()
                    isExpanded = false
                },
            )
        }

        // 메인 FAB
        FloatingActionButton(
            onClick = { isExpanded = !isExpanded },
            containerColor = Color(0xFF4285F4),
            contentColor = Color.White,
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (isExpanded) "닫기" else "메뉴 열기",
                modifier = Modifier.rotate(rotationAngle),
            )
        }
    }
}

@Composable
private fun FabButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF37474F),
            shadowElevation = 4.dp,
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = Color(0xFF4285F4),
            contentColor = Color.White,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
            )
        }
    }
}

@Composable
fun AddFriendDialog(
    onDismiss: () -> Unit,
    onConfirm: (ribuddyId: String) -> Unit,
) {
    var ribuddyId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF8FBFF),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "친구 추가",
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
                Text(
                    text = "추가할 친구의 라이버디 ID를 입력하세요",
                    fontSize = 14.sp,
                    color = Color.Gray,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "라이버디 ID",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4285F4),
                    )
                    OutlinedTextField(
                        value = ribuddyId,
                        onValueChange = { ribuddyId = it },
                        placeholder = { Text("예: ribuddy_official", color = Color.Gray) },
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (ribuddyId.isNotBlank()) {
                        onConfirm(ribuddyId.trim())
                    }
                },
                enabled = ribuddyId.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4285F4),
                    disabledContainerColor = Color(0xFFE0E0E0),
                ),
            ) {
                Text(
                    text = "추가",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
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

@Composable
fun CreateTeamDialog(
    onDismiss: () -> Unit,
    onConfirm: (teamName: String, teamDescription: String) -> Unit,
) {
    var teamName by remember { mutableStateOf("") }
    var teamDescription by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                .background(Color(0xFFE8F0FE), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    OutlinedTextField(
                        value = teamDescription,
                        onValueChange = { teamDescription = it },
                        placeholder = { Text("팀 설명을 입력하세요", color = Color.Gray) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().height(100.dp),
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
                        onConfirm(teamName.trim(), teamDescription.trim())
                    }
                },
                enabled = teamName.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
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
                modifier = Modifier.fillMaxWidth().height(48.dp),
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
