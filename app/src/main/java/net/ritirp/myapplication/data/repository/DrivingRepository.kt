package net.ritirp.myapplication.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.ritirp.myapplication.data.api.DrivingApi
import net.ritirp.myapplication.data.local.DataStoreManager
import net.ritirp.myapplication.data.model.*

/**
 * 라이딩 Repository
 */
class DrivingRepository(
    private val drivingApi: DrivingApi,
    context: Context,
) {
    private val dataStore = DataStoreManager.getDataStore(context)

    private val _ridingStatus = MutableStateFlow(RidingStatus.IDLE)
    val ridingStatus: StateFlow<RidingStatus> = _ridingStatus.asStateFlow()

    private val _currentRidingRecordId = MutableStateFlow<String?>(null)
    val currentRidingRecordId: StateFlow<String?> = _currentRidingRecordId.asStateFlow()

    private val _teamMemberLocations = MutableStateFlow<List<TeamMemberLocation>>(emptyList())
    val teamMemberLocations: StateFlow<List<TeamMemberLocation>> = _teamMemberLocations.asStateFlow()

    init {
        // 앱 시작 시 DataStore에서 이전 라이딩 기록 ID 복원
        CoroutineScope(Dispatchers.IO).launch {
            val savedId = getCurrentRidingRecordIdFromStore()
            if (savedId != null) {
                _currentRidingRecordId.value = savedId
                _ridingStatus.value = RidingStatus.RIDING
                Log.d("DrivingRepository", "이전 라이딩 기록 복원: $savedId")
            }
        }
    }

    /**
     * Access Token 가져오기
     */
    private suspend fun getAccessToken(): String {
        return dataStore.data.map { preferences ->
            preferences[DataStoreManager.ACCESS_TOKEN_KEY] ?: ""
        }.first()
    }

    /**
     * DataStore에서 현재 라이딩 기록 ID 가져오기
     */
    suspend fun getCurrentRidingRecordIdFromStore(): String? {
        return dataStore.data.map { preferences ->
            preferences[DataStoreManager.CURRENT_RIDING_RECORD_ID_KEY]
        }.first()
    }

    /**
     * DataStore에 현재 라이딩 기록 ID 저장
     */
    private suspend fun saveCurrentRidingRecordId(ridingRecordId: String?) {
        dataStore.edit { preferences ->
            if (ridingRecordId != null) {
                preferences[DataStoreManager.CURRENT_RIDING_RECORD_ID_KEY] = ridingRecordId
            } else {
                preferences.remove(DataStoreManager.CURRENT_RIDING_RECORD_ID_KEY)
            }
        }
    }

    /**
     * 팀 라이딩 시작
     */
    suspend fun startTeamRiding(
        teamId: String,
        lat: Double,
        lon: Double,
        ele: Double? = null,
        name: String? = null,
    ): Result<String> {
        return try {
            val token = "Bearer ${getAccessToken()}"
            val request = StartTeamRidingRequest(lat, lon, ele, name)

            val response = drivingApi.startTeamRiding(token, teamId, request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val ridingRecordId = response.body()?.result ?: ""
                _currentRidingRecordId.value = ridingRecordId
                _ridingStatus.value = RidingStatus.RIDING
                // DataStore에 영구 저장
                saveCurrentRidingRecordId(ridingRecordId)
                Log.d("DrivingRepository", "라이딩 시작 성공: $ridingRecordId (DataStore에 저장)")
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
        ele: Double? = null,
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
        name: String? = null,
    ): Result<Unit> {
        return try {
            Log.d("DrivingRepository", "라이딩 종료 시도: ridingRecordId=$ridingRecordId")

            val token = "Bearer ${getAccessToken()}"
            val request = EndTeamRidingRequest(lat, lon, ele, name)

            val response = drivingApi.endTeamRiding(token, ridingRecordId, request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                // 성공 시 상태 초기화
                _currentRidingRecordId.value = null
                _ridingStatus.value = RidingStatus.ENDED
                _teamMemberLocations.value = emptyList()
                // DataStore에서도 제거
                saveCurrentRidingRecordId(null)
                Log.d("DrivingRepository", "라이딩 종료 성공 (DataStore에서 제거)")
                Result.success(Unit)
            } else {
                val errorCode = response.code()
                val errorMsg = response.body()?.message ?: "라이딩 종료 실패"
                val apiCode = response.body()?.code ?: ""

                Log.e("DrivingRepository", "라이딩 종료 실패: HTTP $errorCode (API Code: $apiCode) - $errorMsg")

                // 404 에러인 경우 (기록이 존재하지 않음) - 클라이언트 상태 정리
                if (errorCode == 404) {
                    Log.w("DrivingRepository", "서버에 기록이 없으므로 클라이언트 상태를 정리합니다.")
                    _currentRidingRecordId.value = null
                    _ridingStatus.value = RidingStatus.IDLE
                    _teamMemberLocations.value = emptyList()
                    saveCurrentRidingRecordId(null)

                    return Result.failure(Exception("해당 주행 기록을 찾을 수 없습니다. 이미 종료되었거나 존재하지 않는 기록입니다."))
                }

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
    suspend fun resetRidingStatus() {
        _ridingStatus.value = RidingStatus.IDLE
        _currentRidingRecordId.value = null
        _teamMemberLocations.value = emptyList()
        // DataStore에서도 제거
        saveCurrentRidingRecordId(null)
        Log.d("DrivingRepository", "라이딩 상태 초기화 완료")
    }
}
