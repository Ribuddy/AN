package net.ritirp.myapplication.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import net.ritirp.myapplication.data.api.DrivingApi
import net.ritirp.myapplication.data.local.DataStoreManager
import net.ritirp.myapplication.data.model.*

/**
 * 라이딩 Repository
 */
class DrivingRepository(
    private val drivingApi: DrivingApi,
    context: Context
) {
    private val dataStore = DataStoreManager.getDataStore(context)

    private val _ridingStatus = MutableStateFlow(RidingStatus.IDLE)
    val ridingStatus: StateFlow<RidingStatus> = _ridingStatus.asStateFlow()

    private val _currentRidingRecordId = MutableStateFlow<String?>(null)
    val currentRidingRecordId: StateFlow<String?> = _currentRidingRecordId.asStateFlow()

    private val _teamMemberLocations = MutableStateFlow<List<TeamMemberLocation>>(emptyList())
    val teamMemberLocations: StateFlow<List<TeamMemberLocation>> = _teamMemberLocations.asStateFlow()

    /**
     * Access Token 가져오기
     */
    private suspend fun getAccessToken(): String {
        return dataStore.data.map { preferences ->
            preferences[DataStoreManager.ACCESS_TOKEN_KEY] ?: ""
        }.first()
    }

    /**
     * 팀 라이딩 시작
     */
    suspend fun startTeamRiding(
        teamId: String,
        lat: Double,
        lon: Double,
        ele: Double? = null,
        name: String? = null
    ): Result<String> {
        return try {
            val token = "Bearer ${getAccessToken()}"
            val request = StartTeamRidingRequest(lat, lon, ele, name)

            val response = drivingApi.startTeamRiding(token, teamId, request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val ridingRecordId = response.body()?.result ?: ""
                _currentRidingRecordId.value = ridingRecordId
                _ridingStatus.value = RidingStatus.RIDING
                Log.d("DrivingRepository", "라이딩 시작 성공: $ridingRecordId")
                Result.success(ridingRecordId)
            } else {
                val errorMsg = response.body()?.message ?: "라이딩 시작 실패"
                Log.e("DrivingRepository", "라이딩 시작 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("DrivingRepository", "라이딩 시작 오류: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 내 위치 업데이트 & 팀원 위치 조회
     */
    suspend fun updateLocationAndGetTeamLocations(
        ridingRecordId: String,
        lat: Double,
        lon: Double,
        ele: Double? = null
    ): Result<List<TeamMemberLocation>> {
        return try {
            val token = "Bearer ${getAccessToken()}"
            val request = LocationUpdateRequest(lat, lon, ele)

            val response = drivingApi.updateLocationAndGetTeamLocations(token, ridingRecordId, request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val locations = response.body()?.result ?: emptyList<TeamMemberLocation>()
                _teamMemberLocations.value = locations
                Log.d("DrivingRepository", "위치 업데이트 성공, 팀원 수: ${locations.size}")
                Result.success(locations)
            } else {
                val errorMsg = response.body()?.message ?: "위치 업데이트 실패"
                Log.e("DrivingRepository", "위치 업데이트 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("DrivingRepository", "위치 업데이트 오류: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 팀 라이딩 종료
     */
    suspend fun endTeamRiding(
        ridingRecordId: String,
        lat: Double,
        lon: Double,
        ele: Double? = null,
        name: String? = null
    ): Result<Unit> {
        return try {
            val token = "Bearer ${getAccessToken()}"
            val request = EndTeamRidingRequest(lat, lon, ele, name)

            val response = drivingApi.endTeamRiding(token, ridingRecordId, request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                _currentRidingRecordId.value = null
                _ridingStatus.value = RidingStatus.ENDED
                _teamMemberLocations.value = emptyList()
                Log.d("DrivingRepository", "라이딩 종료 성공")
                Result.success(Unit)
            } else {
                val errorMsg = response.body()?.message ?: "라이딩 종료 실패"
                Log.e("DrivingRepository", "라이딩 종료 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("DrivingRepository", "라이딩 종료 오류: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 라이딩 상태 초기화
     */
    fun resetRidingStatus() {
        _ridingStatus.value = RidingStatus.IDLE
        _currentRidingRecordId.value = null
        _teamMemberLocations.value = emptyList()
    }
}
