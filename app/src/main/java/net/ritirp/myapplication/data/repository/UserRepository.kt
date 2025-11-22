package net.ritirp.myapplication.data.repository

import android.content.Context
import android.util.Log
import net.ritirp.myapplication.data.api.RetrofitClient
import net.ritirp.myapplication.data.model.UserProfile

/**
 * 사용자 정보 관련 Repository
 */
class UserRepository(
    @Suppress("UNUSED_PARAMETER") private val context: Context,
) {
    private val userApi = RetrofitClient.userApi

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    suspend fun getMyProfile(): Result<UserProfile> {
        return try {
            Log.d("UserRepository", "내 프로필 정보 조회 시작")
            val response = userApi.getMyProfile()

            Log.d("UserRepository", "응답 코드: ${response.code()}")

            if (response.isSuccessful) {
                val apiResponse = response.body()
                Log.d("UserRepository", "응답 성공: ${apiResponse?.isSuccess}")

                if (apiResponse?.isSuccess == true) {
                    val userData = apiResponse.result
                    Log.d("UserRepository", "API 응답 데이터: id=${userData?.id}, name=${userData?.name}, ribuddyId=${userData?.ribuddyId}")
                    if (userData != null) {
                        val profile =
                            UserProfile(
                                id = userData.id,
                                name = userData.name,
                                nickname = userData.nickname,
                                oneLineIntroduction = userData.introduction,
                                ribuddyId = userData.ribuddyId,
                                profileImage = userData.profileImageUrl,
                            )
                        Log.d("UserRepository", "프로필 조회 성공: ${profile.name} (@${profile.ribuddyId})")
                        Log.d("UserRepository", "UserProfile 생성 완료: id=${profile.id}, name=${profile.name}, ribuddyId=${profile.ribuddyId}")
                        Result.success(profile)
                    } else {
                        Log.e("UserRepository", "결과 데이터가 null입니다")
                        Result.failure(Exception("결과 데이터가 없습니다"))
                    }
                } else {
                    Log.e("UserRepository", "API 응답 실패: ${apiResponse?.message}")
                    Result.failure(Exception(apiResponse?.message ?: "프로필 조회 실패"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("UserRepository", "HTTP 에러: ${response.code()}, $errorBody")
                Result.failure(Exception("프로필 조회 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "프로필 조회 중 예외 발생", e)
            Result.failure(e)
        }
    }
}
