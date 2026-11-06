package com.example.freskoapp

import android.app.Application
import androidx.room.Room
import com.example.freskoapp.data.local.FreskoDatabase
import com.example.freskoapp.data.local.MIGRATION_1_2
import com.example.freskoapp.data.local.MIGRATION_2_3
import com.example.freskoapp.data.local.MIGRATION_3_4
import com.example.freskoapp.data.local.RoomTransactionRunner
import com.example.freskoapp.data.preferences.SettingsRepositoryImpl
import com.example.freskoapp.data.preferences.settingsDataStore
import com.example.freskoapp.data.remote.OPEN_FOOD_FACTS_BASE_URL
import com.example.freskoapp.data.remote.openFoodFactsClient
import com.example.freskoapp.data.repository.EventRepositoryImpl
import com.example.freskoapp.data.repository.ProductInfoRepositoryImpl
import com.example.freskoapp.data.repository.ProductRepositoryImpl
import com.example.freskoapp.domain.repository.EventRepository
import com.example.freskoapp.domain.repository.ProductInfoRepository
import com.example.freskoapp.domain.repository.ProductRepository
import com.example.freskoapp.domain.repository.SettingsRepository
import com.example.freskoapp.domain.repository.TransactionRunner
import com.example.freskoapp.domain.usecase.AddProductUseCase
import com.example.freskoapp.domain.usecase.CloseUnitsUseCase
import com.example.freskoapp.domain.usecase.GetInventoryUseCase
import com.example.freskoapp.domain.usecase.GetProductDetailUseCase
import com.example.freskoapp.domain.usecase.RecordAppEventUseCase
import com.example.freskoapp.domain.usecase.RestoreUnitsUseCase
import com.example.freskoapp.domain.usecase.SearchProductByBarcodeUseCase
import com.example.freskoapp.domain.usecase.UpdateProductUseCase
import com.example.freskoapp.work.ExpirationCheckScheduler
import com.example.freskoapp.work.ExpirationNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Manual dependency injection: ViewModel factories and the worker read this from the Application.
class FreskoApplication : Application() {
    // Lives as long as the process; for work that must not depend on a screen.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // No destructive fallback: a missing migration must crash in testing, never wipe the pilot data.
    private val database by lazy {
        Room.databaseBuilder(this, FreskoDatabase::class.java, "fresko.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }

    val productRepository: ProductRepository by lazy { ProductRepositoryImpl(database.productDao()) }

    val eventRepository: EventRepository by lazy { EventRepositoryImpl(database.productEventDao()) }

    private val transactionRunner: TransactionRunner by lazy { RoomTransactionRunner(database) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(settingsDataStore) }

    val productInfoRepository: ProductInfoRepository by lazy {
        ProductInfoRepositoryImpl(OPEN_FOOD_FACTS_BASE_URL, openFoodFactsClient(callTimeoutMillis = 5_000))
    }

    fun searchProductByBarcodeUseCase() = SearchProductByBarcodeUseCase(productInfoRepository)

    // The inventory and the daily worker share the user's warning days through this use case.
    fun getInventoryUseCase() = GetInventoryUseCase(productRepository, warningDays = settingsRepository.warningDays)

    fun getProductDetailUseCase() = GetProductDetailUseCase(productRepository, warningDays = settingsRepository.warningDays)

    fun addProductUseCase() = AddProductUseCase(productRepository, eventRepository, transactionRunner)

    fun updateProductUseCase() = UpdateProductUseCase(productRepository, eventRepository, transactionRunner)

    fun closeUnitsUseCase() = CloseUnitsUseCase(productRepository, eventRepository, transactionRunner)

    fun restoreUnitsUseCase() = RestoreUnitsUseCase(productRepository, eventRepository, transactionRunner)

    fun recordAppEventUseCase() = RecordAppEventUseCase(eventRepository)

    override fun onCreate() {
        super.onCreate()
        ExpirationNotifier.createChannel(this)
        // Off the main thread: the stored alert time comes from DataStore. KEEP makes it a no-op once scheduled.
        appScope.launch { ExpirationCheckScheduler.schedule(this@FreskoApplication, settingsRepository.alertTime.first()) }
    }
}
