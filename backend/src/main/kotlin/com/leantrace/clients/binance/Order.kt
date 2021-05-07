package com.leantrace.clients.binance


import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.leantrace.converters.UnixTimestampDeserializer
import com.leantrace.domain.OrderStatus
import com.leantrace.domain.TimeInForce
import java.math.BigDecimal
import java.time.LocalDateTime

data class Order(
    val symbol: String,
    val orderId: Long,
    val orderListId: Long,
    val clientOrderId: String,
    @JsonDeserialize(using = UnixTimestampDeserializer::class) val transactTime: LocalDateTime,
    val price: BigDecimal,
    val origQty: BigDecimal,
    val executedQty: BigDecimal,
    val cummulativeQuoteQty: BigDecimal,
    val status: OrderStatus,
    val timeInForce: TimeInForce,
    val type: OrderType,
    val side: OrderSide? = null,
    val fills: List<Fill> = emptyList()
)

data class Fill(
    val commission: BigDecimal,
    val commissionAsset: String,
    val price: BigDecimal,
    val qty: BigDecimal
)
