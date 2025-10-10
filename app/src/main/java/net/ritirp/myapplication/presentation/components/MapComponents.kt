package net.ritirp.myapplication.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ritirp.myapplication.R
import net.ritirp.myapplication.presentation.viewmodel.BottomTab

/**
 * 상단 검색바와 친구 버튼
 */
@Composable
fun TopSearchBar(
    onFriendClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .weight(1f)
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                "오늘은 어디를 달릴까요?",
                fontSize = 15.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            onClick = onFriendClick,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 4.dp,
            color = Color(0xFF3E3E3E),
        ) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Text("👥", fontSize = 20.sp)
            }
        }
    }
}

/**
 * 현재 위치 버튼
 */
@Composable
fun CurrentLocationButton(
    isFollowing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color = Color.White,
        shape = CircleShape,
        shadowElevation = 8.dp,
        modifier = modifier.size(56.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_current_location),
                contentDescription = "현재 위치",
                tint = if (isFollowing) Color(0xFF666666) else Color(0xFF0163BA),
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

/**
 * 하단 네비게이션 바
 */
@Composable
fun BottomNavigationBar(
    currentTab: BottomTab,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        containerColor = Color.White,
        modifier = modifier,
    ) {
        listOf(
            BottomTab.MAP to Icons.Default.Home,
            BottomTab.REPORT to Icons.Default.Build,
            BottomTab.FRIEND to Icons.Default.Group,
            BottomTab.MY to Icons.Default.Person,
        ).forEach { (tab, icon) ->
            NavigationBarItem(
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
            )
        }
    }
}

/**
 * 로딩 인디케이터
 */
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = Color(0xFF2E7DFF),
        )
    }
}

@Preview
@Composable
fun PreviewTopSearchBar() {
    CurrentLocationButton(
        isFollowing = false,
        onClick = {},
    )
}
