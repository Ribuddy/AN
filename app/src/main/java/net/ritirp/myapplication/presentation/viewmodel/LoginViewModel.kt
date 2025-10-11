package net.ritirp.myapplication.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ritirp.myapplication.data.model.AuthState
import net.ritirp.myapplication.data.repository.AuthRepository

/**
 * 로그인 화면 ViewModel
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /**
     * OAuth 콜백 처리
     * Deep Link로 받은 Authorization Code를 서버로 전달합니다.
     */
    fun handleOAuthCallback(
        idToken: String,
        userName: String? = null,
        userEmail: String? = null,
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            // 로그: 서버로 보낼 ID Token 확인
            Log.d("LoginViewModel", "========== Google 로그인 시작 ==========")
            Log.d("LoginViewModel", "서버로 보낼 ID Token (앞 50자): ${idToken.take(50)}...")
            Log.d("LoginViewModel", "ID Token 전체 길이: ${idToken.length}")
            Log.d("LoginViewModel", "Google 사용자 정보: name=$userName, email=$userEmail")

            val result = authRepository.handleGoogleCallback(idToken)

            _authState.value =
                if (result.isSuccess) {
                    val loginResponse = result.getOrNull()!!
                    Log.d("LoginViewModel", "로그인 성공! userId: ${loginResponse.userId}")
                    AuthState.Success(
                        net.ritirp.myapplication.data.model.UserData(
                            userId = loginResponse.userId,
                            email = userEmail ?: loginResponse.email,
                            name = userName ?: loginResponse.name,
                        ),
                    )
                } else {
                    Log.e("LoginViewModel", "로그인 실패: ${result.exceptionOrNull()?.message}")
                    AuthState.Error(result.exceptionOrNull()?.message ?: "Login failed")
                }
        }
    }

    /**
     * 로그인 상태 확인
     */
    fun checkLoginStatus() {
        viewModelScope.launch {
            authRepository.getUserData().collect { userData ->
                if (userData != null) {
                    _authState.value = AuthState.Success(userData)
                }
            }
        }
    }

    /**
     * 로그아웃
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _authState.value = AuthState.Idle
        }
    }

    /**
     * 에러 상태 설정 (외부에서 호출용)
     */
    fun setError(message: String) {
        _authState.value = AuthState.Error(message)
    }
}
