package net.ritirp.myapplication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ritirp.myapplication.data.model.SensitivityLevel
import net.ritirp.myapplication.data.repository.CrashSettingsRepository

/**
 * 사고 감지 설정 ViewModel
 */
class CrashSettingsViewModel(
    private val repository: CrashSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CrashSettingsUiState())
    val uiState: StateFlow<CrashSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            repository.sensitivityLevel.collect { level ->
                _uiState.value = _uiState.value.copy(sensitivityLevel = level)
            }
        }

        viewModelScope.launch {
            repository.isDetectionEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(isDetectionEnabled = enabled)
            }
        }
    }

    fun setSensitivity(level: SensitivityLevel) {
        viewModelScope.launch {
            repository.setSensitivityLevel(level)
        }
    }

    fun toggleDetection(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDetectionEnabled(enabled)
        }
    }
}

data class CrashSettingsUiState(
    val sensitivityLevel: SensitivityLevel = SensitivityLevel.MEDIUM,
    val isDetectionEnabled: Boolean = true
)

class CrashSettingsViewModelFactory(
    private val repository: CrashSettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CrashSettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CrashSettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
