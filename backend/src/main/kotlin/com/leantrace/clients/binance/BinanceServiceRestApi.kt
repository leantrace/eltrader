package com.leantrace.clients.binance

import io.ktor.client.statement.*


interface BinanceServiceRestApi {
    suspend fun createOrder(orderRequest: OrderRequest): HttpResponse

    suspend fun coinsInformation(): List<CoinInformation>

    suspend fun batchOrders(orderRequests: List<OrderRequest>): HttpResponse

    suspend fun getAccount(): Account

    suspend fun startUserDataStream(): String

    suspend fun getLatestPrice(symbol: String): Price

    suspend fun getCandlestickBars(symbol: String, interval: String, limit: Int? = null, startTime: Long? = null, endTime: Long? = null): List<Candlestick>
}
