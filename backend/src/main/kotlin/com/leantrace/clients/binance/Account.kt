package com.leantrace.clients.binance

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.leantrace.converters.UnixTimestampDeserializer
import java.math.BigDecimal
import java.time.LocalDateTime

data class Account(
    val accountType: String,
    val balances: List<Balance>,
    val buyerCommission: Int,
    val canDeposit: Boolean,
    val canTrade: Boolean,
    val canWithdraw: Boolean,
    val makerCommission: BigDecimal,
    val permissions: List<String>,
    val sellerCommission: BigDecimal,
    val takerCommission: BigDecimal,
    @JsonDeserialize(using = UnixTimestampDeserializer::class) val updateTime: LocalDateTime
)
