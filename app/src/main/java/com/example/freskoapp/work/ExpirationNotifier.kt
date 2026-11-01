package com.example.freskoapp.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.freskoapp.MainActivity
import com.example.freskoapp.R
import com.example.freskoapp.domain.model.ExpirationStatus
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.presentation.common.canPostNotifications
import com.example.freskoapp.presentation.navigation.inventoryDeepLink
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object ExpirationNotifier {
    const val CHANNEL_ID = "expiration_alerts"
    const val NOTIFICATION_ID = 1001

    // Marks intents that come from the alert, so MainActivity can record NOTIFICATION_TAPPED.
    const val EXTRA_FROM_NOTIFICATION = "from_notification"

    // minSdk is 26, so channels always exist; creating an existing channel is a no-op.
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.notification_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    // One summary notification; with nothing expiring it clears the previous one instead of posting.
    fun show(context: Context, products: List<Product>) {
        val manager = NotificationManagerCompat.from(context)
        if (products.isEmpty()) {
            manager.cancel(NOTIFICATION_ID)
            return
        }
        if (!context.canPostNotifications()) return
        // Redundant with canPostNotifications(), but lint only recognizes a direct permission check.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        // NEW_TASK + CLEAR_TASK so the deep link is delivered even if the app is already running.
        val intent = Intent(
            Intent.ACTION_VIEW,
            inventoryDeepLink(ExpirationStatus.EXPIRING_SOON),
            context,
            MainActivity::class.java,
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .putExtra(EXTRA_FROM_NOTIFICATION, true)
        val contentIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val body = bodyText(context, soonestToExpire(products, LocalDate.now()))
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_fresko)
            .setContentTitle(
                context.resources.getQuantityString(R.plurals.notification_expiring_title, products.size, products.size)
            )
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}

// The product the alert names: the one closest to expiring (first by name on a tie, so the text is stable).
internal data class SoonestToExpire(val name: String, val daysLeft: Int)

internal fun soonestToExpire(products: List<Product>, today: LocalDate): SoonestToExpire {
    val first = products.minWith(compareBy<Product>({ it.expirationDate }, { it.name }))
    return SoonestToExpire(first.name, ChronoUnit.DAYS.between(today, first.expirationDate).toInt().coerceAtLeast(0))
}

// "Yogur natural vence mañana. Tocá para ver qué consumir primero."
private fun bodyText(context: Context, soonest: SoonestToExpire): String = when (soonest.daysLeft) {
    0 -> context.getString(R.string.notification_body_today, soonest.name)
    1 -> context.getString(R.string.notification_body_tomorrow, soonest.name)
    else -> context.resources.getQuantityString(R.plurals.notification_body_in_days, soonest.daysLeft, soonest.name, soonest.daysLeft)
}
