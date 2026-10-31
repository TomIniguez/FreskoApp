package com.example.freskoapp

import android.app.Application
import androidx.room.Room
import com.example.freskoapp.data.local.FreskoDatabase
import com.example.freskoapp.data.local.MIGRATION_1_2
import com.example.freskoapp.data.local.MIGRATION_2_3
import com.example.freskoapp.data.preferences.SettingsRepositoryImpl
import com.example.freskoapp.data.preferences.settingsDataStore
import com.example.freskoapp.data.repository.ProductRepositoryImpl
import com.example.freskoapp.domain.repository.ProductRepository
import com.example.freskoapp.domain.repository.SettingsRepository
import com.example.freskoapp.domain.usecase.GetInventoryUseCase
import com.example.freskoapp.domain.usecase.GetProductDetailUseCase
import com.example.freskoapp.work.ExpirationCheckScheduler
import com.example.freskoapp.work.ExpirationNotifier

// Manual dependency injection: ViewModel factories and the worker read this from the Application.
class FreskoApplication : Application() {
    val productRepository: ProductRepository by lazy {
        ProductRepositoryImpl(
            Room.databaseBuilder(this, FreskoDatabase::class.java, "fresko.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .productDao()
        )
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(settingsDataStore) }

    // The inventory and the daily worker share the user's warning days through this use case.
    fun getInventoryUseCase() = GetInventoryUseCase(productRepository, warningDays = settingsRepository.warningDays)

    fun getProductDetailUseCase() = GetProductDetailUseCase(productRepository, warningDays = settingsRepository.warningDays)

    override fun onCreate() {
        super.onCreate()
        ExpirationNotifier.createChannel(this)
        ExpirationCheckScheduler.schedule(this)
    }
}
