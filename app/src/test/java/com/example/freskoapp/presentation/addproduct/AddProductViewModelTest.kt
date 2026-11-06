package com.example.freskoapp.presentation.addproduct

import androidx.lifecycle.SavedStateHandle
import com.example.freskoapp.presentation.navigation.START_SCAN_ARG

import com.example.freskoapp.domain.FakeEventRepository
import com.example.freskoapp.domain.NoTransaction
import com.example.freskoapp.domain.FakeProductInfoRepository
import com.example.freskoapp.domain.FakeProductRepository
import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.model.ProductStatus
import com.example.freskoapp.domain.usecase.AddProductUseCase
import com.example.freskoapp.domain.usecase.GetProductDetailUseCase
import com.example.freskoapp.domain.usecase.SearchProductByBarcodeUseCase
import com.example.freskoapp.domain.usecase.UpdateProductUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class AddProductViewModelTest {
    private val date = LocalDate.of(2026, 12, 1)
    private val repo = FakeProductRepository(listOf(Product(1, "Leche", date, 2, Location.FREEZER, createdAt = 1_000)))

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun editVm(id: Long = 1) = AddProductViewModel(
        AddProductUseCase(repo, FakeEventRepository(), NoTransaction), UpdateProductUseCase(repo, FakeEventRepository(), NoTransaction), GetProductDetailUseCase(repo),
        SearchProductByBarcodeUseCase(FakeProductInfoRepository()), id,
    )

    @Test
    fun withAProductIdItPreloadsTheFields() {
        val s = editVm().state.value
        assertTrue(s.isEditing)
        assertEquals("Leche", s.name)
        assertEquals("2", s.quantity)
        assertEquals(date, s.expirationDate)
        assertEquals(Location.FREEZER, s.location)
    }

    @Test
    fun savingEditsTheRecordInsteadOfRegisteringANewOne() {
        val vm = editVm()
        vm.onNameChange("Leche entera")
        vm.onQuantityChange("5")
        vm.save()
        assertTrue(vm.state.value.saved)
        assertFalse(vm.state.value.merged)
        // Registering would have created a second lot; an edit keeps one record with the new values.
        assertEquals(listOf("Leche entera"), repo.products.value.map { it.name })
        assertEquals(listOf(5), repo.products.value.map { it.quantity })
    }

    @Test
    fun productClosedWhileEditingGivesTheUnavailableOutcome() {
        val vm = editVm()
        repo.products.value = repo.products.value.map { it.copy(status = ProductStatus.CONSUMED) }
        vm.save()
        assertTrue(vm.state.value.notAvailable)
        assertFalse(vm.state.value.saved)
        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun unknownProductIdIsUnavailable() {
        assertTrue(editVm(99).state.value.notAvailable)
    }

    private fun newVm(handle: SavedStateHandle = SavedStateHandle()) = AddProductViewModel(
        AddProductUseCase(repo, FakeEventRepository(), NoTransaction), UpdateProductUseCase(repo, FakeEventRepository(), NoTransaction), GetProductDetailUseCase(repo),
        SearchProductByBarcodeUseCase(FakeProductInfoRepository()), null, handle,
    )

    @Test
    fun savingWithMissingFieldsShowsTheErrorsAndSavesNothing() {
        val vm = newVm()
        assertFalse(vm.state.value.nameError)
        vm.save()
        assertTrue(vm.state.value.nameError)
        assertTrue(vm.state.value.dateError)
        assertFalse(vm.state.value.saved)
        vm.onNameChange("Yerba")
        assertFalse(vm.state.value.nameError) // fixed fields drop their error right away
        assertTrue(vm.state.value.dateError)
        assertEquals(1, repo.products.value.size)
    }

    @Test
    fun quantityStepperNeverGoesBelowOne() {
        val vm = newVm()
        vm.onQuantityStep(-1)
        assertEquals("1", vm.state.value.quantity)
        vm.onQuantityStep(1)
        vm.onQuantityStep(1)
        assertEquals("3", vm.state.value.quantity)
    }

    @Test
    fun startScanFromTheSheetIsTakenOnlyOnce() {
        val handle = SavedStateHandle(mapOf(START_SCAN_ARG to true))
        val vm = newVm(handle)
        assertTrue(vm.takeStartScan())
        assertFalse(vm.takeStartScan())
        assertFalse(newVm(handle).takeStartScan()) // a recreated form (rotation) doesn't reopen the camera
        assertFalse(newVm().takeStartScan())
    }
}
