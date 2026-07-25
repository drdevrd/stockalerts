package com.drdevrd.stockalerts.network

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Thin client for Finnhub's /quote endpoint (US stock real-time quotes).
 * Free tier does NOT cover NSE/international quotes - see NseApi for that.
 */
class FinnhubApi(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    data class Quote(val current: Double, val previousClose: Double, val changePercent: Double)

    /** Returns null if the request fails or the symbol has no data. */
    fun getQuote(symbol: String): Quote? {
        val url = "https://finnhub.io/api/v1/quote?symbol=$symbol&token=$apiKey"
        val request = Request.Builder().url(url).get().build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val current = json.optDouble("c", 0.0)
            if (current == 0.0) return null
            val prevClose = json.optDouble("pc", 0.0)
            val changePercent = json.optDouble("dp", 0.0)
            return Quote(current, prevClose, changePercent)
        }
    }
}
