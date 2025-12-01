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
    val friendCount: Int = 0,
    val teamCount: Int = 0,
    val ridingRecordCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedOut: Boolean = false,
    val isUpdatingRibuddyId: Boolean = false,
    val updateSuccess: Boolean = false,
    val updateError: String? = null,
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

                    // 친구 수, 팀 수, 주행 기록 수 가져오기
                    val friendCount = userRepository.getFriendCount().getOrNull() ?: 0
                    val teamCount = userRepository.getTeamCount().getOrNull() ?: 0
                    val ridingRecordCount = userRepository.getRidingRecordCount().getOrNull() ?: 0

                    Log.d("MyViewModel", "통계 로드: 친구=$friendCount, 팀=$teamCount, 주행기록=$ridingRecordCount")

                    _uiState.value =
                        _uiState.value.copy(
                            userProfile = profile,
                            friendCount = friendCount,
                            teamCount = teamCount,
                            ridingRecordCount = ridingRecordCount,
                            isLoading = false,
                        )
                    Log.d(
                        "MyViewModel",
                        "UI 상태 업데이트: name=${_uiState.value.userProfile?.name}, id=${_uiState.value.userProfile?.ribuddyId}",
                    )
                }
                .onFailure { exception ->
                    Log.e("MyViewModel", "프로필 로드 실패: ${exception.message}", exception)
                    _uiState.value =
                        _uiState.value.copy(
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
                _uiState.value =
                    _uiState.value.copy(
                        isLoggedOut = true,
                        userProfile = null,
                    )
                Log.d("MyViewModel", "로그아웃 완료")
            } catch (e: Exception) {
                Log.e("MyViewModel", "로그아웃 실패", e)
                _uiState.value =
                    _uiState.value.copy(
                        error = "로그아웃 실패: ${e.message}",
                    )
            }
        }
    }

    /**
     * 라이버디 ID 변경
     */
    fun updateRibuddyId(newRibuddyId: String) {
        viewModelScope.launch {
            try {
                Log.d("MyViewModel", "라이버디 ID 변경 시작: $newRibuddyId")
                _uiState.value = _uiState.value.copy(
                    isUpdatingRibuddyId = true,
                    updateSuccess = false,
                    updateError = null,
                )

                userRepository.updateRibuddyId(newRibuddyId)
                    .onSuccess {
                        Log.d("MyViewModel", "라이버디 ID 변경 성공")
                        _uiState.value = _uiState.value.copy(
                            isUpdatingRibuddyId = false,
                            updateSuccess = true,
                        )
                        // 프로필 새로고침
                        loadUserProfile()
                    }
                    .onFailure { exception ->
                        Log.e("MyViewModel", "라이버디 ID 변경 실패: ${exception.message}", exception)
                        _uiState.value = _uiState.value.copy(
                            isUpdatingRibuddyId = false,
                            updateError = exception.message ?: "라이버디 ID 변경 실패",
                        )
                    }
            } catch (e: Exception) {
                Log.e("MyViewModel", "라이버디 ID 변경 중 예외 발생", e)
                _uiState.value = _uiState.value.copy(
                    isUpdatingRibuddyId = false,
                    updateError = e.message ?: "알 수 없는 오류",
                )
            }
        }
    }

    /**
     * 업데이트 상태 초기화
     */
    fun clearUpdateStatus() {
        _uiState.value = _uiState.value.copy(
            updateSuccess = false,
            updateError = null,
        )
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
