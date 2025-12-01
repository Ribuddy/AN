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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(BuddyTab.FRIEND) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showCreateTeamDialog by remember { mutableStateOf(false) }
    var selectedTeam by remember { mutableStateOf<net.ritirp.myapplication.data.model.TeamInfo?>(null) }

    val context = LocalContext.current
    val teamRepository = remember { GlobalApplication.getTeamRepository(context) }
    val drivingRepository = remember { GlobalApplication.getDrivingRepository(context) }
    val localRidingRecordRepository = remember { GlobalApplication.getLocalRidingRecordRepository(context) }
    val leanAngleSensorManager = remember { GlobalApplication.getLeanAngleSensorManager(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 센서에서 기울기 각도 수집
    val currentLeanAngle by leanAngleSensorManager.leanAngle.collectAsStateWithLifecycle()

    // 라이딩 상태
    var ridingStatus by remember { mutableStateOf(net.ritirp.myapplication.data.model.RidingStatus.IDLE) }
    var currentRidingRecordId by remember { mutableStateOf<String?>(null) }
    var currentLocalRecordId by remember { mutableStateOf<Long?>(null) } // 로컬 DB의 record ID
    var teamJoinCode by remember { mutableStateOf<String?>(null) }

    // 주행 중 데이터 수집 (5초마다)
    LaunchedEffect(ridingStatus, currentLocalRecordId) {
        if (ridingStatus == net.ritirp.myapplication.data.model.RidingStatus.RIDING && currentLocalRecordId != null) {
            // TODO: 실제 GPS 위치 추적은 LocationUpdateManager나 FusedLocationProviderClient 사용
            // 지금은 테스트를 위해 시작 위치만 저장하고 주행 종료 시 통계 확인
            android.util.Log.d("FriendScreen", "주행 데이터 수집 시작 (실제 GPS 연동 필요)")
        }
    }

    // 팀 상세 화면이 표시되어야 하는 경우
    if (selectedTeam != null) {
        android.util.Log.d("FriendScreen", "TeamDetailScreen 표시: ${selectedTeam!!.name}")
        TeamDetailScreen(
            team = selectedTeam!!,
            joinCode = teamJoinCode,
            onBack = {
                android.util.Log.d("FriendScreen", "팀 상세 화면에서 뒤로가기")
                selectedTeam = null
                teamJoinCode = null
            },
            onGetJoinCode = {
                scope.launch {
                    teamRepository.getTeamJoinCode(selectedTeam!!.id)
                        .onSuccess { code ->
                            android.util.Log.d("FriendScreen", "참여 코드 조회 성공: $code")
                            teamJoinCode = code
                        }
                        .onFailure { error ->
                            android.util.Log.e("FriendScreen", "참여 코드 조회 실패: ${error.message}")
                            scope.launch {
                                snackbarHostState.showSnackbar("참여 코드 조회 실패: ${error.message}")
                            }
                        }
                }
            },
            snackbarHostState = snackbarHostState,
            onLeaveTeam = {
                scope.launch {
                    val teamToLeave = selectedTeam
                    teamToLeave?.let { team ->
                        android.util.Log.d("FriendScreen", "팀 나가기 시도: ${team.name}, id=${team.id}")
                        teamRepository.leaveTeam(team.id).onSuccess {
                            android.util.Log.d("FriendScreen", "팀 나가기 성공: ${team.name}")
                            // 팀 상세 화면 닫기
                            selectedTeam = null
                            teamJoinCode = null
                            snackbarHostState.showSnackbar("'${team.name}' 팀에서 나갔습니다")
                        }.onFailure { error ->
                            android.util.Log.e("FriendScreen", "팀 나가기 실패: ${error.message}")
                            snackbarHostState.showSnackbar("팀 나가기 실패: ${error.message}")
                        }
                    }
                }
            },
            onStartRiding = { teamId ->
                android.util.Log.d("FriendScreen", "팀 라이딩 시작: teamId=$teamId")
                scope.launch {
                    // 현재 위치 가져오기 (실제로는 GPS에서 가져와야 함)
                    val lat = 37.5666102
                    val lon = 126.9783881

                    // 1. 로컬 DB에 주행 기록 저장
                    localRidingRecordRepository.startRiding(
                        teamId = teamId,
                        teamName = selectedTeam?.name,
                        startLat = lat,
                        startLon = lon,
                        startEle = null,
                        startLocationName = "시작 위치",
                    ).onSuccess { localRecordId ->
                        android.util.Log.d("FriendScreen", "로컬 DB에 주행 시작 저장 성공: localId=$localRecordId")
                        currentLocalRecordId = localRecordId
                        ridingStatus = net.ritirp.myapplication.data.model.RidingStatus.RIDING

                        // 센서 시작
                        leanAngleSensorManager.start()
                        android.util.Log.d("FriendScreen", "기울기 센서 시작")

                        snackbarHostState.showSnackbar("팀 라이딩이 시작되었습니다. (로컬 저장)")

                        // 2. 서버에도 전송 시도 (선택적)
                        drivingRepository.startTeamRiding(teamId, lat, lon, null, "시작 위치")
                            .onSuccess { ridingRecordId ->
                                android.util.Log.d("FriendScreen", "서버에 팀 라이딩 시작 전송 성공: $ridingRecordId")
                                currentRidingRecordId = ridingRecordId
                            }
                            .onFailure { error ->
                                android.util.Log.e("FriendScreen", "서버 전송 실패 (로컬 저장만 됨): ${error.message}")
                            }
                    }.onFailure { error ->
                        android.util.Log.e("FriendScreen", "로컬 DB 저장 실패: ${error.message}")
                        snackbarHostState.showSnackbar("주행 시작 실패: ${error.message}")
                    }
                }
            },
            onEndRiding = {
                android.util.Log.d("FriendScreen", "팀 라이딩 종료")
                currentLocalRecordId?.let { localRecordId ->
                    scope.launch {
                        // 현재 위치 가져오기
                        val lat = 37.5666102
                        val lon = 126.9783881

                        // 1. 로컬 DB에 주행 종료 저장
                        localRidingRecordRepository.endRiding(
                            recordId = localRecordId,
                            endLat = lat,
                            endLon = lon,
                            endEle = null,
                            endLocationName = "종료 위치",
                        ).onSuccess {
                            android.util.Log.d("FriendScreen", "로컬 DB에 주행 종료 저장 성공")

                            // 센서 중지
                            leanAngleSensorManager.stop()
                            android.util.Log.d("FriendScreen", "기울기 센서 중지")

                            // 저장된 데이터 확인
                            val savedRecord = localRidingRecordRepository.getRecordById(localRecordId)
                            savedRecord?.let { record ->
                                val durationSeconds = record.durationMillis / 1000.0
                                val distanceKm = record.distanceMeters / 1000.0

                                android.util.Log.d(
                                    "FriendScreen",
                                    """
                                    ========== 저장된 주행 기록 ==========
                                    ID: ${record.id}
                                    팀: ${record.teamName} (${record.teamId})
                                    시작: ${record.startTime}
                                    종료: ${record.endTime}
                                    
                                    📊 주행 통계:
                                    - Distance (km): ${"%.2f".format(distanceKm)} km
                                    - Duration (s): ${"%.0f".format(durationSeconds)} s
                                    - Top Speed (km/h): ${"%.1f".format(record.maxSpeedKmh)} km/h
                                    - Average Speed (km/h): ${"%.1f".format(record.averageSpeedKmh)} km/h
                                    
                                    ⛰️ 고도 정보:
                                    - Climb (m): ${"%.1f".format(record.totalClimbMeters)} m
                                    - Fall (m): ${"%.1f".format(record.totalFallMeters)} m
                                    - Max Elevation: ${record.maxElevation?.let { "%.1f".format(it) } ?: "N/A"} m
                                    - Min Elevation: ${record.minElevation?.let { "%.1f".format(it) } ?: "N/A"} m
                                    
                                    🏍️ 기울기 정보:
                                    - Max Lean Angle (°): ${"%.1f".format(record.maxLeanAngleDegrees)}°
                                    - Avg Lean Angle (°): ${"%.1f".format(record.avgLeanAngleDegrees)}°
                                    
                                    📍 경로:
                                    - 경로 포인트: ${record.routePoints.size}개
                                    - 시작: ${record.startLocationName ?: "N/A"}
                                    - 종료: ${record.endLocationName ?: "N/A"}
                                    =====================================
                                    """.trimIndent(),
                                )
                            }

                            currentLocalRecordId = null
                            ridingStatus = net.ritirp.myapplication.data.model.RidingStatus.IDLE
                            snackbarHostState.showSnackbar("팀 라이딩이 종료되었습니다. (로컬 저장)")

                            // 2. 서버에도 전송 시도 (선택적)
                            currentRidingRecordId?.let { serverRecordId ->
                                drivingRepository.endTeamRiding(serverRecordId, lat, lon, null, "종료 위치")
                                    .onSuccess {
                                        android.util.Log.d("FriendScreen", "서버에 팀 라이딩 종료 전송 성공")
                                        currentRidingRecordId = null
                                    }
                                    .onFailure { error ->
                                        android.util.Log.e("FriendScreen", "서버 전송 실패 (로컬 저장만 됨): ${error.message}")
                                    }
                            }
                        }.onFailure { error ->
                            android.util.Log.e("FriendScreen", "로컬 DB 저장 실패: ${error.message}")
                            snackbarHostState.showSnackbar("주행 종료 저장 실패: ${error.message}")
                        }
                    }
                }
            },
            ridingStatus = ridingStatus,
            teamMemberLocations = emptyList(),
            onNavigateToMap = { /* TODO: 지도 화면으로 이동 */ },
        )
        return
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
                    text = "버디",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                )
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                ),
        )

        // 친구/팀 탭
        CustomTabRow(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
        )

        // 검색창
        SearchBar(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        // 컨텐츠
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                BuddyTab.FRIEND -> {
                    FriendListContent(
                        viewModel = viewModel,
                        onAddClick = { showAddFriendDialog = true },
                    )
                }
                BuddyTab.TEAM -> {
                    TeamListContent(
                        onAddClick = { },
                        onTeamClick = { team ->
                            android.util.Log.d("FriendScreen", "팀 선택됨: ${team.name}, id=${team.id}")
                            scope.launch {
                                teamRepository.getTeamInfo(team.id)
                                    .onSuccess { teamInfo ->
                                        android.util.Log.d("FriendScreen", "팀 정보 조회 성공, 상세 화면 표시")
                                        selectedTeam = teamInfo
                                    }
                                    .onFailure { error ->
                                        android.util.Log.e("FriendScreen", "팀 정보 조회 실패: ${error.message}")
                                    }
                            }
                        },
                    )
                }
            }

            // 확장 가능한 FAB
            ExpandableFab(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                onAddFriendClick = { showAddFriendDialog = true },
                onCreateTeamClick = { showCreateTeamDialog = true },
                selectedTab = selectedTab,
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
                        // TODO: 팀 목록 새로고침
                    }.onFailure {
                        // TODO: 에러 처리
                    }
                }
            },
        )
    }

}

/**
 * 커스텀 탭 로우 (친구/팀)
 */
@Composable
fun CustomTabRow(
    selectedTab: BuddyTab,
    onTabSelected: (BuddyTab) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 친구 탭
        TabButton(
            text = "친구",
            isSelected = selectedTab == BuddyTab.FRIEND,
            onClick = { onTabSelected(BuddyTab.FRIEND) },
            modifier = Modifier.weight(1f),
        )

        // 팀 탭
        TabButton(
            text = "팀",
            isSelected = selectedTab == BuddyTab.TEAM,
            onClick = { onTabSelected(BuddyTab.TEAM) },
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 탭 버튼
 */
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
        colors =
            ButtonDefaults.buttonColors(
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

/**
 * 검색바 (간단한 버전)
 */
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
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4285F4),
                unfocusedBorderColor = Color(0xFFE0E0E0),
            ),
        singleLine = true,
    )
}

/**
 * 친구 리스트 컨텐츠
 */
@Composable
fun FriendListContent(
    viewModel: FriendViewModel,
    onAddClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = Color(0xFF4285F4))
        }
    } else if (uiState.friends.isEmpty()) {
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
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(uiState.filteredFriends) { friend ->
                FriendListItem(
                    friend = friend,
                    onToggleFavorite = { viewModel.toggleFavorite(friend.userId, friend.isFavorite) },
                    onDelete = { viewModel.deleteFriend(friend.userId) },
                )
            }
        }
    }
}

/**
 * 팀 리스트 컨텐츠
 */
@Composable
fun TeamListContent(
    onAddClick: () -> Unit,
    onTeamClick: (net.ritirp.myapplication.data.model.TeamInfo) -> Unit,
) {
    val context = LocalContext.current
    val teamRepository = remember { GlobalApplication.getTeamRepository(context) }
    var teams by remember { mutableStateOf<List<net.ritirp.myapplication.data.model.TeamInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 팀 목록 로드
    LaunchedEffect(Unit) {
        isLoading = true
        teamRepository.getTeamList()
            .onSuccess { teamList ->
                teams = teamList
                isLoading = false
            }
            .onFailure {
                isLoading = false
            }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = Color(0xFF4285F4))
        }
    } else if (teams.isEmpty()) {
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
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(teams) { team ->
                TeamListItem(
                    team = team,
                    onClick = {
                        android.util.Log.d("FriendScreen", "팀 클릭: ${team.name}, id=${team.id}")
                        onTeamClick(team)
                    },
                )
            }
        }
    }
}

/**
 * 친구 리스트 아이템
 */
@Composable
fun FriendListItem(
    friend: FriendInfo,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier =
            Modifier
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
            // 프로필 아이콘
            Icon(
                painter = painterResource(id = R.drawable.ic_buddy),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.Unspecified,
            )

            // 이름
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

/**
 * 팀 리스트 아이템
 */
@Composable
fun TeamListItem(
    team: net.ritirp.myapplication.data.model.TeamInfo,
    onClick: () -> Unit = {},
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            // 팀 아이콘
            Icon(
                painter = painterResource(id = R.drawable.ic_group),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.Unspecified,
            )

            // 팀 정보
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
}

/**
 * 확장 가능한 FAB 컴포넌트
 */
@Composable
fun ExpandableFab(
    modifier: Modifier = Modifier,
    onAddFriendClick: () -> Unit,
    onCreateTeamClick: () -> Unit,
    selectedTab: BuddyTab,
) {
    var isExpanded by remember { mutableStateOf(false) }

    // 회전 애니메이션
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec =
            spring(
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
        // 서브 버튼 1 - 팀원 초대
        AnimatedVisibility(
            visible = isExpanded,
            enter =
                fadeIn(
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                ) +
                    expandVertically(
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                    ),
            exit =
                fadeOut(
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                ) +
                    shrinkVertically(
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                    ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 라벨
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF37474F),
                    shadowElevation = 4.dp,
                ) {
                    Text(
                        text = "친구 추가",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }

                SmallFloatingActionButton(
                    onClick = {
                        onAddFriendClick()
                        isExpanded = false
                    },
                    containerColor = Color(0xFF4285F4),
                    contentColor = Color.White,
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAddAlt,
                        contentDescription = "친구 추가",
                    )
                }
            }
        }

        // 서브 버튼 2 - 팀 생성
        AnimatedVisibility(
            visible = isExpanded,
            enter =
                fadeIn(
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                ) +
                    expandVertically(
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                    ),
            exit =
                fadeOut(
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                ) +
                    shrinkVertically(
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                    ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 라벨
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF37474F),
                    shadowElevation = 4.dp,
                ) {
                    Text(
                        text = "팀 생성",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }

                SmallFloatingActionButton(
                    onClick = {
                        onCreateTeamClick()
                        isExpanded = false
                    },
                    containerColor = Color(0xFF4285F4),
                    contentColor = Color.White,
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = "팀 생성",
                    )
                }
            }
        }


        // 메인 FAB (토글 버튼)
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

/**
 * 친구 추가 다이얼로그
 */
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

                // 라이버디 ID 입력
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
 * 팀 생성 다이얼로그
 */
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
                        onConfirm(teamName.trim(), teamDescription.trim())
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
