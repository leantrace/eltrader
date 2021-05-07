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
    val clientOrderId: Long,
    val cummulativeQuoteQty: BigDecimal,
    val executedQty: BigDecimal,
    val fills: List<Fill>,
    val orderId: Long,
    val orderListId: Long,
    val origQty: BigDecimal,
    val price: BigDecimal,
    val symbol: String,
    val side: OrderSide?,
    val status: OrderStatus,
    val timeInForce: TimeInForce,
    @JsonDeserialize(using = UnixTimestampDeserializer::class) val transactTime: LocalDateTime,
    val type: OrderType
)

data class Fill(
    val commission: BigDecimal,
    val commissionAsset: String,
    val price: BigDecimal,
    val qty: BigDecimal
)
