package com.drdevrd.stockalerts.data

/**
 * Default watchlist seeded on first launch.
 *
 * NSE_DEFAULT: the Nifty 50 constituents (may drift slightly from the live index
 * over time as NSE reconstitutes it periodically - edit freely in-app).
 *
 * US_DEFAULT: a curated set of large-cap US names, NOT the full S&P 500.
 * Tracking all 500 daily on a free-tier key is impractical (rate limits +
 * no real user benefit from alerts on stocks you don't hold/watch).
 * Add any additional US ticker you want directly in the app.
 */
object DefaultWatchlist {

    val NSE_DEFAULT: List<Pair<String, String>> = listOf(
        "RELIANCE" to "Reliance Industries",
        "TCS" to "Tata Consultancy Services",
        "HDFCBANK" to "HDFC Bank",
        "ICICIBANK" to "ICICI Bank",
        "INFY" to "Infosys",
        "HINDUNILVR" to "Hindustan Unilever",
        "ITC" to "ITC",
        "SBIN" to "State Bank of India",
        "BHARTIARTL" to "Bharti Airtel",
        "KOTAKBANK" to "Kotak Mahindra Bank",
        "LT" to "Larsen & Toubro",
        "AXISBANK" to "Axis Bank",
        "BAJFINANCE" to "Bajaj Finance",
        "MARUTI" to "Maruti Suzuki",
        "ASIANPAINT" to "Asian Paints",
        "HCLTECH" to "HCL Technologies",
        "SUNPHARMA" to "Sun Pharma",
        "TITAN" to "Titan Company",
        "ULTRACEMCO" to "UltraTech Cement",
        "NESTLEIND" to "Nestle India",
        "WIPRO" to "Wipro",
        "ONGC" to "Oil & Natural Gas Corp",
        "NTPC" to "NTPC",
        "POWERGRID" to "Power Grid Corp",
        "M&M" to "Mahindra & Mahindra",
        "TATAMOTORS" to "Tata Motors",
        "TATASTEEL" to "Tata Steel",
        "ADANIENT" to "Adani Enterprises",
        "ADANIPORTS" to "Adani Ports",
        "COALINDIA" to "Coal India",
        "BAJAJFINSV" to "Bajaj Finserv",
        "TECHM" to "Tech Mahindra",
        "INDUSINDBK" to "IndusInd Bank",
        "JSWSTEEL" to "JSW Steel",
        "GRASIM" to "Grasim Industries",
        "DRREDDY" to "Dr. Reddy's Labs",
        "CIPLA" to "Cipla",
        "EICHERMOT" to "Eicher Motors",
        "HEROMOTOCO" to "Hero MotoCorp",
        "BRITANNIA" to "Britannia Industries",
        "DIVISLAB" to "Divi's Laboratories",
        "BPCL" to "Bharat Petroleum",
        "SBILIFE" to "SBI Life Insurance",
        "HDFCLIFE" to "HDFC Life Insurance",
        "APOLLOHOSP" to "Apollo Hospitals",
        "UPL" to "UPL Ltd",
        "TATACONSUM" to "Tata Consumer Products",
        "BAJAJ-AUTO" to "Bajaj Auto",
        "HINDALCO" to "Hindalco Industries",
        "SHREECEM" to "Shree Cement",
        "LTIM" to "LTIMindtree"
    )

    val US_DEFAULT: List<Pair<String, String>> = listOf(
        "AAPL" to "Apple",
        "MSFT" to "Microsoft",
        "GOOGL" to "Alphabet",
        "AMZN" to "Amazon",
        "NVDA" to "Nvidia",
        "META" to "Meta Platforms",
        "TSLA" to "Tesla",
        "BRK.B" to "Berkshire Hathaway",
        "LLY" to "Eli Lilly",
        "AVGO" to "Broadcom",
        "JPM" to "JPMorgan Chase",
        "V" to "Visa",
        "UNH" to "UnitedHealth Group",
        "XOM" to "ExxonMobil",
        "MA" to "Mastercard",
        "PG" to "Procter & Gamble",
        "COST" to "Costco",
        "HD" to "Home Depot",
        "MRK" to "Merck",
        "ABBV" to "AbbVie",
        "CVX" to "Chevron",
        "PEP" to "PepsiCo",
        "KO" to "Coca-Cola",
        "ADBE" to "Adobe",
        "WMT" to "Walmart",
        "BAC" to "Bank of America",
        "CRM" to "Salesforce",
        "NFLX" to "Netflix",
        "AMD" to "AMD",
        "ORCL" to "Oracle"
    )
}
