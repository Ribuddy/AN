package net.ritirp.myapplication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ritirp.myapplication.data.model.FriendInfo
import net.ritirp.myapplication.data.repository.AuthRepository
import net.ritirp.myapplication.data.repository.FriendRepository

/**
 * 친구 관리 ViewModel
 */
class FriendViewModel(
    private val friendRepository: FriendRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FriendUiState())
    val uiState: StateFlow<FriendUiState> = _uiState.asStateFlow()

    init {
        loadFriendList()
        loadMyRibuddyId()
    }

    /**
     * 내 라이버디 ID 로드
     */
    private fun loadMyRibuddyId() {
        viewModelScope.launch {
            // 서버에서 최신 정보 가져오기
            authRepository.fetchMyInfo()
                .onSuccess { userInfo ->
                    _uiState.value =
                        _uiState.value.copy(
                            myRibuddyId = userInfo.ribuddyId,
                            myName = userInfo.name,
                        )
                }
                .onFailure { error ->
                    // 실패 시 로컬 저장소에서 가져오기
                    authRepository.getUserData().first()?.let { userData ->
                        _uiState.value =
                            _uiState.value.copy(
                                myRibuddyId = userData.ribuddyId,
                                myName = userData.name,
                            )
                    }
                }
        }
    }

    /**
     * 친구 목록 새로고침
     */
    fun loadFriendList() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            friendRepository.getFriendList()
                .onSuccess { friendList ->
                    _uiState.value =
                        _uiState.value.copy(
                            favorites = friendList.favorites,
                            friends = friendList.friends,
                            isLoading = false,
                        )
                }
                .onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            error = error.message ?: "친구 목록을 불러오는데 실패했습니다.",
                            isLoading = false,
                        )
                }
        }
    }

    /**
     * 친구 추가
     */
    fun addFriend(ribuddyId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            friendRepository.addFriendByRibuddyId(ribuddyId)
                .onSuccess {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            successMessage = "친구가 추가되었습니다.",
                        )
                    // 친구 목록 새로고침
                    loadFriendList()
                }
                .onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            error = error.message ?: "친구 추가에 실패했습니다.",
                            isLoading = false,
                        )
                }
        }
    }

    /**
     * 친구 삭제
     */
    fun deleteFriend(friendUserId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            friendRepository.deleteFriend(friendUserId)
                .onSuccess {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            successMessage = "친구가 삭제되었습니다.",
                        )
                    // 친구 목록 새로고침
                    loadFriendList()
                }
                .onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            error = error.message ?: "친구 삭제에 실패했습니다.",
                            isLoading = false,
                        )
                }
        }
    }

    /**
     * 즐겨찾기 토글
     */
    fun toggleFavorite(
        friendUserId: String,
        currentIsFavorite: Boolean,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = null)

            friendRepository.toggleFavorite(friendUserId, !currentIsFavorite)
                .onSuccess {
                    // 친구 목록 새로고침
                    loadFriendList()
                }
                .onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            error = error.message ?: "즐겨찾기 변경에 실패했습니다.",
                        )
                }
        }
    }

    /**
     * 검색어 업데이트
     */
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
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
}

/**
 * 친구 UI 상태
 */
data class FriendUiState(
    val favorites: List<FriendInfo> = emptyList(),
    val friends: List<FriendInfo> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val myRibuddyId: String? = null,
    val myName: String? = null,
) {
    // 검색 필터링된 즐겨찾기
    val filteredFavorites: List<FriendInfo>
        get() =
            if (searchQuery.isBlank()) {
                favorites
            } else {
                favorites.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                        it.ribuddyId.contains(searchQuery, ignoreCase = true) ||
                        it.nickname?.contains(searchQuery, ignoreCase = true) == true
                }
            }

    // 검색 필터링된 친구
    val filteredFriends: List<FriendInfo>
        get() =
            if (searchQuery.isBlank()) {
                friends
            } else {
                friends.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                        it.ribuddyId.contains(searchQuery, ignoreCase = true) ||
                        it.nickname?.contains(searchQuery, ignoreCase = true) == true
                }
            }
}

/**
 * FriendViewModel Factory
 */
class FriendViewModelFactory(
    private val friendRepository: FriendRepository,
    private val authRepository: AuthRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FriendViewModel::class.java)) {
            return FriendViewModel(friendRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
