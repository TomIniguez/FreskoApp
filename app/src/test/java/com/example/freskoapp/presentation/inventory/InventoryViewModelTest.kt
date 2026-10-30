package com.example.freskoapp.presentation.inventory

import androidx.lifecycle.SavedStateHandle
import com.example.freskoapp.domain.FakeProductRepository
import com.example.freskoapp.domain.FakeSettingsRepository
import com.example.freskoapp.domain.model.ExpirationStatus
import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.domain.usecase.GetInventoryUseCase
import com.example.freskoapp.domain.usecase.RestoreProductUseCase
import com.example.freskoapp.presentation.common.ProductRemoval
import com.example.freskoapp.presentation.navigation.putRemoval
import kotlinx.coroutines.Dispatchers
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
import java.time.LocalDate

class InventoryViewModelTest {
    private val repo = FakeProductRepository()
    private val settings = FakeSettingsRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(initialStatus: ExpirationStatus? = null, handle: SavedStateHandle = SavedStateHandle()) =
        InventoryViewModel(GetInventoryUseCase(repo), settings, RestoreProductUseCase(repo), handle, initialStatus)

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

    @Test
    fun removalPostedInTheHandleIsExposedAndCleared() = runTest {
        val handle = SavedStateHandle()
        val vm = viewModel(handle = handle)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.pendingRemoval.collect {} }
        assertNull(vm.pendingRemoval.value)
        val removal = ProductRemoval(1, "Leche", ProductStatus.CONSUMED)
        handle.putRemoval(removal)
        assertEquals(removal, vm.pendingRemoval.value)
        vm.onRemovalShown()
        assertNull(vm.pendingRemoval.value)
    }

    @Test
    fun undoRemovalPutsTheProductBackInItems() = runTest {
        repo.products.value = listOf(
            Product(1, "Leche", LocalDate.of(2026, 12, 1), 1, Location.HELADERA, ProductStatus.CONSUMED)
        )
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.items.collect {} }
        assertEquals(emptyList<Long>(), vm.items.value.map { it.product.id })
        vm.undoRemoval(ProductRemoval(1, "Leche", ProductStatus.CONSUMED))
        assertEquals(listOf(1L), vm.items.value.map { it.product.id })
    }
}
