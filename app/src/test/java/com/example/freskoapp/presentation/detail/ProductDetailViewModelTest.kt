package com.example.freskoapp.presentation.detail

import com.example.freskoapp.domain.FakeProductRepository
import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.domain.usecase.GetProductDetailUseCase
import com.example.freskoapp.domain.usecase.CloseUnitsUseCase
import com.example.freskoapp.presentation.common.ProductRemoval
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ProductDetailViewModelTest {
    private val repo = FakeProductRepository(
        listOf(
            Product(1, "Leche", LocalDate.of(2026, 12, 1), 2, Location.HELADERA),
            Product(2, "Queso", LocalDate.of(2026, 12, 5), 1, Location.HELADERA, ProductStatus.DISCARDED),
        )
    )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(id: Long) =
        ProductDetailViewModel(id, GetProductDetailUseCase(repo), CloseUnitsUseCase(repo))

    // WhileSubscribed: collect so the stateIn upstream is active.
    private fun kotlinx.coroutines.test.TestScope.observe(vm: ProductDetailViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
    }

    @Test
    fun loadsProductContent() = runTest {
        val vm = viewModel(1)
        observe(vm)
        assertFalse(vm.state.value.isLoading)
        assertEquals("Leche", vm.state.value.item?.product?.name)
        assertFalse(vm.state.value.notAvailable)
    }

    @Test
    fun markConsumedSetsRemovalAndUpdatesRepository() = runTest {
        val vm = viewModel(1)
        observe(vm)
        vm.markConsumed(2)
        assertEquals(ProductRemoval(1, "Leche", ProductStatus.CONSUMED, 2, true), vm.state.value.removal)
        assertEquals(ProductStatus.CONSUMED, repo.getProduct(1).first()?.status)
        assertFalse(vm.state.value.notAvailable)
    }

    @Test
    fun partialConsumeKeepsTheProductActiveAndRecordsHowToUndo() = runTest {
        val vm = viewModel(1)
        observe(vm)
        vm.markConsumed(1)
        assertEquals(ProductRemoval(1, "Leche", ProductStatus.CONSUMED, 1, false), vm.state.value.removal)
        assertEquals(ProductStatus.ACTIVE, repo.getProduct(1).first()?.status)
        assertEquals(1, repo.getProduct(1).first()?.quantity)
    }

    @Test
    fun secondCallDoesNothing() = runTest {
        val vm = viewModel(1)
        observe(vm)
        vm.markConsumed(2)
        vm.markDiscarded()
        assertEquals(ProductStatus.CONSUMED, repo.getProduct(1).first()?.status)
        assertEquals(ProductStatus.CONSUMED, vm.state.value.removal?.status)
    }

    @Test
    fun unknownIdIsNotAvailable() = runTest {
        val vm = viewModel(99)
        observe(vm)
        assertTrue(vm.state.value.notAvailable)
    }

    @Test
    fun alreadyDiscardedProductIsNotAvailableAndButtonsAreNoOps() = runTest {
        val vm = viewModel(2)
        observe(vm)
        assertTrue(vm.state.value.notAvailable)
        vm.markConsumed(1)
        assertNull(vm.state.value.removal)
        assertEquals(ProductStatus.DISCARDED, repo.getProduct(2).first()?.status)
        assertNotNull(vm.state.value.item)
    }
}
