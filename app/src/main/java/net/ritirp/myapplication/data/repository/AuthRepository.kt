package net.ritirp.myapplication.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import net.ritirp.myapplication.data.api.RetrofitClient
import net.ritirp.myapplication.data.local.DataStoreManager
import net.ritirp.myapplication.data.model.LoginResponse
import net.ritirp.myapplication.data.model.UserData
import net.ritirp.myapplication.data.model.UserInfo

/**
 * 인증 관련 Repository
 */
class AuthRepository(private val context: Context) {
    private val authApi = RetrofitClient.authApi
    private val dataStore = DataStoreManager.getDataStore(context)

    /**
     * Google OAuth 콜백 처리
     * ID 토큰을 서버로 전달하여 JWT 토큰을 받아옵니다.
     */
    suspend fun handleGoogleCallback(idToken: String): Result<LoginResponse> {
        return try {
            // ID 토큰을 담을 요청 객체 생성
            val request = mapOf("idToken" to idToken)

            Log.d("AuthRepository", "========== 서버 API 호출 ==========")
            Log.d("AuthRepository", "URL: https://ribuddy.kyeoungwoon.kr/v1/auth/google/login")
            Log.d("AuthRepository", "요청 Body: { idToken: \"${idToken.take(50)}...\" }")

            val response = authApi.verifyGoogleToken(request)

            Log.d("AuthRepository", "서버 응답 코드: ${response.code()}")
            Log.d("AuthRepository", "서버 응답 성공 여부: ${response.isSuccessful}")

            if (response.isSuccessful) {
                val apiResponse = response.body()
                Log.d("AuthRepository", "응답 Body: isSuccess=${apiResponse?.isSuccess}, message=${apiResponse?.message}")

                if (apiResponse?.isSuccess == true) {
                    val loginData = apiResponse.result
                    if (loginData != null) {
                        // 토큰 및 사용자 정보 저장
                        saveTokens(loginData.accessToken, loginData.refreshToken ?: "")
                        saveUserData(
                            loginData.userId,
                            loginData.email ?: "",
                            loginData.name ?: "",
                            loginData.ribuddyId ?: ""
                        )
                        Log.d("AuthRepository", "로그인 성공 및 토큰 저장 완료")
                        Result.success(loginData)
                    } else {
                        Log.e("AuthRepository", "로그인 데이터가 null입니다.")
                        Result.failure(Exception("로그인 데이터를 받지 못했습니다."))
                    }
                } else {
                    val errorMsg = apiResponse?.message ?: "알 수 없는 오류"
                    Log.e("AuthRepository", "서버 응답 실패: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorMsg = "서버 오류: ${response.code()}"
                Log.e("AuthRepository", errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "네트워크 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 토큰 저장
     */
    private suspend fun saveTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { preferences ->
            preferences[DataStoreManager.ACCESS_TOKEN_KEY] = accessToken
            preferences[DataStoreManager.REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    /**
     * 사용자 정보 저장
     */
    private suspend fun saveUserData(userId: String, email: String, name: String, ribuddyId: String) {
        dataStore.edit { preferences ->
            preferences[DataStoreManager.USER_ID_KEY] = userId
            preferences[DataStoreManager.USER_EMAIL_KEY] = email
            preferences[DataStoreManager.USER_NAME_KEY] = name
            preferences[DataStoreManager.RIBUDDY_ID_KEY] = ribuddyId
        }
    }

    /**
     * 액세스 토큰 조회
     */
    fun getAccessToken(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[DataStoreManager.ACCESS_TOKEN_KEY]
        }
    }

    /**
     * 리프레시 토큰 조회
     */
    fun getRefreshToken(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[DataStoreManager.REFRESH_TOKEN_KEY]
        }
    }

    /**
     * 사용자 정보 조회
     */
    fun getUserData(): Flow<UserData?> {
        return dataStore.data.map { preferences ->
            val userId = preferences[DataStoreManager.USER_ID_KEY]
            val email = preferences[DataStoreManager.USER_EMAIL_KEY]
            val name = preferences[DataStoreManager.USER_NAME_KEY]
            val ribuddyId = preferences[DataStoreManager.RIBUDDY_ID_KEY]

            if (userId != null) {
                UserData(userId, email, name, ribuddyId = ribuddyId)
            } else {
                null
            }
        }
    }

    /**
     * 서버에서 내 정보 조회 (최신 정보)
     */
    suspend fun fetchMyInfo(): Result<UserInfo> {
        return try {
            val token = dataStore.data.first()[DataStoreManager.ACCESS_TOKEN_KEY]
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("로그인이 필요합니다."))
            }

            Log.d("AuthRepository", "내 정보 조회 요청")
            val response = authApi.getMyInfo("Bearer $token")

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val userInfo = response.body()?.result
                if (userInfo != null) {
                    // DataStore에도 최신 정보 저장
                    saveUserData(
                        userInfo.id,
                        "",  // email은 API에 없음
                        userInfo.name,
                        userInfo.ribuddyId
                    )
                    Log.d("AuthRepository", "내 정보 조회 성공: ${userInfo.ribuddyId}")
                    Result.success(userInfo)
                } else {
                    Result.failure(Exception("사용자 정보를 받지 못했습니다."))
                }
            } else {
                val errorMsg = response.body()?.message ?: "사용자 정보 조회에 실패했습니다."
                Log.e("AuthRepository", "내 정보 조회 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "내 정보 조회 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 로그아웃 (토큰 삭제)
     */
    suspend fun logout() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
