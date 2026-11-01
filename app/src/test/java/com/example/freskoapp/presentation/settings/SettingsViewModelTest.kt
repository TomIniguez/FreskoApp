package com.example.freskoapp.presentation.settings

import java.time.LocalTime
import com.example.freskoapp.domain.repository.DEFAULT_ALERT_TIME
import com.example.freskoapp.domain.usecase.SetAlertTimeUseCase
import com.example.freskoapp.domain.FakeEventRepository
import com.example.freskoapp.domain.FakeSettingsRepository
import com.example.freskoapp.domain.usecase.SetWarningDaysUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest {
    private val settings = FakeSettingsRepository(warningDays = 5)
    private val rescheduled = mutableListOf<LocalTime>()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = SettingsViewModel(settings, SetWarningDaysUseCase(settings), FakeEventRepository(), SetAlertTimeUseCase(settings) { rescheduled += it })

    @Test
    fun exposesRepositoryValue() = runTest {
        val vm = viewModel()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.warningDays.collect {} }
        assertEquals(5, vm.warningDays.value)
        job.cancel()
    }

    @Test
    fun selectingDaysUpdatesRepository() = runTest {
        val vm = viewModel()
        vm.onWarningDaysSelected(7)
        assertEquals(7, settings.warningDaysState.value)
    }

    @Test
    fun pickingAnAlertTimeStoresItAndMovesTheDailyCheck() = runTest {
        val vm = viewModel()
        assertEquals(DEFAULT_ALERT_TIME, vm.alertTime.value)
        vm.onAlertTimeSelected(LocalTime.of(20, 30, 15))
        assertEquals(LocalTime.of(20, 30), settings.alertTimeState.value) // minute precision
        assertEquals(listOf(LocalTime.of(20, 30)), rescheduled)
    }
}
