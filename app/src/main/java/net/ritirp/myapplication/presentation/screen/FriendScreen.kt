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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    FRIEND, TEAM
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
    var showLeaveTeamDialog by remember { mutableStateOf(false) }
    var selectedTeamToLeave by remember { mutableStateOf<net.ritirp.myapplication.data.model.TeamInfo?>(null) }

    val context = LocalContext.current
    val teamRepository = remember { GlobalApplication.getTeamRepository(context) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
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
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )

        // 친구/팀 탭
        CustomTabRow(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        // 검색창
        SearchBar(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // 컨텐츠
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                BuddyTab.FRIEND -> {
                    FriendListContent(
                        viewModel = viewModel,
                        onAddClick = { showAddFriendDialog = true }
                    )
                }
                BuddyTab.TEAM -> {
                    TeamListContent(
                        onAddClick = { }
                    )
                }
            }

            // 확장 가능한 FAB
            ExpandableFab(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                onAddFriendClick = { showAddFriendDialog = true },
                onCreateTeamClick = { showCreateTeamDialog = true },
                onLeaveTeamClick = { showLeaveTeamDialog = true },
                selectedTab = selectedTab
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
            }
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
                        isCrew = false
                    ).onSuccess {
                        showCreateTeamDialog = false
                        // TODO: 팀 목록 새로고침
                    }.onFailure {
                        // TODO: 에러 처리
                    }
                }
            }
        )
    }

    if (showLeaveTeamDialog) {
        LeaveTeamDialog(
            onDismiss = {
                showLeaveTeamDialog = false
                selectedTeamToLeave = null
            },
            teamName = selectedTeamToLeave?.name ?: "",
            onConfirm = {
                selectedTeamToLeave?.let { team ->
                    scope.launch {
                        teamRepository.leaveTeam(team.id).onSuccess {
                            showLeaveTeamDialog = false
                            selectedTeamToLeave = null
                            // TODO: 팀 목록 새로고침
                        }.onFailure {
                            // TODO: 에러 처리
                        }
                    }
                }
            }
        )
    }
}

/**
 * 커스텀 탭 로우 (친구/팀)
 */
@Composable
fun CustomTabRow(
    selectedTab: BuddyTab,
    onTabSelected: (BuddyTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 친구 탭
        TabButton(
            text = "친구",
            isSelected = selectedTab == BuddyTab.FRIEND,
            onClick = { onTabSelected(BuddyTab.FRIEND) },
            modifier = Modifier.weight(1f)
        )

        // 팀 탭
        TabButton(
            text = "팀",
            isSelected = selectedTab == BuddyTab.TEAM,
            onClick = { onTabSelected(BuddyTab.TEAM) },
            modifier = Modifier.weight(1f)
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
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF4285F4) else Color(0xFFF5F5F5),
            contentColor = if (isSelected) Color.White else Color.Gray
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
                tint = Color.Gray
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF4285F4),
            unfocusedBorderColor = Color(0xFFE0E0E0)
        ),
        singleLine = true
    )
}

/**
 * 친구 리스트 컨텐츠
 */
@Composable
fun FriendListContent(
    viewModel: FriendViewModel,
    onAddClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF4285F4))
        }
    } else if (uiState.friends.isEmpty()) {
        // 빈 상태
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
                Text(
                    text = "친구가 없습니다",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.filteredFriends) { friend ->
                FriendListItem(
                    friend = friend,
                    onToggleFavorite = { viewModel.toggleFavorite(friend.userId, friend.isFavorite) },
                    onDelete = { viewModel.deleteFriend(friend.userId) }
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
    onAddClick: () -> Unit
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
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF4285F4))
        }
    } else if (teams.isEmpty()) {
        // 빈 상태
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
                Text(
                    text = "팀이 없습니다",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(teams) { team ->
                TeamListItem(team = team)
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
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            // 프로필 아이콘
            Icon(
                painter = painterResource(id = R.drawable.ic_buddy),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.Unspecified
            )

            // 이름
            Column {
                Text(
                    text = friend.nickname ?: friend.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                friend.nickname?.let {
                    Text(
                        text = friend.name,
                        fontSize = 13.sp,
                        color = Color.Gray
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
fun TeamListItem(team: net.ritirp.myapplication.data.model.TeamInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            // 팀 아이콘
            Icon(
                painter = painterResource(id = R.drawable.ic_group),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.Unspecified
            )

            // 팀 정보
            Column {
                Text(
                    text = team.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = "${team.members?.size ?: 0}명",
                    fontSize = 13.sp,
                    color = Color.Gray
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
    onLeaveTeamClick: () -> Unit,
    selectedTab: BuddyTab
) {
    var isExpanded by remember { mutableStateOf(false) }

    // 회전 애니메이션
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotation"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 서브 버튼 1 - 팀원 초대
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = fadeOut(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + shrinkVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 라벨
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF37474F),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = "친구 추가",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                SmallFloatingActionButton(
                    onClick = {
                        onAddFriendClick()
                        isExpanded = false
                    },
                    containerColor = Color(0xFF4285F4),
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAddAlt,
                        contentDescription = "친구 추가"
                    )
                }
            }
        }

        // 서브 버튼 2 - 팀 생성
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = fadeOut(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + shrinkVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 라벨
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF37474F),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = "팀 생성",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                SmallFloatingActionButton(
                    onClick = {
                        onCreateTeamClick()
                        isExpanded = false
                    },
                    containerColor = Color(0xFF4285F4),
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = "팀 생성"
                    )
                }
            }
        }

        // 서브 버튼 3 - 팀 나가기
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = fadeOut(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + shrinkVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 라벨
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFD32F2F),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = "팀 나가기",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                SmallFloatingActionButton(
                    onClick = {
                        onLeaveTeamClick()
                        isExpanded = false
                    },
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "팀 나가기"
                    )
                }
            }
        }

        // 메인 FAB (토글 버튼)
        FloatingActionButton(
            onClick = { isExpanded = !isExpanded },
            containerColor = Color(0xFF4285F4),
            contentColor = Color.White,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (isExpanded) "닫기" else "메뉴 열기",
                modifier = Modifier.rotate(rotationAngle)
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
        title = { Text("친구 추가") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "추가할 친구의 라이버디 ID를 입력하세요",
                    fontSize = 14.sp,
                    color = Color.Gray,
                )
                OutlinedTextField(
                    value = ribuddyId,
                    onValueChange = { ribuddyId = it },
                    label = { Text("라이버디 ID") },
                    placeholder = { Text("예: ribuddy_official") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (ribuddyId.isNotBlank()) {
                        onConfirm(ribuddyId.trim())
                    }
                },
                enabled = ribuddyId.isNotBlank(),
            ) {
                Text("추가")
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
        title = { Text("팀 생성") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("팀 이름") },
                    placeholder = { Text("예: 라이버디") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = teamDescription,
                    onValueChange = { teamDescription = it },
                    label = { Text("팀 설명 (선택)") },
                    placeholder = { Text("예: 함께 라이딩해요!") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (teamName.isNotBlank()) {
                        onConfirm(teamName.trim(), teamDescription.trim())
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
 * 팀 나가기 다이얼로그
 */
@Composable
fun LeaveTeamDialog(
    onDismiss: () -> Unit,
    teamName: String,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("팀 나가기") },
        text = {
            Text(
                text = "'$teamName' 팀에서 나가시겠습니까?\n나가면 다시 참여하려면 초대코드가 필요합니다.",
                fontSize = 14.sp,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFFD32F2F)
                )
            ) {
                Text("나가기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
    )
}
