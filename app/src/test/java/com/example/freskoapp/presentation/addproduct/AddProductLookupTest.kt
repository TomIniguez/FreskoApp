package com.example.freskoapp.presentation.addproduct

import com.example.freskoapp.domain.FakeEventRepository
import com.example.freskoapp.domain.NoTransaction
import androidx.lifecycle.SavedStateHandle
import com.example.freskoapp.domain.FakeProductInfoRepository
import com.example.freskoapp.domain.FakeProductRepository
import com.example.freskoapp.domain.model.AddMethod
import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.model.ProductInfo
import com.example.freskoapp.domain.model.ProductInfoResult
import com.example.freskoapp.domain.usecase.AddProductUseCase
import com.example.freskoapp.domain.usecase.GetProductDetailUseCase
import com.example.freskoapp.domain.usecase.SearchProductByBarcodeUseCase
import com.example.freskoapp.domain.usecase.UpdateProductUseCase
import com.example.freskoapp.presentation.navigation.SCANNED_BARCODE_KEY
import com.example.freskoapp.presentation.navigation.putScannedBarcode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class AddProductLookupTest {
    private val date = LocalDate.of(2026, 12, 1)
    private val products = FakeProductRepository(listOf(Product(1, "Leche", date, 2, Location.FREEZER, createdAt = 1_000)))
    private val lookups = FakeProductInfoRepository()
    private var handle = SavedStateHandle()
    private val events = FakeEventRepository()
    private val nutella = ProductInfoResult.Found(ProductInfo("Nutella", "Ferrero"))

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(productId: Long? = null) = AddProductViewModel(
        AddProductUseCase(products, events, NoTransaction), UpdateProductUseCase(products, FakeEventRepository(), NoTransaction), GetProductDetailUseCase(products),
        SearchProductByBarcodeUseCase(lookups), productId, handle,
    )

    // Simulates the scanner handing a code back to the form's back stack entry.
    private fun scan(barcode: String = "3017620422003") = handle.putScannedBarcode(barcode)

    private fun AddProductViewModel.fillForm() {
        onQuantityChange("3")
        onDateSelected(date)
        onLocationChange(Location.ALACENA)
    }

    @Test
    fun foundFillsABlankName() {
        lookups.result = nutella
        val vm = vm()
        scan()
        assertEquals("Nutella", vm.state.value.name)
        assertEquals(LookupState.Found("Nutella"), vm.state.value.lookup)
        assertEquals(listOf("3017620422003"), lookups.searched)
    }

    @Test
    fun foundNeverOverwritesATypedName() {
        lookups.result = nutella
        val vm = vm()
        vm.onNameChange("Crema de avellanas")
        scan()
        assertEquals("Crema de avellanas", vm.state.value.name)
        assertEquals(LookupState.Found("Nutella"), vm.state.value.lookup)
    }

    @Test
    fun eachFailureHasItsOwnStateAndKeepsTheFields() {
        val cases = mapOf(
            ProductInfoResult.NotFound to LookupFailure.NOT_FOUND,
            ProductInfoResult.Timeout to LookupFailure.TIMEOUT,
            ProductInfoResult.ServerError to LookupFailure.SERVER_ERROR,
            ProductInfoResult.Offline to LookupFailure.OFFLINE,
        )
        cases.forEach { (result, kind) ->
            lookups.result = result
            handle = SavedStateHandle() // one form entry per case
            val vm = vm()
            vm.onNameChange("Yerba")
            vm.fillForm()
            scan()
            val s = vm.state.value
            assertEquals(LookupState.Failure(kind), s.lookup)
            assertEquals("Yerba", s.name)
            assertEquals("3", s.quantity)
            assertEquals(date, s.expirationDate)
            assertEquals(Location.ALACENA, s.location)
            assertTrue(s.canSave)
        }
    }

    @Test
    fun retryLooksUpTheSameBarcodeAgain() {
        lookups.result = ProductInfoResult.Offline
        val vm = vm()
        scan()
        assertEquals("3017620422003", vm.state.value.scannedBarcode)
        lookups.result = nutella
        vm.retry()
        assertEquals(listOf("3017620422003", "3017620422003"), lookups.searched)
        assertEquals("Nutella", vm.state.value.name)
    }

    @Test
    fun retryWithoutAScanDoesNothing() {
        vm().retry()
        assertEquals(0, lookups.calls)
    }

    @Test
    fun aNewScanCancelsTheLookupInFlight() {
        val firstGate = CompletableDeferred<Unit>()
        lookups.gate = firstGate
        lookups.result = ProductInfoResult.Found(ProductInfo("Primero", null))
        val vm = vm()
        scan("11111111")
        assertEquals(LookupState.Loading, vm.state.value.lookup)

        lookups.gate = null
        lookups.result = ProductInfoResult.Found(ProductInfo("Segundo", null))
        scan("22222222")
        // Releasing the first lookup must not overwrite the second one's result.
        firstGate.complete(Unit)
        assertEquals("Segundo", vm.state.value.name)
        assertEquals(LookupState.Found("Segundo"), vm.state.value.lookup)
    }

    @Test
    fun loadingDoesNotBlockSaving() {
        lookups.gate = CompletableDeferred()
        val vm = vm()
        vm.onNameChange("Yerba")
        vm.fillForm()
        scan()
        assertEquals(LookupState.Loading, vm.state.value.lookup)
        assertTrue(vm.state.value.canSave)
        vm.save()
        assertTrue(vm.state.value.saved)
    }

    @Test
    fun scannedBarcodeIsConsumedOnce() {
        val vm = vm()
        scan()
        assertNull(handle.get<String>(SCANNED_BARCODE_KEY))
        // A second VM on the same entry (e.g. after process death) must not look it up again.
        vm()
        assertEquals(1, lookups.calls)
        assertEquals(LookupState.Failure(LookupFailure.NOT_FOUND), vm.state.value.lookup)
    }

    @Test
    fun scansAreIgnoredWhileEditing() {
        lookups.result = nutella
        val vm = vm(productId = 1)
        scan()
        assertEquals(0, lookups.calls)
        assertEquals("Leche", vm.state.value.name)
        assertEquals(LookupState.Idle, vm.state.value.lookup)
    }

    @Test
    fun invalidScannedCodeIsNotFoundWithoutANetworkCall() {
        val vm = vm()
        scan("123")
        assertEquals(0, lookups.calls)
        assertEquals(LookupState.Failure(LookupFailure.NOT_FOUND), vm.state.value.lookup)
    }

    @Test
    fun saveRecordsScanOnlyWhenABarcodeWasScannedInThisForm() {
        lookups.result = nutella
        val scanned = vm()
        scan()
        scanned.fillForm()
        scanned.save()
        handle = SavedStateHandle() // a new form entry
        val manual = vm()
        manual.onNameChange("Yerba")
        manual.fillForm()
        manual.save()
        assertEquals(listOf(AddMethod.SCAN, AddMethod.MANUAL), events.events.value.map { it.method })
        assertTrue(events.events.value.all { it.durationMs != null })
    }
}
