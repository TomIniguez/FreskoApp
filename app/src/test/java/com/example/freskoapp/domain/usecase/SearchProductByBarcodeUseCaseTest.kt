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
    fun anyBarcodeIsSearchedWithoutValidation() = runBlocking {
        assertEquals(ProductInfoResult.NotFound, useCase("not-a-barcode"))
        assertEquals(listOf("not-a-barcode"), repo.searched)
    }

    @Test
    fun cancellationIsNotSwallowed() {
        repo.error = CancellationException("cancelled")
        assertThrows(CancellationException::class.java) { runBlocking { useCase("3017620422003") } }
    }
}
