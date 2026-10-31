package com.example.freskoapp.data.repository

import com.example.freskoapp.data.remote.openFoodFactsClient
import com.example.freskoapp.domain.model.ProductInfo
import com.example.freskoapp.domain.model.ProductInfoResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class ProductInfoRepositoryImplTest {
    private val server = MockWebServer()
    private lateinit var repo: ProductInfoRepositoryImpl

    @Before
    fun setUp() {
        server.start()
        repo = ProductInfoRepositoryImpl(server.url("/").toString(), openFoodFactsClient(callTimeoutMillis = 200))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun search(response: MockResponse, barcode: String = "3017620422003"): ProductInfoResult = runBlocking {
        server.enqueue(response)
        repo.search(barcode)
    }

    private fun json(code: Int, body: String) = MockResponse().setResponseCode(code).setBody(body)

    // Real responses captured from world.openfoodfacts.org with ?fields=product_name,brands.
    @Test
    fun foundRealResponse() {
        val body = """{"code":"3017620422003","product":{"brands":"Nutella, Ferrero","product_name":"Nutella"},"status":1,"status_verbose":"product found"}"""
        assertEquals(ProductInfoResult.Found(ProductInfo("Nutella", "Nutella, Ferrero")), search(json(200, body)))
    }

    @Test
    fun invalidCodeRealResponseIsNotFound() {
        val body = """{"code":"00000000","status":0,"status_verbose":"no code or invalid code"}"""
        assertEquals(ProductInfoResult.NotFound, search(json(200, body), barcode = "0000000000000"))
    }

    @Test
    fun notFoundRealResponseIs404() {
        val body = """{"code":"7791234567892","status":0,"status_verbose":"product not found"}"""
        assertEquals(ProductInfoResult.NotFound, search(json(404, body), barcode = "7791234567892"))
    }

    @Test
    fun emptyProductNameIsNotFound() {
        val body = """{"code":"1","product":{"brands":"Ferrero","product_name":""},"status":1,"status_verbose":"product found"}"""
        assertEquals(ProductInfoResult.NotFound, search(json(200, body)))
    }

    @Test
    fun missingProductNameIsNotFound() {
        val body = """{"code":"1","product":{"brands":"Ferrero"},"status":1,"status_verbose":"product found"}"""
        assertEquals(ProductInfoResult.NotFound, search(json(200, body)))
    }

    @Test
    fun serverErrorsAreServerError() {
        assertEquals(ProductInfoResult.ServerError, search(json(500, "oops")))
        assertEquals(ProductInfoResult.ServerError, search(json(503, "")))
        assertEquals(2, server.requestCount) // one request each: no retry
    }

    @Test
    fun malformedJsonIsServerError() {
        assertEquals(ProductInfoResult.ServerError, search(json(200, "{not json")))
    }

    @Test
    fun emptyBodyIsServerError() {
        assertEquals(ProductInfoResult.ServerError, search(json(200, "")))
    }

    @Test
    fun slowResponseIsTimeout() {
        val slow = json(200, """{"status":1,"product":{"product_name":"Nutella"}}""").setHeadersDelay(1, TimeUnit.SECONDS)
        assertEquals(ProductInfoResult.Timeout, search(slow))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun droppedConnectionIsOffline() {
        assertEquals(ProductInfoResult.Offline, search(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun sendsUserAgentPathAndFields() {
        search(json(404, "{}"))
        val request = server.takeRequest()
        assertEquals("Fresko/1.0 (UADE academic project)", request.getHeader("User-Agent"))
        assertEquals("/api/v2/product/3017620422003.json?fields=product_name,brands", request.path)
    }

    @Test
    fun cancellationPropagatesInsteadOfBecomingAResult() = runBlocking {
        val patientRepo = ProductInfoRepositoryImpl(server.url("/").toString(), openFoodFactsClient(callTimeoutMillis = 10_000))
        server.enqueue(json(200, "{}").setHeadersDelay(5, TimeUnit.SECONDS))
        var produced: ProductInfoResult? = null
        val call = async { produced = patientRepo.search("3017620422003") }
        // Cancel only once the request is in flight, so the HTTP call itself gets cancelled.
        assertNotNull(withContext(Dispatchers.IO) { server.takeRequest(5, TimeUnit.SECONDS) })
        call.cancel()
        call.join()
        assertTrue(call.isCancelled)
        assertNull(produced)
    }
}
