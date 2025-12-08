package net.ritirp.myapplication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.ritirp.myapplication.data.local.entity.RidingRecordEntity
import net.ritirp.myapplication.data.model.DailyDistance
import net.ritirp.myapplication.data.model.ImprovementCategory
import net.ritirp.myapplication.data.model.ImprovementPoint
import net.ritirp.myapplication.data.model.PeriodFilter
import net.ritirp.myapplication.data.model.RidingScore
import net.ritirp.myapplication.data.model.RidingSummary
import net.ritirp.myapplication.data.model.ScoreFilter
import net.ritirp.myapplication.data.repository.LocalRidingRecordRepository
import java.util.Calendar

/**
 * 주행 리포트 UI 상태
 */
data class RidingReportUiState(
    val currentScore: Int = 87,
    val highScore: Int = 85,
    val periodFilter: PeriodFilter = PeriodFilter.WEEK,
    val scoreFilter: ScoreFilter = ScoreFilter.WEEK,
    val ridingSummary: RidingSummary = RidingSummary(),
    val ridingScore: RidingScore = RidingScore(),
    val improvementPoints: List<ImprovementPoint> = emptyList(),
    val isLoading: Boolean = false,
)

/**
 * 주행 리포트 ViewModel
 */
class RidingReportViewModel(
    private val localRidingRecordRepository: LocalRidingRecordRepository,
    private val ridingStatisticsRepository: net.ritirp.myapplication.data.repository.RidingStatisticsRepository,
) : ViewModel() {
    // 서버에서 가져온 주행 기록 목록 (UI 표시용)
    private val _records = MutableStateFlow<List<RidingRecordEntity>>(emptyList())
    val records: StateFlow<List<RidingRecordEntity>> = _records.asStateFlow()

    private val _uiState = MutableStateFlow(RidingReportUiState())
    val uiState: StateFlow<RidingReportUiState> = _uiState.asStateFlow()

    init {
        loadReportData()
        loadRecentRidingRecords()
    }

    /**
     * 서버에서 최근 주행 기록 목록을 가져오기
     */
    private fun loadRecentRidingRecords() {
        viewModelScope.launch {
            // 서버에서 내 라이딩 기록 목록 조회
            val result = ridingStatisticsRepository.getMyRidingRecords()
            if (result.isSuccess) {
                val summaryList = result.getOrNull() ?: emptyList()
                android.util.Log.d("RidingReportViewModel", "서버에서 받은 주행 기록 수: ${summaryList.size}")

                val serverRecords = summaryList.mapIndexed { index, summary ->
                    android.util.Log.d("RidingReportViewModel", "주행 기록 #$index: id=${summary.id}, status=${summary.status}")
                    RidingRecordEntity(
                        id = index.toLong(),
                        startTime = java.util.Date(), // 서버에서 시간 정보 없음, 임시로 현재 시간
                        endTime = if (summary.status == "COMPLETED") java.util.Date() else null,
                        durationMillis = 0L, // 서버에서 제공 안함
                        distanceMeters = 0.0, // 서버에서 제공 안함
                        averageSpeedKmh = 0.0,
                        maxSpeedKmh = 0.0,
                        totalClimbMeters = 0.0,
                        totalFallMeters = 0.0,
                        maxLeanAngleDegrees = 0.0,
                        startLocationName = null,
                        startLat = 0.0,
                        startLon = 0.0,
                        endLocationName = null,
                        endLat = null,
                        endLon = null,
                        isCompleted = summary.status == "COMPLETED",
                        isSyncedToServer = true,
                        serverRecordId = summary.id, // 서버의 "id" 필드 사용
                    )
                }
                _records.value = serverRecords
                android.util.Log.d("RidingReportViewModel", "주행 기록 ${serverRecords.size}개 로드 완료")
            } else {
                android.util.Log.e("RidingReportViewModel", "주행 기록 로드 실패: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    /**
     * 서버 응답을 RidingRecordEntity로 변환
     */
    private fun convertToEntity(
        report: net.ritirp.myapplication.data.model.RidingReportResponse,
        localId: Long,
    ): RidingRecordEntity {
        return RidingRecordEntity(
            id = localId,
            startTime = parseIsoTimestamp(report.startTime),
            endTime = report.endTime?.let { parseIsoTimestamp(it) },
            durationMillis = report.durationMillis,
            distanceMeters = report.distanceMeters,
            averageSpeedKmh = report.avgSpeedKmh,
            maxSpeedKmh = report.maxSpeedKmh,
            totalClimbMeters = report.totalClimbMeters,
            totalFallMeters = report.totalFallMeters,
            maxLeanAngleDegrees = report.maxLeanAngleDegrees,
            startLocationName = report.startLocation?.name,
            startLat = report.startLocation?.lat ?: 0.0,
            startLon = report.startLocation?.lon ?: 0.0,
            endLocationName = report.endLocation?.name,
            endLat = report.endLocation?.lat,
            endLon = report.endLocation?.lon,
            isCompleted = true,
            isSyncedToServer = true,
            serverRecordId = report.ridingRecordId,
        )
    }

    /**
     * ISO 8601 타임스탬프를 Date로 변환
     */
    private fun parseIsoTimestamp(timestamp: String): java.util.Date {
        return try {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            formatter.parse(timestamp) ?: java.util.Date()
        } catch (e: Exception) {
            java.util.Date()
        }
    }

    fun deleteRecord(record: RidingRecordEntity) {
        viewModelScope.launch {
            localRidingRecordRepository.deleteRecord(record)
        }
    }

    /**
     * 선택한 주행 기록의 상세 정보를 서버에서 가져오기
     */
    suspend fun loadRecordDetail(ridingRecordId: String): RidingRecordEntity? {
        val result = ridingStatisticsRepository.getRidingReport(ridingRecordId)
        return if (result.isSuccess) {
            val report = result.getOrNull()!!
            convertToEntity(report, 0L)
        } else {
            null
        }
    }

    fun setPeriodFilter(filter: PeriodFilter) {
        _uiState.value = _uiState.value.copy(periodFilter = filter)
        loadReportData()
    }

    fun setScoreFilter(filter: ScoreFilter) {
        _uiState.value = _uiState.value.copy(scoreFilter = filter)
        loadScoreData()
    }

    private fun loadReportData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // 서버 API에서 통계 데이터 가져오기
            val summary = when (_uiState.value.periodFilter) {
                PeriodFilter.WEEK -> fetchWeeklyStatistics()
                PeriodFilter.MONTH -> fetchMonthlyStatistics()
                PeriodFilter.YEAR -> fetchYearlyStatistics()
            }

            // 임시로 점수와 개선사항은 더미 데이터 사용
            val currentRecords = records.value
            val score = calculateScore(currentRecords)
            val improvements = calculateImprovements(currentRecords)

            _uiState.value = _uiState.value.copy(
                ridingSummary = summary,
                ridingScore = score,
                improvementPoints = improvements,
                isLoading = false,
            )
        }
    }

    private fun loadScoreData() {
        viewModelScope.launch {
            val currentRecords = records.value
            val score = calculateScore(currentRecords)
            _uiState.value = _uiState.value.copy(ridingScore = score)
        }
    }

    /**
     * 주간 통계를 서버에서 가져오기
     */
    private suspend fun fetchWeeklyStatistics(): RidingSummary {
        val result = ridingStatisticsRepository.getWeeklyStatistics()
        return if (result.isSuccess) {
            val data = result.getOrNull()!!
            val dailyDistances = data.dailyStats.map { stat ->
                DailyDistance(
                    label = stat.dayOfWeek,
                    distanceKm = stat.distance,
                )
            }
            RidingSummary(
                totalDistanceKm = data.totalDistance,
                totalDurationMinutes = data.totalDuration / 60, // 초를 분으로 변환
                dailyDistances = dailyDistances,
            )
        } else {
            // 실패 시 빈 데이터 반환
            RidingSummary()
        }
    }

    /**
     * 월간 통계를 서버에서 가져오기
     * 4주(Week 1~4)로 표시
     */
    private suspend fun fetchMonthlyStatistics(): RidingSummary {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val result = ridingStatisticsRepository.getMonthlyStatistics(year)

        return if (result.isSuccess) {
            val data = result.getOrNull()!!
            val currentMonth = calendar.get(Calendar.MONTH) + 1

            // 현재 월의 데이터 찾기
            val currentMonthData = data.monthlyStats.find {
                it.month == String.format("%04d-%02d", year, currentMonth)
            }

            // 월간 거리를 4주로 나누어 표시 (실제로는 서버에서 주별 데이터를 받아야 하지만, 임시로 균등 분할)
            val totalDistance = currentMonthData?.distance ?: 0.0
            val weeklyDistance = totalDistance / 4.0

            val dailyDistances = listOf(
                DailyDistance(label = "W1", distanceKm = weeklyDistance),
                DailyDistance(label = "W2", distanceKm = weeklyDistance),
                DailyDistance(label = "W3", distanceKm = weeklyDistance),
                DailyDistance(label = "W4", distanceKm = weeklyDistance),
            )

            RidingSummary(
                totalDistanceKm = data.totalDistance,
                totalDurationMinutes = data.totalDuration / 60, // 초를 분으로 변환
                dailyDistances = dailyDistances,
            )
        } else {
            // 실패 시 빈 4주 데이터 반환
            RidingSummary(
                totalDistanceKm = 0.0,
                totalDurationMinutes = 0,
                dailyDistances = listOf(
                    DailyDistance(label = "W1", distanceKm = 0.0),
                    DailyDistance(label = "W2", distanceKm = 0.0),
                    DailyDistance(label = "W3", distanceKm = 0.0),
                    DailyDistance(label = "W4", distanceKm = 0.0),
                ),
            )
        }
    }

    /**
     * 연간 통계를 서버에서 가져오기
     * 12개월(Jan~Dec)로 표시
     */
    private suspend fun fetchYearlyStatistics(): RidingSummary {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val result = ridingStatisticsRepository.getMonthlyStatistics(year)

        return if (result.isSuccess) {
            val data = result.getOrNull()!!

            // 월 이름 배열
            val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

            // 12개월 데이터 생성
            val dailyDistances = (1..12).map { month ->
                val monthStr = String.format("%04d-%02d", year, month)
                val monthData = data.monthlyStats.find { it.month == monthStr }
                DailyDistance(
                    label = monthNames[month - 1],
                    distanceKm = monthData?.distance ?: 0.0
                )
            }

            RidingSummary(
                totalDistanceKm = data.totalDistance,
                totalDurationMinutes = data.totalDuration / 60,
                dailyDistances = dailyDistances,
            )
        } else {
            // 실패 시 빈 12개월 데이터 반환
            val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            RidingSummary(
                totalDistanceKm = 0.0,
                totalDurationMinutes = 0,
                dailyDistances = monthNames.map { DailyDistance(label = it, distanceKm = 0.0) },
            )
        }
    }

    private fun calculateScore(records: List<RidingRecordEntity>): RidingScore {
        if (records.isEmpty()) {
            return RidingScore(
                totalScore = 70,
                operationSafetyScore = 52,
                speedSafetyScore = 52,
                leanStabilityScore = 52,
            )
        }

        // 속도 안전 점수 계산 (최고 속도 기반)
        val avgMaxSpeed = records.map { it.maxSpeedKmh }.average()
        val speedSafetyScore = when {
            avgMaxSpeed < 60 -> 90
            avgMaxSpeed < 80 -> 75
            avgMaxSpeed < 100 -> 60
            else -> 45
        }

        // 기울기 안정성 점수 계산
        val avgLeanAngle = records.map { it.maxLeanAngleDegrees }.average()
        val leanStabilityScore = when {
            avgLeanAngle < 20 -> 90
            avgLeanAngle < 35 -> 70
            avgLeanAngle < 45 -> 55
            else -> 40
        }

        // 조작 안전 점수 (급가속/급감속 기반 - 속도 변화량으로 추정)
        val operationSafetyScore = ((speedSafetyScore + leanStabilityScore) / 2).coerceIn(40, 95)

        // 전체 점수
        val totalScore = ((operationSafetyScore + speedSafetyScore + leanStabilityScore) / 3)

        return RidingScore(
            totalScore = totalScore,
            operationSafetyScore = operationSafetyScore,
            speedSafetyScore = speedSafetyScore,
            leanStabilityScore = leanStabilityScore,
        )
    }

    private fun calculateImprovements(records: List<RidingRecordEntity>): List<ImprovementPoint> {
        val improvements = mutableListOf<ImprovementPoint>()

        if (records.isEmpty()) {
            improvements.add(
                ImprovementPoint(
                    category = ImprovementCategory.OPERATION,
                    title = "조작 이상 10km당 6회 이상 발생",
                    description = "급감속 + 급가속 + 급정지 + 급출발",
                ),
            )
            improvements.add(
                ImprovementPoint(
                    category = ImprovementCategory.LEAN_STABILITY,
                    title = "좌측 기울기 45° 이상",
                    description = "급코너 위험",
                ),
            )
            return improvements
        }

        // 최대 기울기 분석
        val maxLeanAngle = records.maxOfOrNull { it.maxLeanAngleDegrees } ?: 0.0
        if (maxLeanAngle > 45) {
            improvements.add(
                ImprovementPoint(
                    category = ImprovementCategory.LEAN_STABILITY,
                    title = "좌측 기울기 ${maxLeanAngle.toInt()}° 이상",
                    description = "급코너 위험",
                ),
            )
        }

        // 속도 분석
        val maxSpeed = records.maxOfOrNull { it.maxSpeedKmh } ?: 0.0
        if (maxSpeed > 100) {
            improvements.add(
                ImprovementPoint(
                    category = ImprovementCategory.SPEED,
                    title = "최고 속도 ${maxSpeed.toInt()}km/h 기록",
                    description = "과속 주의 필요",
                ),
            )
        }

        // 기본 개선 포인트 추가
        if (improvements.isEmpty()) {
            improvements.add(
                ImprovementPoint(
                    category = ImprovementCategory.OPERATION,
                    title = "조작 이상 10km당 6회 이상 발생",
                    description = "급감속 + 급가속 + 급정지 + 급출발",
                ),
            )
        }

        return improvements
    }
}

/**
 * RidingReportViewModel Factory
 */
class RidingReportViewModelFactory(
    private val localRidingRecordRepository: LocalRidingRecordRepository,
    private val ridingStatisticsRepository: net.ritirp.myapplication.data.repository.RidingStatisticsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RidingReportViewModel::class.java)) {
            return RidingReportViewModel(localRidingRecordRepository, ridingStatisticsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
