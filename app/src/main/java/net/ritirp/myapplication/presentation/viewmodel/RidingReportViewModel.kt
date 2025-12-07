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
) : ViewModel() {
    val records: StateFlow<List<RidingRecordEntity>> =
        localRidingRecordRepository
            .getCompletedRecords()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    private val _uiState = MutableStateFlow(RidingReportUiState())
    val uiState: StateFlow<RidingReportUiState> = _uiState.asStateFlow()

    init {
        loadReportData()
    }

    fun deleteRecord(record: RidingRecordEntity) {
        viewModelScope.launch {
            localRidingRecordRepository.deleteRecord(record)
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

            // 현재 기록들을 기반으로 통계 계산
            val currentRecords = records.value
            val summary = calculateSummary(currentRecords, _uiState.value.periodFilter)
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

    private fun calculateSummary(records: List<RidingRecordEntity>, filter: PeriodFilter): RidingSummary {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        val filteredRecords = records.filter { record ->
            val recordTime = record.startTime.time
            when (filter) {
                PeriodFilter.WEEK -> now - recordTime <= 7 * 24 * 60 * 60 * 1000L
                PeriodFilter.MONTH -> now - recordTime <= 30 * 24 * 60 * 60 * 1000L
                PeriodFilter.YEAR -> now - recordTime <= 365 * 24 * 60 * 60 * 1000L
            }
        }

        val totalDistanceKm = filteredRecords.sumOf { it.distanceMeters / 1000 }
        val totalDurationMinutes = filteredRecords.sumOf { it.durationMillis / 60000 }

        // 요일별 거리 계산
        val dayNames = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
        val dailyDistances = dayNames.mapIndexed { index, dayName ->
            val dayRecords = filteredRecords.filter { record ->
                val cal = Calendar.getInstance()
                cal.time = record.startTime
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                // Calendar.MONDAY = 2, 따라서 index 조정
                val adjustedIndex = if (index == 6) 1 else index + 2
                dayOfWeek == adjustedIndex
            }
            DailyDistance(
                dayOfWeek = dayName,
                distanceKm = dayRecords.sumOf { it.distanceMeters / 1000 },
            )
        }

        return RidingSummary(
            totalDistanceKm = totalDistanceKm,
            totalDurationMinutes = totalDurationMinutes,
            dailyDistances = dailyDistances,
        )
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
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RidingReportViewModel::class.java)) {
            return RidingReportViewModel(localRidingRecordRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
