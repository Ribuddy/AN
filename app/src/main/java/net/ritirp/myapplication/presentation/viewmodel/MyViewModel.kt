package net.ritirp.myapplication.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ritirp.myapplication.data.model.UserProfile
import net.ritirp.myapplication.data.repository.AuthRepository
import net.ritirp.myapplication.data.repository.UserRepository

/**
 * MY 화면의 UI 상태
 */
data class MyUiState(
    val userProfile: UserProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedOut: Boolean = false,
)

/**
 * MY 화면 ViewModel
 */
class MyViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyUiState())
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    /**
     * 사용자 프로필 정보 로드
     */
    private fun loadUserProfile() {
        viewModelScope.launch {
            Log.d("MyViewModel", "프로필 로드 시작")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            Log.d("MyViewModel", "로딩 상태 설정: ${_uiState.value.isLoading}")

            userRepository.getMyProfile()
                .onSuccess { profile ->
                    Log.d("MyViewModel", "프로필 로드 성공: ${profile.name}, @${profile.ribuddyId}")
                    _uiState.value = _uiState.value.copy(
                        userProfile = profile,
                        isLoading = false,
                    )
                    Log.d("MyViewModel", "UI 상태 업데이트: name=${_uiState.value.userProfile?.name}, id=${_uiState.value.userProfile?.ribuddyId}")
                }
                .onFailure { exception ->
                    Log.e("MyViewModel", "프로필 로드 실패: ${exception.message}", exception)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "프로필 로드 실패",
                    )
                }
        }
    }

    /**
     * 프로필 새로고침
     */
    fun refreshProfile() {
        loadUserProfile()
    }

    /**
     * 로그아웃
     */
    fun logout() {
        viewModelScope.launch {
            try {
                Log.d("MyViewModel", "로그아웃 시작")
                authRepository.logout()
                _uiState.value = _uiState.value.copy(
                    isLoggedOut = true,
                    userProfile = null,
                )
                Log.d("MyViewModel", "로그아웃 완료")
            } catch (e: Exception) {
                Log.e("MyViewModel", "로그아웃 실패", e)
                _uiState.value = _uiState.value.copy(
                    error = "로그아웃 실패: ${e.message}",
                )
            }
        }
    }
}

/**
 * MyViewModel Factory
 */
class MyViewModelFactory(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyViewModel::class.java)) {
            return MyViewModel(userRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
