package net.ritirp.myapplication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.ritirp.myapplication.data.local.RidingRecordEntity
import net.ritirp.myapplication.data.repository.RidingRecordRepository

/**
 * 주행 리포트 ViewModel
 */
class RidingReportViewModel(
    private val ridingRecordRepository: RidingRecordRepository,
) : ViewModel() {
    val records: StateFlow<List<RidingRecordEntity>> =
        ridingRecordRepository
            .getAllRecords()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            ridingRecordRepository.deleteRecord(id)
        }
    }

    fun deleteAllRecords() {
        viewModelScope.launch {
            ridingRecordRepository.deleteAllRecords()
        }
    }
}

/**
 * RidingReportViewModel Factory
 */
class RidingReportViewModelFactory(
    private val ridingRecordRepository: RidingRecordRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RidingReportViewModel::class.java)) {
            return RidingReportViewModel(ridingRecordRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
