package net.ritirp.myapplication.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.first
import net.ritirp.myapplication.data.api.RetrofitClient
import net.ritirp.myapplication.data.local.DataStoreManager
import net.ritirp.myapplication.data.model.*

/**
 * 팀 관련 Repository
 */
class TeamRepository(private val context: Context) {
    private val teamApi = RetrofitClient.teamApi
    private val dataStore = DataStoreManager.getDataStore(context)

    /**
     * 저장된 토큰 가져오기
     */
    private suspend fun getAccessToken(): String? {
        return dataStore.data.first()[DataStoreManager.ACCESS_TOKEN_KEY]
    }

    /**
     * 팀 생성
     */
    suspend fun createTeam(
        name: String,
        description: String? = null,
        members: List<String>,
        isCrew: Boolean = false
    ): Result<String> {
        return try {
            val token = getAccessToken()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("로그인이 필요합니다."))
            }

            val request = CreateTeamRequest(
                name = name,
                description = description,
                members = members,
                isCrew = isCrew
            )

            Log.d("TeamRepository", "팀 생성 요청: name=$name, members=${members.size}명")
            val response = teamApi.createTeam("Bearer $token", request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val teamId = response.body()?.result ?: ""  // result가 직접 String
                Log.d("TeamRepository", "팀 생성 성공: teamId=$teamId")
                Result.success(teamId)
            } else {
                val errorMsg = response.body()?.message ?: "팀 생성에 실패했습니다."
                Log.e("TeamRepository", "팀 생성 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("TeamRepository", "팀 생성 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 내가 속한 팀 목록 조회
     */
    suspend fun getTeamList(): Result<List<TeamInfo>> {
        return try {
            val token = getAccessToken()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("로그인이 필요합니다."))
            }

            Log.d("TeamRepository", "팀 목록 조회 요청")
            val response = teamApi.getTeamList("Bearer $token")

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val teams = response.body()?.result ?: emptyList()  // result가 직접 List
                Log.d("TeamRepository", "팀 목록 조회 성공: ${teams.size}개 팀")
                Result.success(teams)
            } else {
                val errorMsg = response.body()?.message ?: "팀 목록 조회에 실패했습니다."
                Log.e("TeamRepository", "팀 목록 조회 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("TeamRepository", "팀 목록 조회 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 팀 참여하기
     */
    suspend fun joinTeam(teamId: String): Result<Unit> {
        return try {
            val token = getAccessToken()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("로그인이 필요합니다."))
            }

            val request = JoinOrLeaveTeamRequest(id = teamId)
            Log.d("TeamRepository", "팀 참여 요청: teamId=$teamId")
            val response = teamApi.joinTeam("Bearer $token", request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Log.d("TeamRepository", "팀 참여 성공")
                Result.success(Unit)
            } else {
                val errorMsg = response.body()?.message ?: "팀 참여에 실패했습니다."
                Log.e("TeamRepository", "팀 참여 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("TeamRepository", "팀 참여 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 팀 탈퇴하기
     */
    suspend fun leaveTeam(teamId: String): Result<Unit> {
        return try {
            val token = getAccessToken()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("로그인이 필요합니다."))
            }

            val request = JoinOrLeaveTeamRequest(id = teamId)
            Log.d("TeamRepository", "팀 탈퇴 요청: teamId=$teamId")
            val response = teamApi.leaveTeam("Bearer $token", request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Log.d("TeamRepository", "팀 탈퇴 성공")
                Result.success(Unit)
            } else {
                val errorMsg = response.body()?.message ?: "팀 탈퇴에 실패했습니다."
                Log.e("TeamRepository", "팀 탈퇴 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("TeamRepository", "팀 탈퇴 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 팀 정보 조회
     */
    suspend fun getTeamInfo(teamId: String): Result<TeamInfo> {
        return try {
            val token = getAccessToken()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("로그인이 필요합니다."))
            }

            Log.d("TeamRepository", "팀 정보 조회 요청: teamId=$teamId")
            val response = teamApi.getTeamInfo("Bearer $token", teamId)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val teamInfo = response.body()?.result
                if (teamInfo != null) {
                    Log.d("TeamRepository", "팀 정보 조회 성공: ${teamInfo.name}")
                    Result.success(teamInfo)
                } else {
                    Result.failure(Exception("팀 정보를 찾을 수 없습니다."))
                }
            } else {
                val errorMsg = response.body()?.message ?: "팀 정보 조회에 실패했습니다."
                Log.e("TeamRepository", "팀 정보 조회 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("TeamRepository", "팀 정보 조회 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 팀 참여 코드 조회
     */
    suspend fun getTeamJoinCode(teamId: String): Result<String> {
        return try {
            val token = getAccessToken()
            if (token.isNullOrEmpty()) {
                return Result.failure(Exception("로그인이 필요합니다."))
            }

            Log.d("TeamRepository", "팀 참여 코드 조회: teamId=$teamId")
            val response = teamApi.getTeamJoinCode("Bearer $token", teamId)

            Log.d("TeamRepository", "응답 코드: ${response.code()}")
            Log.d("TeamRepository", "응답 바디: ${response.body()}")

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val joinCode = response.body()?.result ?: ""
                Log.d("TeamRepository", "팀 참여 코드 조회 성공: $joinCode")
                Result.success(joinCode)
            } else {
                val errorMsg = response.body()?.message ?: "참여 코드 조회에 실패했습니다."
                Log.e("TeamRepository", "참여 코드 조회 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("TeamRepository", "참여 코드 조회 오류", e)
            Result.failure(e)
        }
    }
}
