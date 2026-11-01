package com.example.freskoapp.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.freskoapp.domain.repository.DEFAULT_ALERT_TIME
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object ExpirationCheckScheduler {
    const val WORK_NAME = "expiration_check"

    // Time until the next `at`: later today if it hasn't passed, otherwise tomorrow; exactly `at` means now.
    fun initialDelay(now: LocalDateTime, at: LocalTime = DEFAULT_ALERT_TIME): Duration {
        var next = now.toLocalDate().atTime(at)
        if (next.isBefore(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }

    // App start. KEEP: an already scheduled check keeps its time; periodic work is not exact (Doze, battery savers).
    fun schedule(context: Context, at: LocalTime, clock: Clock = Clock.systemDefaultZone()) =
        enqueue(context, at, clock, ExistingPeriodicWorkPolicy.KEEP)

    // The user picked another time: the pending run moves to it. CANCEL_AND_REENQUEUE, because the daily period
    // counts from the first enqueue and a plain update would keep the old time of day.
    fun reschedule(context: Context, at: LocalTime, clock: Clock = Clock.systemDefaultZone()) =
        enqueue(context, at, clock, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE)

    private fun enqueue(context: Context, at: LocalTime, clock: Clock, policy: ExistingPeriodicWorkPolicy) {
        val request = PeriodicWorkRequestBuilder<ExpirationCheckWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay(LocalDateTime.now(clock), at))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, policy, request)
    }
}
