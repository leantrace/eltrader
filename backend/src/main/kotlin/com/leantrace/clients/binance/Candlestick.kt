package com.leantrace.clients.binance

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyOrder

/**
 * Kline/Candlestick bars for a symbol. Klines are uniquely identified by their open time.
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder
@JsonIgnoreProperties(ignoreUnknown = true)
data class Candlestick(
    val openTime: Long,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val volume: String,
    val closeTime: Long,
    val quoteAssetVolume: String,
    val numberOfTrades: Long,
    val takerBuyBaseAssetVolume: String,
    val takerBuyQuoteAssetVolume: String
)


/**
 * Kline/Candlestick intervals.
 * m -> minutes; h -> hours; d -> days; w -> weeks; M -> months
 */
@JsonIgnoreProperties(ignoreUnknown = true)
enum class CandlestickInterval(val intervalId: String) {
    ONE_MINUTE("1m"),
    THREE_MINUTES("3m"),
    FIVE_MINUTES("5m"),
    FIFTEEN_MINUTES("15m"),
    HALF_HOURLY("30m"),
    HOURLY("1h"),
    TWO_HOURLY("2h"),
    FOUR_HOURLY("4h"),
    SIX_HOURLY("6h"),
    EIGHT_HOURLY("8h"),
    TWELVE_HOURLY("12h"),
    DAILY("1d"),
    THREE_DAILY("3d"),
    WEEKLY("1w"),
    MONTHLY("1M")
}
