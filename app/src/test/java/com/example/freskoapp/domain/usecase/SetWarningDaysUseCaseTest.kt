package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.FakeSettingsRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SetWarningDaysUseCaseTest {
    private val repo = FakeSettingsRepository()
    private val useCase = SetWarningDaysUseCase(repo)

    @Test
    fun validValueReachesRepository() = runTest {
        useCase(5)
        assertEquals(5, repo.warningDaysState.value)
    }

    @Test
    fun rejectsValuesOutsideRange() = runTest {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { useCase(0) } }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { useCase(8) } }
        assertEquals(DEFAULT_WARNING_DAYS, repo.warningDaysState.value)
    }
}
