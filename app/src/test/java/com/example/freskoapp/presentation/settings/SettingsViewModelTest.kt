package com.example.freskoapp.presentation.settings

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

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = SettingsViewModel(settings, SetWarningDaysUseCase(settings))

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
}
