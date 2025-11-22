package net.ritirp.myapplication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.ritirp.myapplication.data.local.entity.RidingRecordEntity
import net.ritirp.myapplication.data.repository.LocalRidingRecordRepository

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

    fun deleteRecord(record: RidingRecordEntity) {
        viewModelScope.launch {
            localRidingRecordRepository.deleteRecord(record)
        }
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
