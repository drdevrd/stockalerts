package com.drdevrd.stockalerts.network

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Fetches NSE prices from a user-published Google Sheet CSV, where each row
 * uses GOOGLEFINANCE("NSE:"&symbol, "price"/"changepct") formulas.
 *
 * This exists because NSE's own site actively blocks non-browser HTTP
 * clients (HTTP 403) - Google Sheets acts as a free, already-authorized
 * proxy that pulls the data on our behalf. Data is ~20 min delayed, which
 * is irrelevant for a market-close check.
 *
 * Expected CSV format (header row required):
 *   Symbol,Price,ChangePct
 *   RELIANCE,1280.5,0.42
 *   TCS,3500.1,-0.15
 */
class GoogleSheetsApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    data class SheetResult(val bySymbol: Map<String, PriceResult>, val error: String?)

    /** Fetches and parses the whole published CSV once - call this once per NSE check, not per-symbol. */
    fun fetchAll(csvUrl: String): SheetResult {
        if (csvUrl.isBlank()) {
            return SheetResult(emptyMap(), "No Google Sheets CSV URL set in Settings")
        }

        return try {
            val request = Request.Builder().url(csvUrl).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return SheetResult(emptyMap(), "Sheet CSV fetch returned HTTP ${response.code}")
                }
                val body = response.body?.string()
                    ?: return SheetResult(emptyMap(), "Sheet CSV response was empty")

                val lines = body.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                if (lines.size < 2) {
                    return SheetResult(emptyMap(), "Sheet CSV had no data rows (only header or empty)")
                }

                val map = mutableMapOf<String, PriceResult>()
                // Skip header row (index 0).
                for (i in 1 until lines.size) {
                    val cols = splitCsvLine(lines[i])
                    if (cols.size < 3) continue
                    val symbol = cols[0].trim().uppercase()
                    val price = cols[1].trim().toDoubleOrNull()
                    val changePct = cols[2].trim().toDoubleOrNull()
                    if (symbol.isEmpty() || price == null) continue
                    map[symbol] = PriceResult(price, price, changePct ?: 0.0)
                }

                if (map.isEmpty()) {
                    SheetResult(emptyMap(), "Sheet CSV parsed but no valid rows found - check formulas are returning numbers, not #N/A")
                } else {
                    SheetResult(map, null)
                }
            }
        } catch (e: Exception) {
            SheetResult(emptyMap(), "Sheet CSV request threw: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /** Minimal CSV split handling quoted fields (Google sometimes quotes numbers with commas). */
    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (c in line) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
        }
        result.add(current.toString())
        return result
    }
}
