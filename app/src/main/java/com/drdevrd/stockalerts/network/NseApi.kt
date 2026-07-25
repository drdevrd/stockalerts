package com.drdevrd.stockalerts.network

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Client for NSE India's public (unofficial) quote endpoint.
 * Free, no API key - but NSE requires a valid session cookie obtained by
 * first hitting the homepage, and a realistic browser User-Agent, or it
 * will reject the request. This is undocumented and NSE can change it
 * without notice, so failures should be handled gracefully (skip that
 * symbol today rather than crash the whole batch).
 */
class NseApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    data class Quote(val current: Double, val previousClose: Double, val changePercent: Double)

    /** Hits the NSE homepage once to obtain the session cookies the API needs. */
    private fun primeSession(): String? {
        val request = Request.Builder()
            .url("https://www.nseindia.com/")
            .header("User-Agent", userAgent)
            .header("Accept", "text/html")
            .build()

        client.newCall(request).execute().use { response ->
            val cookies = response.headers("Set-Cookie")
            if (cookies.isEmpty()) return null
            return cookies.joinToString("; ") { it.substringBefore(";") }
        }
    }

    /** Returns null if the request or parsing fails - caller should skip this symbol. */
    fun getQuote(symbol: String): Quote? {
        val cookieHeader = primeSession() ?: return null

        val request = Request.Builder()
            .url("https://www.nseindia.com/api/quote-equity?symbol=$symbol")
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .header("Referer", "https://www.nseindia.com/get-quotes/equity?symbol=$symbol")
            .header("Cookie", cookieHeader)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val priceInfo = json.optJSONObject("priceInfo") ?: return null
            val current = priceInfo.optDouble("lastPrice", 0.0)
            if (current == 0.0) return null
            val prevClose = priceInfo.optDouble("previousClose", 0.0)
            val changePercent = priceInfo.optDouble("pChange", 0.0)
            return Quote(current, prevClose, changePercent)
        }
    }
}
