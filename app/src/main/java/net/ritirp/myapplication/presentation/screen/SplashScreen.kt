package net.ritirp.myapplication.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import net.ritirp.myapplication.R

/**
 * 스플래시 화면
 * 앱 시작 시 표시되는 초기 화면
 */
@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 2초 후 로그인 화면으로 이동
    LaunchedEffect(Unit) {
        delay(2000)
        onNavigateToLogin()
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        // 로고 이미지
        Image(
            painter = painterResource(id = R.drawable.logo_splash),
            contentDescription = "RIBUDDY Logo",
            modifier =
                Modifier
                    .width(280.dp)
                    .height(100.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen(
        onNavigateToLogin = {},
    )
}
