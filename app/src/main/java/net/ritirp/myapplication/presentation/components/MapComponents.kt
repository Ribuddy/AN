package net.ritirp.myapplication.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ritirp.myapplication.R
import net.ritirp.myapplication.presentation.viewmodel.BottomTab

/**
 * 상단 검색바
 */
@Composable
fun TopSearchBar(
    onFriendClick: () -> Unit = {},
    onSearchBarClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onSearchBarClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 4.dp,
            color = Color.White,
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    "오늘은 어디를 달릴까요?",
                    fontSize = 15.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
        val items =
            listOf(
                BottomTab.MAP,
                BottomTab.REPORT,
                BottomTab.BUDDY,
                BottomTab.MY,
            )

        items.forEach { tab ->
            val iconRes =
                when (tab) {
                    BottomTab.MAP -> R.drawable.ic_bottom_map
                    BottomTab.REPORT -> R.drawable.ic_bottom_report
                    BottomTab.BUDDY -> R.drawable.ic_bottom_buddy
                    BottomTab.MY -> R.drawable.ic_bottom_my
                }

            val isSelected = currentTab == tab
            val selectedColor = Color(0xFF4285F4)
            val unselectedColor = Color(0xFF9E9E9E)

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = tab.label,
                        tint = if (isSelected) selectedColor else unselectedColor,
                        modifier = Modifier.size(if (isSelected) 30.dp else 26.dp),
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        color = if (isSelected) selectedColor else unselectedColor,
                    )
                },
                colors =
                    NavigationBarItemDefaults.colors(
                        indicatorColor = selectedColor.copy(alpha = 0.12f),
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        unselectedIconColor = unselectedColor,
                        unselectedTextColor = unselectedColor,
                    ),
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

/**
 * 출발지/도착지 입력 다이얼로그
 */
@Composable
fun RouteInputDialog(
    currentLocationName: String = "내 현재 위치",
    onDismiss: () -> Unit,
    onConfirm: (departure: String, destination: String) -> Unit,
) {
    var departureText by remember { mutableStateOf(currentLocationName) }
    var destinationText by remember { mutableStateOf("") }
    var useCurrentLocation by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = "🚴 경로 설정",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4285F4),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "원하는 경로를 입력하세요",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // 출발지 섹션
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F8FF),
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "📍",
                                fontSize = 20.sp,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text(
                                text = "출발지",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4285F4),
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Checkbox(
                                checked = useCurrentLocation,
                                onCheckedChange = { checked ->
                                    useCurrentLocation = checked
                                    if (checked) {
                                        departureText = currentLocationName
                                    } else {
                                        departureText = ""
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF4285F4),
                                    checkmarkColor = Color.White,
                                ),
                            )
                            Text(
                                text = "내 현재 위치 사용",
                                fontSize = 14.sp,
                                color = Color(0xFF333333),
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }

                        if (!useCurrentLocation) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = departureText,
                                onValueChange = { departureText = it },
                                placeholder = {
                                    Text(
                                        "출발지를 입력하세요",
                                        color = Color(0xFFAAAAAA),
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4285F4),
                                    unfocusedBorderColor = Color(0xFFDDDDDD),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                ),
                            )
                        }
                    }
                }

                // 도착지 섹션
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F8FF),
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "🏁",
                                fontSize = 20.sp,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text(
                                text = "도착지",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4285F4),
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = destinationText,
                            onValueChange = { destinationText = it },
                            placeholder = {
                                Text(
                                    "도착지를 입력하세요",
                                    color = Color(0xFFAAAAAA),
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4285F4),
                                unfocusedBorderColor = Color(0xFFDDDDDD),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                            ),
                        )
                    }
                }

                // 안내 문구
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                ) {
                    Text(
                        text = "💡 ",
                        fontSize = 14.sp,
                    )
                    Text(
                        text = "지도에서 원하는 위치를 터치해도 도착지를 설정할 수 있습니다.",
                        fontSize = 13.sp,
                        color = Color(0xFF888888),
                        lineHeight = 18.sp,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (destinationText.isNotBlank()) {
                        onConfirm(departureText, destinationText)
                    }
                },
                enabled = destinationText.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4285F4),
                    disabledContainerColor = Color(0xFFCCCCCC),
                ),
                modifier = Modifier.height(48.dp),
            ) {
                Text(
                    "경로 찾기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(48.dp),
            ) {
                Text(
                    "취소",
                    color = Color(0xFF666666),
                    fontSize = 16.sp,
                )
            }
        },
    )
}

@Preview
@Composable
fun PreviewTopSearchBar() {
    CurrentLocationButton(
        isFollowing = false,
        onClick = {},
    )
}
