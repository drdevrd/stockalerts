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
 * (or block non-browser clients via bot detection) without notice.
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

    /** Result of a fetch attempt: either a quote, or a specific human-readable reason it failed. */
    data class Outcome(val quote: Quote?, val error: String?)

    private data class SessionResult(val cookieHeader: String?, val homepageStatus: Int, val error: String?)

    /** Hits the NSE homepage once to obtain the session cookies the API needs. */
    private fun primeSession(): SessionResult {
        return try {
            val request = Request.Builder()
                .url("https://www.nseindia.com/")
                .header("User-Agent", userAgent)
                .header("Accept", "text/html")
                .build()

            client.newCall(request).execute().use { response ->
                val cookies = response.headers("Set-Cookie")
                if (cookies.isEmpty()) {
                    SessionResult(null, response.code, "Homepage returned HTTP ${response.code} but no session cookies (likely bot-blocked)")
                } else {
                    SessionResult(cookies.joinToString("; ") { it.substringBefore(";") }, response.code, null)
                }
            }
        } catch (e: Exception) {
            SessionResult(null, -1, "Homepage request threw: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    fun getQuote(symbol: String): Outcome {
        val session = primeSession()
        val cookieHeader = session.cookieHeader
            ?: return Outcome(null, session.error ?: "Unknown session error")

        return try {
            val request = Request.Builder()
                .url("https://www.nseindia.com/api/quote-equity?symbol=$symbol")
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .header("Referer", "https://www.nseindia.com/get-quotes/equity?symbol=$symbol")
                .header("Cookie", cookieHeader)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Outcome(null, "Quote API returned HTTP ${response.code} for $symbol")
                }
                val body = response.body?.string()
                    ?: return Outcome(null, "Quote API returned empty body for $symbol")
                val json = JSONObject(body)
                val priceInfo = json.optJSONObject("priceInfo")
                    ?: return Outcome(null, "Quote API response missing priceInfo for $symbol (got: ${body.take(150)})")
                val current = priceInfo.optDouble("lastPrice", 0.0)
                if (current == 0.0) return Outcome(null, "Quote API returned zero price for $symbol")
                val prevClose = priceInfo.optDouble("previousClose", 0.0)
                val changePercent = priceInfo.optDouble("pChange", 0.0)
                Outcome(Quote(current, prevClose, changePercent), null)
            }
        } catch (e: Exception) {
            Outcome(null, "Quote request threw: ${e.javaClass.simpleName}: ${e.message}")
        }
    }
}
