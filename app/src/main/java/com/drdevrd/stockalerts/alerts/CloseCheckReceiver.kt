package com.drdevrd.stockalerts.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.drdevrd.stockalerts.data.Exchange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class CloseCheckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val exchangeName = intent.getStringExtra("exchange") ?: return
        val exchange = Exchange.valueOf(exchangeName)

        // Reschedule tomorrow's check right away so a slow/failed run today
        // never breaks the daily cadence.
        AlarmScheduler.scheduleNextClose(context, exchange)

        val zone = if (exchange == Exchange.NSE) ZoneId.of("Asia/Kolkata") else ZoneId.of("America/New_York")
        val today = LocalDate.now(zone)
        if (today.dayOfWeek.value >= 6) return // Saturday/Sunday - markets closed

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AlertEngine.runCheck(context, exchange, force = false)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
