package net.ritirp.myapplication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ritirp.myapplication.data.model.TeamInfo
import net.ritirp.myapplication.data.repository.TeamRepository

/**
 * 팀 관리 ViewModel
 */
class TeamViewModel(
    private val teamRepository: TeamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeamUiState())
    val uiState: StateFlow<TeamUiState> = _uiState.asStateFlow()

    init {
        loadTeamList()
    }

    /**
     * 팀 목록 새로고침
     */
    fun loadTeamList() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            teamRepository.getTeamList()
                .onSuccess { teams ->
                    _uiState.value = _uiState.value.copy(
                        teams = teams,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "팀 목록을 불러오는데 실패했습니다.",
                        isLoading = false
                    )
                }
        }
    }

    /**
     * 팀 생성
     */
    fun createTeam(
        name: String,
        description: String? = null,
        members: List<String> = emptyList(),
        isCrew: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            teamRepository.createTeam(name, description, members, isCrew)
                .onSuccess { teamId ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "팀이 생성되었습니다."
                    )
                    // 팀 목록 새로고침
                    loadTeamList()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "팀 생성에 실패했습니다.",
                        isLoading = false
                    )
                }
        }
    }

    /**
     * 팀 참여
     */
    fun joinTeam(teamId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            teamRepository.joinTeam(teamId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "팀에 참여했습니다."
                    )
                    // 팀 목록 새로고침
                    loadTeamList()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "팀 참여에 실패했습니다.",
                        isLoading = false
                    )
                }
        }
    }

    /**
     * 팀 탈퇴
     */
    fun leaveTeam(teamId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            teamRepository.leaveTeam(teamId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "팀에서 탈퇴했습니다."
                    )
                    // 팀 목록 새로고침
                    loadTeamList()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "팀 탈퇴에 실패했습니다.",
                        isLoading = false
                    )
                }
        }
    }

    /**
     * 팀 정보 조회
     */
    fun getTeamInfo(teamId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            teamRepository.getTeamInfo(teamId)
                .onSuccess { teamInfo ->
                    _uiState.value = _uiState.value.copy(
                        selectedTeam = teamInfo,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "팀 정보 조회에 실패했습니다.",
                        isLoading = false
                    )
                }
        }
    }

    /**
     * 팀 참여 코드 조회
     */
    fun getTeamJoinCode(teamId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(teamJoinCode = null) // 이전 코드 초기화

            teamRepository.getTeamJoinCode(teamId)
                .onSuccess { joinCode ->
                    android.util.Log.d("TeamViewModel", "참여 코드 받음: $joinCode")
                    _uiState.value = _uiState.value.copy(
                        teamJoinCode = joinCode
                    )
                }
                .onFailure { error ->
                    android.util.Log.e("TeamViewModel", "참여 코드 조회 실패: ${error.message}")
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "참여 코드 조회에 실패했습니다."
                    )
                }
        }
    }

    /**
     * 에러 메시지 초기화
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * 성공 메시지 초기화
     */
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    /**
     * 선택된 팀 초기화
     */
    fun clearSelectedTeam() {
        _uiState.value = _uiState.value.copy(
            selectedTeam = null,
            teamJoinCode = null  // 참여 코드도 함께 초기화
        )
    }
}

/**
 * 팀 UI 상태
 */
data class TeamUiState(
    val teams: List<TeamInfo> = emptyList(),
    val selectedTeam: TeamInfo? = null,
    val teamJoinCode: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

/**
 * TeamViewModel Factory
 */
class TeamViewModelFactory(
    private val teamRepository: TeamRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeamViewModel::class.java)) {
            return TeamViewModel(teamRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
