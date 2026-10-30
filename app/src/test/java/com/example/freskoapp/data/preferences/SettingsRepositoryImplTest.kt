package com.example.freskoapp.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsRepositoryImplTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.dataStore() = PreferenceDataStoreFactory.create(
        scope = backgroundScope,
        produceFile = { File(tmp.root, "test.preferences_pb") },
    )

    @Test
    fun warningDaysDefaultsToThree() = runTest {
        assertEquals(3, SettingsRepositoryImpl(dataStore()).warningDays.first())
    }

    @Test
    fun setWarningDaysPersists() = runTest {
        val repo = SettingsRepositoryImpl(dataStore())
        repo.setWarningDays(5)
        assertEquals(5, repo.warningDays.first())
    }

    @Test
    fun outOfRangeStoredValueIsClamped() = runTest {
        val store = dataStore()
        val repo = SettingsRepositoryImpl(store)
        store.edit { it[intPreferencesKey("warning_days")] = 99 }
        assertEquals(7, repo.warningDays.first())
        store.edit { it[intPreferencesKey("warning_days")] = 0 }
        assertEquals(1, repo.warningDays.first())
    }

    @Test
    fun promptDismissedIsFalseThenTrue() = runTest {
        val repo = SettingsRepositoryImpl(dataStore())
        assertEquals(false, repo.notificationPromptDismissed.first())
        repo.dismissNotificationPrompt()
        assertEquals(true, repo.notificationPromptDismissed.first())
    }
}
