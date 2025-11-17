package net.ritirp.myapplication.data.model

import com.google.gson.annotations.SerializedName

/**
 * 인증 관련 데이터 모델
 */

// 백엔드 공통 응답 형식
data class ApiResponse<T>(
    val isSuccess: Boolean,
    val code: String?,
    val message: String?,
    val result: T?,
)

// 로그인 응답 데이터
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val userId: String,
    val email: String? = null,
    val name: String? = null,
    val ribuddyId: String? = null,
)

// 사용자 정보
data class UserData(
    val userId: String,
    val email: String? = null,
    val name: String? = null,
    val profileImageUrl: String? = null,
    val ribuddyId: String? = null,
)

// 사용자 상세 정보 (API 응답)
data class UserInfo(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("nickname") val nickname: String? = null,
    @SerializedName("oneLineIntroduction") val oneLineIntroduction: String? = null,
    @SerializedName("ribuddyId") val ribuddyId: String,
    @SerializedName("profileImage") val profileImage: String? = null,
)

// 인증 상태
sealed class AuthState {
    object Idle : AuthState()

    object Loading : AuthState()

    data class Success(val user: UserData) : AuthState()

    data class Error(val message: String) : AuthState()
}
