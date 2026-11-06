package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.FakeProductInfoRepository
import com.example.freskoapp.domain.model.ProductInfo
import com.example.freskoapp.domain.model.ProductInfoResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class SearchProductByBarcodeUseCaseTest {
    private val repo = FakeProductInfoRepository()
    private val useCase = SearchProductByBarcodeUseCase(repo)

    private fun resultFor(result: ProductInfoResult): ProductInfoResult = runBlocking {
        repo.result = result
        useCase("3017620422003")
    }

    @Test
    fun foundPassesThrough() {
        val found = ProductInfoResult.Found(ProductInfo("Nutella", "Nutella, Ferrero"))
        assertEquals(found, resultFor(found))
        assertEquals(listOf("3017620422003"), repo.searched)
    }

    @Test
    fun failuresPassThrough() {
        listOf(ProductInfoResult.NotFound, ProductInfoResult.Offline, ProductInfoResult.Timeout, ProductInfoResult.ServerError)
            .forEach { assertEquals(it, resultFor(it)) }
    }

    @Test
    fun foundWithBlankNameIsNotFound() {
        assertEquals(ProductInfoResult.NotFound, resultFor(ProductInfoResult.Found(ProductInfo("  ", "Ferrero"))))
    }

    @Test
    fun invalidBarcodesAreNotFoundWithoutCallingTheRepository() = runBlocking {
        repo.result = ProductInfoResult.Found(ProductInfo("Nutella", null))
        // "..", too short, 15 digits, Arabic-Indic digits, and a space-padded code (no trimming).
        listOf("..", "123", "123456789012345", "١٢٣٤٥٦٧٨", " 12345678").forEach {
            assertEquals(it, ProductInfoResult.NotFound, useCase(it))
        }
        assertEquals(0, repo.calls)
    }

    @Test
    fun eightAndFourteenDigitBarcodesReachTheRepository() = runBlocking {
        useCase("12345678")
        useCase("12345678901234")
        assertEquals(listOf("12345678", "12345678901234"), repo.searched)
    }

    @Test
    fun cancellationIsNotSwallowed() {
        repo.error = CancellationException("cancelled")
        assertThrows(CancellationException::class.java) { runBlocking { useCase("3017620422003") } }
    }
}
