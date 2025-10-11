package net.ritirp.myapplication.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ritirp.myapplication.R

/**
 * 로그인 화면
 * 구글 및 카카오 로그인 기능 제공
 */
@Composable
fun LoginScreen(
    onGoogleLoginClick: () -> Unit,
    onKakaoLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // 로고 이미지
            Image(
                painter = painterResource(id = R.drawable.logo_splash),
                contentDescription = "RIBUDDY Logo",
                modifier =
                    Modifier
                        .width(280.dp)
                        .height(100.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 부제목
            Text(
                text = "Team Riding System",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.weight(1f))

            // "Login with" 구분선
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Divider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFE0E0E0),
                    thickness = 1.dp,
                )
                Text(
                    text = "  Login with  ",
                    fontSize = 14.sp,
                    color = Color.Gray,
                )
                Divider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFE0E0E0),
                    thickness = 1.dp,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 카카오 로그인 버튼
            Button(
                onClick = onKakaoLoginClick,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFEE500),
                        contentColor = Color.Black,
                    ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "💬",
                        fontSize = 24.sp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "카카오 로그인",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 구글 로그인 버튼
            Button(
                onClick = onGoogleLoginClick,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(
                            width = 1.dp,
                            color = Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(8.dp),
                        ),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                    ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "G",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4285F4),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Google",
                        fontSize = 16.sp,
                        color = Color(0xFF757575),
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        onGoogleLoginClick = {},
        onKakaoLoginClick = {},
    )
}
