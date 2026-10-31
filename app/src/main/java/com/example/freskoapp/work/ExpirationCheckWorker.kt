package com.example.freskoapp.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.freskoapp.FreskoApplication
import com.example.freskoapp.domain.usecase.GetExpiringProductsUseCase

// Daily check. Dependencies come from the Application (manual DI), so no custom WorkerFactory is needed.
// It queries Room on every run, so edited dates, quantities and locations are picked up with no rescheduling.
class ExpirationCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val app = applicationContext as FreskoApplication
        ExpirationNotifier.show(app, GetExpiringProductsUseCase(app.getInventoryUseCase())())
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }
}
