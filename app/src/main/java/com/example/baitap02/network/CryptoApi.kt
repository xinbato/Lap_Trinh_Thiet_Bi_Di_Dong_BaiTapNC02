package com.example.baitap02.network

import com.example.baitap02.data.CoinItem
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface CryptoApi {
    @GET("coins/markets")
    suspend fun getMarkets(
        @Query("vs_currency") currency: String = "usd",
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1,
        @Query("sparkline") sparkline: Boolean = true,
        @Query("price_change_percentage") changeIntervals: String = "1h,24h,7d"
    ): List<CoinItem>

    @GET("global")
    suspend fun getGlobal(): com.example.baitap02.data.GlobalResponse

    companion object {
        private const val BASE_URL = "https://api.coingecko.com/api/v3/"

        val instance: CryptoApi by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CryptoApi::class.java)
        }
    }
}
