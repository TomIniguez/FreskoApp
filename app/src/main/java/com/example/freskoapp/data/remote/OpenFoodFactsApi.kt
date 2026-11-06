package com.example.freskoapp.data.remote

import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

const val OPEN_FOOD_FACTS_BASE_URL = "https://world.openfoodfacts.org/"

// Open Food Facts asks every client to identify itself.
const val OPEN_FOOD_FACTS_USER_AGENT = "Fresko/1.0 (UADE academic project)"

interface OpenFoodFactsApi {
    // Response<T> so a 404 comes back as a code instead of an HttpException.
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProduct(
        @Path("barcode") barcode: String,
        @Query("fields", encoded = true) fields: String = "product_name,brands", // literal comma, as verified with curl
    ): Response<ProductInfoDto>
}

// No retries: a lookup must fail within one call timeout, not several.
fun openFoodFactsClient(callTimeoutMillis: Long): OkHttpClient = OkHttpClient.Builder()
    .callTimeout(callTimeoutMillis, TimeUnit.MILLISECONDS)
    .retryOnConnectionFailure(false)
    .addInterceptor { chain ->
        chain.proceed(chain.request().newBuilder().header("User-Agent", OPEN_FOOD_FACTS_USER_AGENT).build())
    }
    .build()
