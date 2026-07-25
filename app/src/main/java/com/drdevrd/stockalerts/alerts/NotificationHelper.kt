package com.drdevrd.stockalerts.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.drdevrd.stockalerts.ui.MainActivity

object NotificationHelper {

    const val CHANNEL_ID = "stock_close_alerts"
    const val NOTIF_ID_NSE = 1001
    const val NOTIF_ID_US = 1002

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Stock closing price alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Repeating alerts for NSE/US stock closing prices"
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun show(context: Context, notifId: Int, title: String, message: String) {
        ensureChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context, notifId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, NotificationDismissReceiver::class.java).apply {
            putExtra("notif_id", notifId)
        }
        val dismissPending = PendingIntent.getBroadcast(
            context, notifId, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .setDeleteIntent(dismissPending)
            .addAction(0, "Dismiss reminders", dismissPending)

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notifId, builder.build())
    }

    fun cancel(context: Context, notifId: Int) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(notifId)
    }
}
