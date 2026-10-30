package com.example.freskoapp.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object ExpirationCheckScheduler {
    val NOTIFICATION_TIME: LocalTime = LocalTime.of(9, 0)
    const val WORK_NAME = "expiration_check"

    // Time until the next `at`: later today if it hasn't passed, otherwise tomorrow; exactly `at` means now.
    fun initialDelay(now: LocalDateTime, at: LocalTime = NOTIFICATION_TIME): Duration {
        var next = now.toLocalDate().atTime(at)
        if (next.isBefore(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }

    // KEEP: the first run time is fixed by the first enqueue; periodic work is not exact (Doze, battery savers).
    fun schedule(context: Context, clock: Clock = Clock.systemDefaultZone()) {
        val request = PeriodicWorkRequestBuilder<ExpirationCheckWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay(LocalDateTime.now(clock)))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
