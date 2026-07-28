package com.drdevrd.stockalerts.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.drdevrd.stockalerts.data.AlertState
import com.drdevrd.stockalerts.data.AlertMode
import com.drdevrd.stockalerts.data.AppDatabase
import com.drdevrd.stockalerts.data.Exchange
import com.drdevrd.stockalerts.data.Prefs
import com.drdevrd.stockalerts.data.StockEntity
import com.drdevrd.stockalerts.network.PriceFetcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
                runCheck(context, exchange)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun runCheck(context: Context, exchange: Exchange) {
        val dao = AppDatabase.getInstance(context).stockDao()
        val stocks = dao.getByExchange(exchange)
        if (stocks.isEmpty()) return

        val apiKey = Prefs.getFinnhubApiKey(context)
        val fetcher = PriceFetcher(apiKey)
        val dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        val closeLines = mutableListOf<String>()
        val targetLines = mutableListOf<String>()

        for (stock in stocks) {
            if (stock.lastAlertedDate == dateStr) continue // already handled today

            val result = fetcher.fetch(stock.symbol, stock.exchange) ?: continue

            val sign = if (result.changePercent >= 0) "+" else ""
            val closeLine = "${stock.symbol}: ${"%.2f".format(result.current)} ($sign${"%.2f".format(result.changePercent)}%)"

            if (stock.alertMode == AlertMode.DAILY_CLOSE || stock.alertMode == AlertMode.BOTH) {
                closeLines.add(closeLine)
            }

            val target = stock.targetPrice
            if (target != null && (stock.alertMode == AlertMode.TARGET_CROSS || stock.alertMode == AlertMode.BOTH)) {
                val crossed = (result.current >= target && (stock.lastKnownPrice ?: 0.0) < target) ||
                        (result.current <= target && (stock.lastKnownPrice ?: Double.MAX_VALUE) > target)
                if (crossed && !stock.targetAlreadyHit) {
                    targetLines.add("${stock.symbol} crossed target ${"%.2f".format(target)} -> ${"%.2f".format(result.current)}")
                }
            }

            dao.update(
                stock.copy(
                    lastAlertedDate = dateStr,
                    lastKnownPrice = result.current,
                    targetAlreadyHit = if (targetLines.isNotEmpty()) true else stock.targetAlreadyHit
                )
            )
        }

        if (closeLines.isEmpty() && targetLines.isEmpty()) return

        val notifId = if (exchange == Exchange.NSE) NotificationHelper.NOTIF_ID_NSE else NotificationHelper.NOTIF_ID_US
        val title = if (exchange == Exchange.NSE) "NSE closing prices" else "US closing prices"
        val messageParts = mutableListOf<String>()
        if (targetLines.isNotEmpty()) messageParts.add(targetLines.joinToString("\n"))
        if (closeLines.isNotEmpty()) messageParts.add(closeLines.joinToString("\n"))
        val message = messageParts.joinToString("\n\n")

        AlertState.setActive(context, notifId, title, message)
        NotificationHelper.show(context, notifId, title, message)
        AlarmScheduler.scheduleRepeat(context, notifId, Prefs.getRepeatIntervalMinutes(context))
    }
}
