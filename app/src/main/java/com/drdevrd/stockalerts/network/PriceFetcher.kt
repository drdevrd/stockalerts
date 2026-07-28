package com.drdevrd.stockalerts.network

import com.drdevrd.stockalerts.data.Exchange

data class PriceResult(val current: Double, val previousClose: Double, val changePercent: Double)
data class FetchOutcome(val result: PriceResult?, val error: String?)

class PriceFetcher(finnhubApiKey: String) {

    private val finnhub = FinnhubApi(finnhubApiKey)
    private val nse = NseApi()

    /** Blocking call - always invoke from a background thread/coroutine. */
    fun fetch(symbol: String, exchange: Exchange): FetchOutcome {
        return when (exchange) {
            Exchange.US -> {
                val quote = finnhub.getQuote(symbol)
                if (quote != null) {
                    FetchOutcome(PriceResult(quote.current, quote.previousClose, quote.changePercent), null)
                } else {
                    FetchOutcome(null, "Finnhub returned no data for $symbol (check API key in Settings, or symbol may be wrong)")
                }
            }
            Exchange.NSE -> {
                val outcome = nse.getQuote(symbol)
                if (outcome.quote != null) {
                    FetchOutcome(PriceResult(outcome.quote.current, outcome.quote.previousClose, outcome.quote.changePercent), null)
                } else {
                    FetchOutcome(null, outcome.error ?: "Unknown NSE error")
                }
            }
        }
    }
}
