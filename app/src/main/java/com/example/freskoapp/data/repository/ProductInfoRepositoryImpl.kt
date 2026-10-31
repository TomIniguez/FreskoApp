package com.example.freskoapp.data.repository

import com.example.freskoapp.data.mapper.toDomain
import com.example.freskoapp.data.remote.OpenFoodFactsApi
import com.example.freskoapp.domain.model.ProductInfoResult
import com.example.freskoapp.domain.repository.ProductInfoRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.io.InterruptedIOException
import kotlin.coroutines.cancellation.CancellationException

class ProductInfoRepositoryImpl(
    baseUrl: String,
    client: OkHttpClient,
    json: Json = Json { ignoreUnknownKeys = true },
) : ProductInfoRepository {
    private val api = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(OpenFoodFactsApi::class.java)

    override suspend fun search(barcode: String): ProductInfoResult = try {
        val response = api.getProduct(barcode)
        when {
            response.code() == 404 -> ProductInfoResult.NotFound
            !response.isSuccessful -> ProductInfoResult.ServerError
            else -> response.body()?.let { dto -> dto.toDomain()?.let { ProductInfoResult.Found(it) } ?: ProductInfoResult.NotFound }
                ?: ProductInfoResult.ServerError
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: InterruptedIOException) { // callTimeout and SocketTimeoutException both land here
        ProductInfoResult.Timeout
    } catch (e: IOException) { // also a body cut off mid-read: indistinguishable from a dropped connection
        ProductInfoResult.Offline
    } catch (e: IllegalArgumentException) { // SerializationException is one: malformed or empty JSON
        ProductInfoResult.ServerError
    }
}
