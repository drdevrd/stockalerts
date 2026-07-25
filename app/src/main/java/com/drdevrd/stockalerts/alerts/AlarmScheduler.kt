package com.drdevrd.stockalerts.alerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.drdevrd.stockalerts.data.Exchange
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object AlarmScheduler {

    private const val REQ_NSE = 501
    private const val REQ_US = 502

    /** Schedules (or reschedules) both daily close-check alarms. Call on app start and after boot. */
    fun scheduleAll(context: Context) {
        scheduleNextClose(context, Exchange.NSE)
        scheduleNextClose(context, Exchange.US)
    }

    fun scheduleNextClose(context: Context, exchange: Exchange) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        val triggerMillis = when (exchange) {
            // NSE closes 15:30 IST, Mon-Fri. We schedule daily; the receiver
            // itself skips weekends/holidays gracefully (empty/stale response).
            Exchange.NSE -> nextOccurrence(ZoneId.of("Asia/Kolkata"), LocalTime.of(15, 30))
            // US market closes 16:00 America/New_York - java.time handles
            // EST/EDT transitions automatically so this stays correct year-round.
            Exchange.US -> nextOccurrence(ZoneId.of("America/New_York"), LocalTime.of(16, 0))
        }

        val intent = Intent(context, CloseCheckReceiver::class.java).apply {
            putExtra("exchange", exchange.name)
        }
        val reqCode = if (exchange == Exchange.NSE) REQ_NSE else REQ_US
        val pendingIntent = PendingIntent.getBroadcast(
            context, reqCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent
        )
    }

    /** Next epoch-millis occurrence of [timeOfDay] in [zone], skipping to tomorrow if already past today. */
    private fun nextOccurrence(zone: ZoneId, timeOfDay: LocalTime): Long {
        val now = ZonedDateTime.now(zone)
        var candidate = now.with(timeOfDay)
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1)
        }
        return candidate.toInstant().toEpochMilli()
    }

    fun scheduleRepeat(context: Context, notifId: Int, delayMinutes: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, RepeatNotifyReceiver::class.java).apply {
            putExtra("notif_id", notifId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + delayMinutes * 60_000L
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    fun cancelRepeat(context: Context, notifId: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, RepeatNotifyReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
