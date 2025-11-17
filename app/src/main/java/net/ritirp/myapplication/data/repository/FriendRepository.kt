package net.ritirp.myapplication.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.first
import net.ritirp.myapplication.data.api.RetrofitClient
import net.ritirp.myapplication.data.local.DataStoreManager
import net.ritirp.myapplication.data.model.*

/**
 * 친구 관련 Repository
 */
class FriendRepository(private val context: Context) {
    private val friendApi = RetrofitClient.friendApi
    private val dataStore = DataStoreManager.getDataStore(context)

    /**
     * 저장된 토큰 가져오기
     */
    private suspend fun getAccessToken(): String? {
        return dataStore.data.first()[DataStoreManager.ACCESS_TOKEN_KEY]
    }

    /**
     * 친구 목록 조회
     */
    suspend fun getFriendList(): Result<FriendListResponse> {
        return try {
            val token = getAccessToken()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("로그인이 필요합니다."))
            }

            Log.d("FriendRepository", "친구 목록 조회 요청")
            val response = friendApi.getFriendList("Bearer $token")

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val friendList = response.body()?.result ?: emptyList() // result가 직접 List
                // 즐겨찾기와 일반 친구로 분리
                val favorites = friendList.filter { it.isFavorite }
                val friends = friendList.filter { !it.isFavorite }

                Log.d("FriendRepository", "친구 목록 조회 성공: 즐겨찾기 ${favorites.size}명, 친구 ${friends.size}명")
                Result.success(FriendListResponse(favorites = favorites, friends = friends))
            } else {
                val errorMsg = response.body()?.message ?: "친구 목록 조회에 실패했습니다."
                Log.e("FriendRepository", "친구 목록 조회 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("FriendRepository", "친구 목록 조회 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 라이버디 ID로 친구 추가
     */
    suspend fun addFriendByRibuddyId(ribuddyId: String): Result<Unit> {
        return try {
            val token = getAccessToken()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("로그인이 필요합니다."))
            }

            val request = AddFriendByRibuddyIdRequest(ribuddyId = ribuddyId)
            Log.d("FriendRepository", "친구 추가 요청: ribuddyId=$ribuddyId")
            val response = friendApi.addFriendByRibuddyId("Bearer $token", request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Log.d("FriendRepository", "친구 추가 성공")
                Result.success(Unit)
            } else {
                val errorMsg = response.body()?.message ?: "친구 추가에 실패했습니다."
                Log.e("FriendRepository", "친구 추가 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("FriendRepository", "친구 추가 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 친구 삭제
     */
    suspend fun deleteFriend(friendUserId: String): Result<Unit> {
        return try {
            val token = getAccessToken()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("로그인이 필요합니다."))
            }

            val request = DeleteFriendRequest(friendUserId = friendUserId)
            Log.d("FriendRepository", "친구 삭제 요청: userId=$friendUserId")
            val response = friendApi.deleteFriend("Bearer $token", request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Log.d("FriendRepository", "친구 삭제 성공")
                Result.success(Unit)
            } else {
                val errorMsg = response.body()?.message ?: "친구 삭제에 실패했습니다."
                Log.e("FriendRepository", "친구 삭제 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("FriendRepository", "친구 삭제 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 친구 즐겨찾기 설정/해제
     */
    suspend fun toggleFavorite(
        friendUserId: String,
        isFavorite: Boolean,
    ): Result<Unit> {
        return try {
            val token = getAccessToken()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("로그인이 필요합니다."))
            }

            val request =
                EditFriendStatusRequest(
                    toUserId = friendUserId,
                    isFavorite = isFavorite,
                )
            Log.d("FriendRepository", "친구 즐겨찾기 변경: userId=$friendUserId, isFavorite=$isFavorite")
            val response = friendApi.editFriendStatus("Bearer $token", request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Log.d("FriendRepository", "친구 즐겨찾기 변경 성공")
                Result.success(Unit)
            } else {
                val errorMsg = response.body()?.message ?: "친구 즐겨찾기 변경에 실패했습니다."
                Log.e("FriendRepository", "친구 즐겨찾기 변경 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("FriendRepository", "친구 즐겨찾기 변경 오류", e)
            Result.failure(e)
        }
    }
}
