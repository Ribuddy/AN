package net.ritirp.myapplication.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * DataStore 싱글톤
 * AuthRepository와 TeamRepository에서 공유
 */

// DataStore 인스턴스 (싱글톤)
private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

object DataStoreManager {
    val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
    val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    val USER_ID_KEY = stringPreferencesKey("user_id")
    val USER_EMAIL_KEY = stringPreferencesKey("user_email")
    val USER_NAME_KEY = stringPreferencesKey("user_name")
    val RIBUDDY_ID_KEY = stringPreferencesKey("ribuddy_id")

    fun getDataStore(context: Context): DataStore<Preferences> {
        return context.authDataStore
    }
}
