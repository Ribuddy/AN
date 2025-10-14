package net.ritirp.myapplication.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.ritirp.myapplication.data.model.SensitivityLevel

private val Context.crashSettingsDataStore by preferencesDataStore(name = "crash_settings")

/**
 * 사고 감지 설정 저장소
 */
class CrashSettingsRepository(private val context: Context) {

    companion object {
        private val SENSITIVITY_KEY = stringPreferencesKey("sensitivity_level")
        private val DETECTION_ENABLED_KEY = stringPreferencesKey("detection_enabled")
    }

    val sensitivityLevel: Flow<SensitivityLevel> = context.crashSettingsDataStore.data
        .map { preferences ->
            val levelName = preferences[SENSITIVITY_KEY] ?: SensitivityLevel.MEDIUM.name
            try {
                SensitivityLevel.valueOf(levelName)
            } catch (e: IllegalArgumentException) {
                SensitivityLevel.MEDIUM
            }
        }

    val isDetectionEnabled: Flow<Boolean> = context.crashSettingsDataStore.data
        .map { preferences ->
            preferences[DETECTION_ENABLED_KEY]?.toBoolean() ?: true
        }

    suspend fun setSensitivityLevel(level: SensitivityLevel) {
        context.crashSettingsDataStore.edit { preferences ->
            preferences[SENSITIVITY_KEY] = level.name
        }
    }

    suspend fun setDetectionEnabled(enabled: Boolean) {
        context.crashSettingsDataStore.edit { preferences ->
            preferences[DETECTION_ENABLED_KEY] = enabled.toString()
        }
    }
}
