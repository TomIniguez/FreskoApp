package com.example.freskoapp.presentation.inventory

import com.example.freskoapp.domain.FakeSettingsRepository
import com.example.freskoapp.domain.model.ExpirationStatus
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.repository.ProductRepository
import com.example.freskoapp.domain.usecase.GetInventoryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class InventoryViewModelTest {
    private val repo = object : ProductRepository {
        override fun getProducts(): Flow<List<Product>> = flowOf(emptyList())
        override suspend fun addProduct(product: Product) = Unit
    }
    private val settings = FakeSettingsRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(initialStatus: ExpirationStatus? = null) =
        InventoryViewModel(GetInventoryUseCase(repo), settings, initialStatus)

    @Test
    fun initialStatusPreselectsFilter() {
        assertEquals(ExpirationStatus.EXPIRING_SOON, viewModel(ExpirationStatus.EXPIRING_SOON).filter.value.status)
    }

    @Test
    fun noInitialStatusLeavesFilterEmpty() {
        assertNull(viewModel().filter.value.status)
    }

    @Test
    fun selectingSameChipClearsIt() {
        val vm = viewModel(ExpirationStatus.EXPIRING_SOON)
        vm.onStatusSelected(ExpirationStatus.EXPIRING_SOON)
        assertNull(vm.filter.value.status)
    }

    @Test
    fun dismissedFlagComesFromSettings() = runTest {
        val vm = viewModel()
        // WhileSubscribed: collect so the stateIn upstream is active.
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.notificationPromptDismissed.collect {} }
        assertEquals(false, vm.notificationPromptDismissed.value)
        vm.dismissNotificationPrompt()
        assertEquals(true, vm.notificationPromptDismissed.value)
        job.cancel()
    }
}
