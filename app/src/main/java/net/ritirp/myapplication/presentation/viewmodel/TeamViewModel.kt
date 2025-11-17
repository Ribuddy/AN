package net.ritirp.myapplication.presentation.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.ritirp.myapplication.data.model.RidingStatus
import net.ritirp.myapplication.data.model.TeamInfo
import net.ritirp.myapplication.data.model.TeamMemberLocation
import net.ritirp.myapplication.data.repository.DrivingRepository
import net.ritirp.myapplication.data.repository.MapRepository
import net.ritirp.myapplication.data.repository.TeamRepository
import kotlin.coroutines.resume

/**
 * 팀 관리 ViewModel
 */
class TeamViewModel(
    private val teamRepository: TeamRepository,
    private val drivingRepository: DrivingRepository,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val mapRepository: MapRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TeamUiState())
    val uiState: StateFlow<TeamUiState> = _uiState.asStateFlow()

    init {
        loadTeamList()
        observeRidingStatus()
    }

    /**
     * 실제 GPS 위치 가져오기
     */
    @SuppressLint("MissingPermission")
    private suspend fun getCurrentGpsLocation(): Pair<Double, Double> =
        suspendCancellableCoroutine { continuation ->
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token,
            ).addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(Pair(location.latitude, location.longitude))
                } else {
                    // 위치를 가져오지 못한 경우 기본 위치 사용
                    continuation.resume(Pair(37.5666102, 126.9783881))
                }
            }.addOnFailureListener {
                // 실패한 경우 기본 위치 사용
                continuation.resume(Pair(37.5666102, 126.9783881))
            }
        }

    /**
     * 라이딩 상태 관찰
     */
    private fun observeRidingStatus() {
        viewModelScope.launch {
            drivingRepository.ridingStatus.collect { status ->
                _uiState.value = _uiState.value.copy(ridingStatus = status)
            }
        }

        viewModelScope.launch {
            drivingRepository.currentRidingRecordId.collect { recordId ->
                _uiState.value = _uiState.value.copy(currentRidingRecordId = recordId)
            }
        }

        viewModelScope.launch {
            drivingRepository.teamMemberLocations.collect { locations ->
                _uiState.value = _uiState.value.copy(teamMemberLocations = locations)
                // 팀원 위치를 지도 마커로 업데이트
                if (locations.isNotEmpty()) {
                    mapRepository.updateTeamMemberMarkers(locations)
                } else {
                    mapRepository.clearTeamMarkers()
                }
            }
        }
    }

    /**
     * 팀 목록 새로고침
     */
    fun loadTeamList() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            teamRepository.getTeamList()
                .onSuccess { teams ->
                    _uiState.value =
                        _uiState.value.copy(
                            teams = teams,
                            isLoading = false,
                        )
                }
                .onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            error = error.message ?: "팀 목록을 불러오는데 실패했습니다.",
                            isLoading = false,
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
        isCrew: Boolean = false,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            teamRepository.createTeam(name, description, members, isCrew)
                .onSuccess { teamId ->
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            successMessage = "팀이 생성되었습니다.",
                        )
                    // 팀 목록 새로고침
                    loadTeamList()
                }
                .onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            error = error.message ?: "팀 생성에 실패했습니다.",
                            isLoading = false,
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
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            successMessage = "팀에 참여했습니다.",
                        )
                    // 팀 목록 새로고침
                    loadTeamList()
                }
                .onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            error = error.message ?: "팀 참여에 실패했습니다.",
                            isLoading = false,
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
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            successMessage = "팀에서 탈퇴했습니다.",
                        )
                    // 팀 목록 새로고침
                    loadTeamList()
                }
                .onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            error = error.message ?: "팀 탈퇴에 실패했습니다.",
                            isLoading = false,
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
                    _uiState.value =
                        _uiState.value.copy(
                            selectedTeam = teamInfo,
                            isLoading = false,
                        )
                }
                .onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            error = error.message ?: "팀 정보 조회에 실패했습니다.",
                            isLoading = false,
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
                    _uiState.value =
                        _uiState.value.copy(
                            teamJoinCode = joinCode,
                        )
                }
                .onFailure { error ->
                    android.util.Log.e("TeamViewModel", "참여 코드 조회 실패: ${error.message}")
                    _uiState.value =
                        _uiState.value.copy(
                            error = error.message ?: "참여 코드 조회에 실패했습니다.",
                        )
                }
        }
    }

    /**
     * 팀 라이딩 시작
     */
    fun startTeamRiding(
        teamId: String,
        lat: Double,
        lon: Double,
        ele: Double? = null,
        name: String? = null,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            drivingRepository.startTeamRiding(teamId, lat, lon, ele, name)
                .onSuccess { ridingRecordId ->
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            successMessage = "팀 라이딩이 시작되었습니다.",
                            currentRidingRecordId = ridingRecordId,
                        )
                    // LocationUpdateManager가 자동으로 위치 업데이트 시작
                    android.util.Log.d("TeamViewModel", "팀 라이딩 시작 성공, LocationUpdateManager가 자동으로 위치 업데이트 시작")
                }
                .onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            error = error.message ?: "팀 라이딩 시작에 실패했습니다.",
                            isLoading = false,
                        )
                }
        }
    }

    /**
     * 팀 라이딩 시작
     */
    fun startRiding(teamId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // 실제 GPS 위치 가져오기
            val (lat, lon) = getCurrentGpsLocation()

            startTeamRiding(teamId, lat, lon, null, "시작 위치")
        }
    }

    /**
     * 위치 업데이트 (주기적으로 호출)
     */
    fun updateMyLocationAndGetTeamLocations(
        lat: Double,
        lon: Double,
        ele: Double? = null,
    ) {
        val ridingRecordId = _uiState.value.currentRidingRecordId

        if (ridingRecordId == null) {
            android.util.Log.w("TeamViewModel", "ridingRecordId가 null입니다. 위치 업데이트를 건너뜁니다.")
            return
        }

        android.util.Log.d("TeamViewModel", "위치 업데이트 API 호출: ridingRecordId=$ridingRecordId, lat=$lat, lon=$lon")

        viewModelScope.launch {
            drivingRepository.updateLocationAndGetTeamLocations(ridingRecordId, lat, lon, ele)
                .onSuccess { locations ->
                    android.util.Log.d("TeamViewModel", "위치 업데이트 성공: ${locations.size}명의 팀원")
                    _uiState.value =
                        _uiState.value.copy(
                            teamMemberLocations = locations,
                        )
                }
                .onFailure { error ->
                    // 위치 업데이트는 조용히 실패 처리 (너무 자주 발생할 수 있음)
                    android.util.Log.e("TeamViewModel", "위치 업데이트 실패: ${error.message}")
                }
        }
    }

    /**
     * 팀 라이딩 종료
     */
    fun endTeamRiding(
        lat: Double,
        lon: Double,
        ele: Double? = null,
        name: String? = null,
    ) {
        val ridingRecordId = _uiState.value.currentRidingRecordId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            drivingRepository.endTeamRiding(ridingRecordId, lat, lon, ele, name)
                .onSuccess {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            successMessage = "팀 라이딩이 종료되었습니다.",
                            currentRidingRecordId = null,
                            teamMemberLocations = emptyList(),
                        )
                }
                .onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            error = error.message ?: "팀 라이딩 종료에 실패했습니다.",
                            isLoading = false,
                        )
                }
        }
    }

    /**
     * 팀 라이딩 종료
     */
    fun endRiding() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // 실제 GPS 위치 가져오기
            val (lat, lon) = getCurrentGpsLocation()

            endTeamRiding(lat, lon, null, "종료 위치")
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
        _uiState.value =
            _uiState.value.copy(
                selectedTeam = null,
                teamJoinCode = null, // 참여 코드도 함께 초기화
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
    val successMessage: String? = null,
    val ridingStatus: RidingStatus = RidingStatus.IDLE,
    val currentRidingRecordId: String? = null,
    val teamMemberLocations: List<TeamMemberLocation> = emptyList(),
)

/**
 * TeamViewModel Factory
 */
class TeamViewModelFactory(
    private val teamRepository: TeamRepository,
    private val drivingRepository: DrivingRepository,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val mapRepository: MapRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeamViewModel::class.java)) {
            return TeamViewModel(teamRepository, drivingRepository, fusedLocationClient, mapRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
