package com.example.freskoapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.freskoapp.domain.repository.SettingsRepository
import com.example.freskoapp.domain.usecase.DEFAULT_WARNING_DAYS
import com.example.freskoapp.domain.usecase.MAX_WARNING_DAYS
import com.example.freskoapp.domain.usecase.MIN_WARNING_DAYS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Single DataStore instance per file: this delegate must be the only one for "fresko_settings".
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "fresko_settings")

class SettingsRepositoryImpl(private val dataStore: DataStore<Preferences>) : SettingsRepository {
    private val data: Flow<Preferences> = dataStore.data.catch {
        if (it is IOException) emit(emptyPreferences()) else throw it
    }

    override val warningDays: Flow<Int> = data.map {
        (it[WARNING_DAYS] ?: DEFAULT_WARNING_DAYS).coerceIn(MIN_WARNING_DAYS, MAX_WARNING_DAYS)
    }

    override val notificationPromptDismissed: Flow<Boolean> = data.map { it[PROMPT_DISMISSED] ?: false }

    override suspend fun setWarningDays(days: Int) {
        dataStore.edit { it[WARNING_DAYS] = days }
    }

    override suspend fun dismissNotificationPrompt() {
        dataStore.edit { it[PROMPT_DISMISSED] = true }
    }

    private companion object {
        val WARNING_DAYS = intPreferencesKey("warning_days")
        val PROMPT_DISMISSED = booleanPreferencesKey("notification_prompt_dismissed")
    }
}
