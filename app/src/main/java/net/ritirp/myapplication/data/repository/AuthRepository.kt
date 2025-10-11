package net.ritirp.myapplication.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.ritirp.myapplication.data.api.RetrofitClient
import net.ritirp.myapplication.data.model.LoginResponse
import net.ritirp.myapplication.data.model.UserData

/**
 * 인증 관련 Repository
 */
class AuthRepository(private val context: Context) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
    }

    private val authApi = RetrofitClient.authApi

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

                if (apiResponse?.isSuccess == true && apiResponse.result != null) {
                    // 성공: 토큰 저장
                    Log.d("AuthRepository", "✅ 로그인 성공! JWT 토큰 저장 중...")
                    saveTokens(apiResponse.result)
                    Result.success(apiResponse.result)
                } else {
                    // API 응답은 받았지만 비즈니스 로직 실패
                    val errorMessage = apiResponse?.message ?: "로그인에 실패했습니다."
                    Log.e("AuthRepository", "❌ 비즈니스 로직 실패: $errorMessage")
                    Result.failure(Exception(errorMessage))
                }
            } else {
                // HTTP 에러
                val errorBody = response.errorBody()?.string()
                Log.e("AuthRepository", "❌ HTTP 에러: ${response.code()}")
                Log.e("AuthRepository", "에러 Body: $errorBody")
                Result.failure(Exception("서버 오류: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ 예외 발생: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 토큰 및 사용자 정보 저장
     */
    private suspend fun saveTokens(loginResponse: LoginResponse) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = loginResponse.accessToken
            loginResponse.refreshToken?.let {
                preferences[REFRESH_TOKEN_KEY] = it
            }
            preferences[USER_ID_KEY] = loginResponse.userId
            loginResponse.email?.let {
                preferences[USER_EMAIL_KEY] = it
            }
            loginResponse.name?.let {
                preferences[USER_NAME_KEY] = it
            }
        }
    }

    /**
     * 저장된 Access Token 가져오기
     */
    fun getAccessToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[ACCESS_TOKEN_KEY]
        }
    }

    /**
     * 저장된 사용자 정보 가져오기
     */
    fun getUserData(): Flow<UserData?> {
        return context.dataStore.data.map { preferences ->
            val userId = preferences[USER_ID_KEY]
            val email = preferences[USER_EMAIL_KEY]
            val name = preferences[USER_NAME_KEY]

            if (userId != null && email != null && name != null) {
                UserData(userId, email, name)
            } else {
                null
            }
        }
    }

    /**
     * 로그인 상태 확인
     */
    suspend fun isLoggedIn(): Boolean {
        var isLogged = false
        context.dataStore.data.map { preferences ->
            isLogged = preferences[ACCESS_TOKEN_KEY] != null
        }
        return isLogged
    }

    /**
     * 로그아웃 (토큰 삭제)
     */
    suspend fun logout() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
