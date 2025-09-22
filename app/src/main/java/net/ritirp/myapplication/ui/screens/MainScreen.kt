package net.ritirp.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kakao.vectormap.LatLng

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onNavigationClick: (LatLng, LatLng) -> Unit = { _, _ -> }
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    var searchText by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize()) {
        // 지도 화면
        MapScreen(
            onNavigationClick = onNavigationClick,
            showFloatingButtons = false, // 메인페이지에서는 기본 플로팅 버튼 숨김
            modifier = Modifier.fillMaxSize()
        )

        // 상단 검색바
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(4.dp, RoundedCornerShape(25.dp)),
            shape = RoundedCornerShape(25.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "검색",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "어디로 어디로 갈까요?",
                    color = Color.Gray,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "사용자",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 우측 플로팅 버튼들
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 현재 위치 버튼
            FloatingActionButton(
                onClick = { /* 현재 위치로 이동 */ },
                modifier = Modifier.size(48.dp),
                containerColor = Color.White,
                contentColor = Color.Black
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "현재 위치",
                    modifier = Modifier.size(24.dp)
                )
            }

            // 레이어 버튼
            FloatingActionButton(
                onClick = { /* 레이어 설정 */ },
                modifier = Modifier.size(48.dp),
                containerColor = Color.White,
                contentColor = Color.Black
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "레이어",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 하단 네비게이션 바
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(8.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            BottomNavigationBar(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it }
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        NavigationTab("지도", Icons.Default.Place),
        NavigationTab("즐겨 찾은곳", Icons.Default.Favorite),
        NavigationTab("친구", Icons.Default.Person),
        NavigationTab("MY", Icons.Default.AccountCircle)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEachIndexed { index, tab ->
            BottomNavigationItem(
                tab = tab,
                isSelected = selectedTabIndex == index,
                onClick = { onTabSelected(index) }
            )
        }
    }
}

@Composable
fun BottomNavigationItem(
    tab: NavigationTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF2196F3).copy(alpha = 0.1f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.title,
            tint = if (isSelected) Color(0xFF2196F3) else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tab.title,
            color = if (isSelected) Color(0xFF2196F3) else Color.Gray,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

data class NavigationTab(
    val title: String,
    val icon: ImageVector
)
