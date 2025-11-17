package net.ritirp.myapplication.data.api

import net.ritirp.myapplication.data.model.ApiResponse
import net.ritirp.myapplication.data.model.LoginResponse
import net.ritirp.myapplication.data.model.UserInfo
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * 인증 관련 API 인터페이스
 */
interface AuthApi {
    companion object {
//        const val BASE_URL = "https://ribuddy.kyeoungwoon.kr/"
        const val BASE_URL = "http://10.210.60.65:7777/"
    }

    /**
     * Google ID 토큰 검증
     * 클라이언트에서 얻은 Google ID 토큰을 서버로 보내 유효성을 검증하고,
     * 서버의 JWT 토큰을 발급받습니다.
     */
    @POST("v2/auth/google/login")
    suspend fun verifyGoogleToken(
        @Body request: Map<String, String>,
    ): Response<ApiResponse<LoginResponse>>

    /**
     * 내 정보 조회
     */
    @GET("v1/users/me")
    suspend fun getMyInfo(
        @Header("Authorization") token: String,
    ): Response<ApiResponse<UserInfo>>
}
