package com.leantrace.clients.binance

data class Balance(
    val asset: String,
    val free: String,
    val locked: String
)
