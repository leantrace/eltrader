package com.leantrace.clients.binance

fun interface BinanceApiCallback<T> {
    fun onResponse(var1: T)
    fun onFailure(cause: Throwable) {}
}

interface BinanceServiceWSApi {
}
