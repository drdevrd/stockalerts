package com.drdevrd.stockalerts.alerts

import android.content.Context
import com.drdevrd.stockalerts.data.AlertMode
import com.drdevrd.stockalerts.data.AlertState
import com.drdevrd.stockalerts.data.AppDatabase
import com.drdevrd.stockalerts.data.Exchange
import com.drdevrd.stockalerts.data.Prefs
import com.drdevrd.stockalerts.network.FinnhubApi
import com.drdevrd.stockalerts.network.GoogleSheetsApi
import com.drdevrd.stockalerts.network.PriceResult
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object AlertEngine {

    data class RunResult(
        val stocksInList: Int,
        val fetchSucceeded: Int,
        val fetchFailed: Int,
        val notificationShown: Boolean,
        val failedSymbols: List<String>,
        val sampleError: String?
    )

    /**
     * Runs a price check for [exchange] and shows a notification if anything qualifies.
     *
     * @param force When true (used by the manual "Test Now" button), skips the
     * weekend check and the "already alerted today" guard, and always shows a
     * notification if at least one stock's price was fetched successfully.
     */
    suspend fun runCheck(context: Context, exchange: Exchange, force: Boolean = false): RunResult {
        val dao = AppDatabase.getInstance(context).stockDao()
        val stocks = dao.getByExchange(exchange)
        if (stocks.isEmpty()) {
            return RunResult(0, 0, 0, false, emptyList(), null)
        }

        val dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        val closeLines = mutableListOf<String>()
        val targetLines = mutableListOf<String>()
        val testLines = mutableListOf<String>()
        val failedSymbols = mutableListOf<String>()
        var successCount = 0
        var sampleError: String? = null

        // NSE fetches everything in one batch call (one CSV covers all symbols);
        // US still fetches per-symbol via Finnhub.
        var nseBatch: Map<String, PriceResult> = emptyMap()
        if (exchange == Exchange.NSE) {
            val csvUrl = Prefs.getGoogleSheetsCsvUrl(context)
            val sheetResult = GoogleSheetsApi().fetchAll(csvUrl)
            nseBatch = sheetResult.bySymbol
            if (sheetResult.error != null) sampleError = sheetResult.error
        }

        val finnhub = if (exchange == Exchange.US) FinnhubApi(Prefs.getFinnhubApiKey(context)) else null

        for (stock in stocks) {
            if (!force && stock.lastAlertedDate == dateStr) continue

            val result: PriceResult? = if (exchange == Exchange.NSE) {
                val hit = nseBatch[stock.symbol.uppercase()]
                if (hit == null && sampleError == null) {
                    sampleError = "${stock.symbol} not found in the published sheet - check it has a row with that exact symbol"
                }
                hit
            } else {
                val quote = finnhub?.getQuote(stock.symbol)
                if (quote == null && sampleError == null) {
                    sampleError = "Finnhub returned no data for ${stock.symbol} (check API key, or symbol may be wrong)"
                }
                quote?.let { PriceResult(it.current, it.previousClose, it.changePercent) }
            }

            if (result == null) {
                failedSymbols.add(stock.symbol)
                continue
            }
            successCount++

            val sign = if (result.changePercent >= 0) "+" else ""
            val closeLine = "${stock.symbol}: ${"%.2f".format(result.current)} ($sign${"%.2f".format(result.changePercent)}%)"

            if (force) testLines.add(closeLine)
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

            if (!force) {
                dao.update(
                    stock.copy(
                        lastAlertedDate = dateStr,
                        lastKnownPrice = result.current,
                        targetAlreadyHit = if (targetLines.isNotEmpty()) true else stock.targetAlreadyHit
                    )
                )
            }
        }

        val notifId = if (exchange == Exchange.NSE) NotificationHelper.NOTIF_ID_NSE else NotificationHelper.NOTIF_ID_US
        val title = if (exchange == Exchange.NSE) "NSE closing prices" else "US closing prices"

        val shouldNotify = if (force) testLines.isNotEmpty() else (closeLines.isNotEmpty() || targetLines.isNotEmpty())

        if (shouldNotify) {
            val messageParts = mutableListOf<String>()
            if (targetLines.isNotEmpty()) messageParts.add(targetLines.joinToString("\n"))
            if (closeLines.isNotEmpty()) messageParts.add(closeLines.joinToString("\n"))
            else if (force && testLines.isNotEmpty()) messageParts.add(testLines.joinToString("\n"))
            val message = messageParts.joinToString("\n\n")

            AlertState.setActive(context, notifId, if (force) "$title (TEST)" else title, message)
            NotificationHelper.show(context, notifId, if (force) "$title (TEST)" else title, message)
            AlarmScheduler.scheduleRepeat(context, notifId, Prefs.getRepeatIntervalMinutes(context))
        }

        return RunResult(
            stocksInList = stocks.size,
            fetchSucceeded = successCount,
            fetchFailed = failedSymbols.size,
            notificationShown = shouldNotify,
            failedSymbols = failedSymbols,
            sampleError = sampleError
        )
    }
}
