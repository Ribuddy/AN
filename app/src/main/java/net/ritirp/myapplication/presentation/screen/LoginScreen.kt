package net.ritirp.myapplication.presentation.screen

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import net.ritirp.myapplication.R
import net.ritirp.myapplication.data.model.AuthState

/**
 * 로그인 화면
 * 구글 및 카카오 로그인 기능 제공
 */
@Composable
fun LoginScreen(
    onGoogleLoginSuccess: (idToken: String, userName: String?, userEmail: String?) -> Unit,
    onKakaoLoginClick: () -> Unit,
    authState: AuthState = AuthState.Idle,
    modifier: Modifier = Modifier,
    onLoginError: (String) -> Unit,
) {
    val context = LocalContext.current

    // GoogleSignInClient 인스턴스 생성
    val googleSignInClient =
        remember {
            getGoogleSignInClient(context)
        }

    // Google 로그인 결과 처리를 위한 ActivityResultLauncher
    val googleSignInLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleSignInResult(task, onGoogleLoginSuccess, onLoginError)
            } else {
                onLoginError("Google 로그인에 실패했습니다. (결과 코드: ${result.resultCode})")
            }
        }

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

            // 에러 메시지 표시
            if (authState is AuthState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                ) {
                    Text(
                        text = authState.message,
                        color = Color(0xFFD32F2F),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            // "Login with" 구분선
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFE0E0E0),
                    thickness = 1.dp,
                )
                Text(
                    text = "  Login with  ",
                    fontSize = 14.sp,
                    color = Color.Gray,
                )
                HorizontalDivider(
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
                enabled = authState !is AuthState.Loading,
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
                onClick = {
                    val signInIntent = googleSignInClient.signInIntent
                    googleSignInLauncher.launch(signInIntent)
                },
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
                enabled = authState !is AuthState.Loading,
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF4285F4),
                        strokeWidth = 2.dp,
                    )
                } else {
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
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

private fun getGoogleSignInClient(context: android.content.Context): GoogleSignInClient {
    val gso =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("649923655769-933ci1q9lvq6g4kpgmevmccmm08mvpsj.apps.googleusercontent.com")
            .requestEmail()
            .build()
    return GoogleSignIn.getClient(context, gso)
}

/**
 * Google 로그인 결과 처리
 */
private fun handleSignInResult(
    completedTask: Task<GoogleSignInAccount>,
    onSuccess: (String, String?, String?) -> Unit,
    onError: (String) -> Unit,
) {
    try {
        val account = completedTask.getResult(ApiException::class.java)
        val idToken = account?.idToken
        val userName = account?.displayName
        val userEmail = account?.email

        if (idToken != null) {
            Log.d("LoginScreen", "Google ID Token: $idToken")
            Log.d("LoginScreen", "User Name: $userName")
            Log.d("LoginScreen", "User Email: $userEmail")
            onSuccess(idToken, userName, userEmail)
        } else {
            onError("Google ID 토큰을 가져오는 데 실패했습니다.")
        }
    } catch (e: ApiException) {
        Log.w("LoginScreen", "signInResult:failed code=" + e.statusCode)
        onError("Google 로그인에 실패했습니다. (에러 코드: ${e.statusCode})")
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        onGoogleLoginSuccess = { _, _, _ -> },
        onKakaoLoginClick = {},
        onLoginError = {},
    )
}
