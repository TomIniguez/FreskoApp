package com.example.freskoapp

import android.app.Application
import androidx.room.Room
import com.example.freskoapp.data.local.FreskoDatabase
import com.example.freskoapp.data.repository.ProductRepositoryImpl
import com.example.freskoapp.domain.repository.ProductRepository

// Manual dependency injection: ViewModel factories read this from the Application.
class FreskoApplication : Application() {
    val productRepository: ProductRepository by lazy {
        ProductRepositoryImpl(
            Room.databaseBuilder(this, FreskoDatabase::class.java, "fresko.db").build().productDao()
        )
    }
}
