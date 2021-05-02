package com.leantrace.clients.binance

import java.math.BigDecimal

data class Price(
    val price: BigDecimal,
    val symbol: String
)
