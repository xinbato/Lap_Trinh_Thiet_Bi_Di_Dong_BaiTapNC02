package com.example.baitap02.data

import com.google.gson.annotations.SerializedName

data class CoinItem(
    val id: String,
    val symbol: String,
    val name: String,
    val image: String,
    @SerializedName("market_cap_rank") val rank: Int,
    @SerializedName("current_price") val currentPrice: Double,
    @SerializedName("total_volume") val totalVolume: Double,
    @SerializedName("market_cap") val marketCap: Double,
    @SerializedName("price_change_percentage_1h_in_currency") val change1h: Double?,
    @SerializedName("price_change_percentage_24h_in_currency") val change24h: Double?,
    @SerializedName("price_change_percentage_7d_in_currency") val change7d: Double?,
    @SerializedName("sparkline_in_7d") val sparkline: Sparkline?
)

data class Sparkline(
    val price: List<Double>?
)

data class GlobalResponse(
    val data: GlobalData
)

data class GlobalData(
    @SerializedName("active_cryptocurrencies") val activeCryptos: Int = 0,
    @SerializedName("markets") val marketsCount: Int = 0,
    @SerializedName("total_market_cap") val totalMarketCap: Map<String, Double>? = null,
    @SerializedName("total_volume") val totalVolume: Map<String, Double>? = null,
    @SerializedName("market_cap_percentage") val dominancePercentage: Map<String, Double>? = null,
    @SerializedName("market_cap_change_percentage_24h_usd") val marketCapChange24h: Double? = null,
    @SerializedName("fear_and_greed_index") val fearAndGreed: FearAndGreed? = null
)

data class FearAndGreed(
    val value: Int = 0,
    val sentiment: String = ""
)
