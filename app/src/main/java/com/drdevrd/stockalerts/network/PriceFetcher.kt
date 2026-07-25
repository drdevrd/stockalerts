package com.drdevrd.stockalerts.network

import com.drdevrd.stockalerts.data.Exchange

data class PriceResult(val current: Double, val previousClose: Double, val changePercent: Double)

class PriceFetcher(finnhubApiKey: String) {

    private val finnhub = FinnhubApi(finnhubApiKey)
    private val nse = NseApi()

    /** Blocking call - always invoke from a background thread/coroutine. */
    fun fetch(symbol: String, exchange: Exchange): PriceResult? {
        return when (exchange) {
            Exchange.US -> finnhub.getQuote(symbol)?.let {
                PriceResult(it.current, it.previousClose, it.changePercent)
            }
            Exchange.NSE -> nse.getQuote(symbol)?.let {
                PriceResult(it.current, it.previousClose, it.changePercent)
            }
        }
    }
}
