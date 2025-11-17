package net.ritirp.myapplication.presentation.screen

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.ritirp.myapplication.data.model.FriendInfo
import net.ritirp.myapplication.presentation.viewmodel.FriendViewModel

/**
 * 버디 (친구) 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendScreen(
    viewModel: FriendViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showMyRibuddyIdDialog by remember { mutableStateOf(false) }

    // 에러/성공 스낵바
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

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "버디",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = { showMyRibuddyIdDialog = true }) {
                        Icon(Icons.Default.Badge, "내 ID")
                    }
                    IconButton(onClick = { viewModel.loadFriendList() }) {
                        Icon(Icons.Default.Refresh, "새로고침")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            // 검색창
            SearchBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onAddClick = { showAddDialog = true },
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // 즐겨찾기 섹션
                    if (uiState.filteredFavorites.isNotEmpty()) {
                        item {
                            SectionHeader("즐겨찾기")
                        }
                        items(uiState.filteredFavorites) { friend ->
                            FriendCard(
                                friend = friend,
                                onToggleFavorite = { viewModel.toggleFavorite(friend.userId, friend.isFavorite) },
                                onDelete = { viewModel.deleteFriend(friend.userId) },
                            )
                        }
                    }

                    // 팀 섹션
                    if (uiState.filteredFavorites.isNotEmpty() && uiState.filteredFriends.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (uiState.filteredFriends.isNotEmpty()) {
                        item {
                            SectionHeader("팀")
                        }
                        items(uiState.filteredFriends) { friend ->
                            FriendCard(
                                friend = friend,
                                onToggleFavorite = { viewModel.toggleFavorite(friend.userId, friend.isFavorite) },
                                onDelete = { viewModel.deleteFriend(friend.userId) },
                            )
                        }
                    }

                    // 빈 상태
                    if (uiState.filteredFavorites.isEmpty() && uiState.filteredFriends.isEmpty() && !uiState.isLoading) {
                        item {
                            EmptyFriendView(
                                onAddClick = { showAddDialog = true },
                            )
                        }
                    }
                }
            }
        }

        // 친구 추가 다이얼로그
        if (showAddDialog) {
            AddFriendDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { ribuddyId ->
                    viewModel.addFriend(ribuddyId)
                    showAddDialog = false
                },
            )
        }

        // 내 ID 다이얼로그
        if (showMyRibuddyIdDialog) {
            MyRibuddyIdDialog(
                onDismiss = { showMyRibuddyIdDialog = false },
                ribuddyId = uiState.myRibuddyId,
            )
        }
    }
}

/**
 * 검색바
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddClick: () -> Unit,
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("이름, 아이디 검색") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.PersonAdd, contentDescription = "친구 추가", tint = MaterialTheme.colorScheme.primary)
            }
        },
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
    )
}

/**
 * 섹션 헤더
 */
@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

/**
 * 친구 카드
 */
@Composable
fun FriendCard(
    friend: FriendInfo,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                // 프로필 아이콘
                Surface(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape),
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

                // 이름과 정보
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = friend.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    friend.nickname?.let { nickname ->
                        Text(
                            text = nickname,
                            fontSize = 12.sp,
                            color = Color.Gray,
                        )
                    }
                }
            }

            // 즐겨찾기 & 삭제 버튼
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (friend.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "즐겨찾기",
                        tint = if (friend.isFavorite) Color(0xFFFFD700) else Color.Gray,
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "삭제",
                        tint = Color.Red,
                    )
                }
            }
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("친구 삭제") },
            text = { Text("'${friend.name}' 님을 친구 목록에서 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                ) {
                    Text("삭제", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            },
        )
    }
}

/**
 * 빈 친구 목록 뷰
 */
@Composable
fun EmptyFriendView(onAddClick: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.PersonAdd,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.Gray,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "친구가 없습니다",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "라이버디 ID로 친구를 추가해보세요",
            fontSize = 14.sp,
            color = Color.Gray,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddClick) {
            Icon(Icons.Default.PersonAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("친구 추가")
        }
    }
}

/**
 * 친구 추가 다이얼로그
 */
@OptIn(ExperimentalMaterial3Api::class)
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
 * 내 라이버디 ID 다이얼로그
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRibuddyIdDialog(
    onDismiss: () -> Unit,
    ribuddyId: String?,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("내 라이버디 ID") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (ribuddyId != null) {
                    Text(
                        text = "내 라이버디 ID는 다음과 같습니다:",
                        fontSize = 14.sp,
                        color = Color.Gray,
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                    ) {
                        Text(
                            text = ribuddyId,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    Text(
                        text = "친구에게 이 ID를 공유하여 친구 추가를 요청하세요",
                        fontSize = 12.sp,
                        color = Color.Gray,
                    )
                } else {
                    Text(
                        text = "라이버디 ID를 불러올 수 없습니다",
                        fontSize = 14.sp,
                        color = Color.Gray,
                    )
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
