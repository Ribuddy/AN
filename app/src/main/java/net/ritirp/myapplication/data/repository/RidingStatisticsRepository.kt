package net.ritirp.myapplication.data.repository

import android.util.Log
import net.ritirp.myapplication.data.api.DrivingApi
import net.ritirp.myapplication.data.model.MonthlyStatisticsResponse
import net.ritirp.myapplication.data.model.MyRidingRecordsResponse
import net.ritirp.myapplication.data.model.RidingReportResponse
import net.ritirp.myapplication.data.model.WeeklyStatisticsResponse
import net.ritirp.myapplication.data.model.YearlyStatisticsResponse

/**
 * 주행 통계 Repository
 */
class RidingStatisticsRepository(
    private val drivingApi: DrivingApi,
) {
    /**
     * 주간 통계 조회
     * @param startDate 주의 시작일 (YYYY-MM-DD). null이면 이번 주 월요일
     */
    suspend fun getWeeklyStatistics(startDate: String? = null): Result<WeeklyStatisticsResponse> {
        return try {
            val response = drivingApi.getWeeklyStatistics(startDate)
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                val data = apiResponse.result
                if (data != null) {
                    Result.success(data)
                } else {
                    Result.failure(Exception("응답 데이터가 null입니다"))
                }
            } else {
                Result.failure(Exception("주간 통계 조회 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("RidingStatisticsRepository", "주간 통계 조회 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 월간 통계 조회
     * @param year 조회할 연도. null이면 올해
     */
    suspend fun getMonthlyStatistics(year: Int? = null): Result<MonthlyStatisticsResponse> {
        return try {
            val response = drivingApi.getMonthlyStatistics(year)
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                val data = apiResponse.result
                if (data != null) {
                    Result.success(data)
                } else {
                    Result.failure(Exception("응답 데이터가 null입니다"))
                }
            } else {
                Result.failure(Exception("월간 통계 조회 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("RidingStatisticsRepository", "월간 통계 조회 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 연간 통계 조회
     */
    suspend fun getYearlyStatistics(): Result<YearlyStatisticsResponse> {
        return try {
            val response = drivingApi.getYearlyStatistics()
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                val data = apiResponse.result
                if (data != null) {
                    Result.success(data)
                } else {
                    Result.failure(Exception("응답 데이터가 null입니다"))
                }
            } else {
                Result.failure(Exception("연간 통계 조회 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("RidingStatisticsRepository", "연간 통계 조회 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 주행 리포트 조회
     * @param ridingRecordId 조회할 주행 기록 ID
     */
    suspend fun getRidingReport(ridingRecordId: String): Result<RidingReportResponse> {
        return try {
            val response = drivingApi.getDrivingReport(ridingRecordId)
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                val data = apiResponse.result
                if (data != null) {
                    Result.success(data)
                } else {
                    Result.failure(Exception("주행 리포트 데이터가 null입니다"))
                }
            } else {
                Result.failure(Exception("주행 리포트 조회 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("RidingStatisticsRepository", "주행 리포트 조회 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 내 라이딩 기록 목록 조회
     */
    suspend fun getMyRidingRecords(): Result<MyRidingRecordsResponse> {
        return try {
            val response = drivingApi.getMyRidingRecords()
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                val data = apiResponse.result
                if (data != null) {
                    Result.success(data)
                } else {
                    Result.failure(Exception("라이딩 기록 목록 데이터가 null입니다"))
                }
            } else {
                Result.failure(Exception("라이딩 기록 목록 조회 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("RidingStatisticsRepository", "라이딩 기록 목록 조회 오류", e)
            Result.failure(e)
        }
    }
}
