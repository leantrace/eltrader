package com.leantrace.clients.binance

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.leantrace.converters.UnixTimestampDeserializer
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Created on 01.05.21
 * @author: Alexander Schamne <alexander.schamne@leantrace.ch>
 *
 */
data class CoinInformation(
    val coin: String,
    val free: BigDecimal,
    val depositAllEnable: Boolean,
    val freeze: BigDecimal,
    val ipoable: BigDecimal,
    val ipoing: BigDecimal,
    val isLegalMoney: Boolean,
    val locked: BigDecimal,
    val name: String,
    val networkList: List<NetworkList>,
    val storage: BigDecimal,
    val trading: Boolean,
    val withdrawAllEnable: Boolean,
    val withdrawing: BigDecimal
)

data class NetworkList(
    val addressRegex: String,
    val coin: String,
    val depositEnable: Boolean,
    val isDefault: Boolean,
    val memoRegex: String,
    val minConfirm: Int,
    val name: String,
    val network: String,
    val resetAddressStatus: Boolean,
    val unLockConfirm: Int,
    val withdrawEnable: Boolean,
    val withdrawFee: BigDecimal,
    val withdrawMin: BigDecimal,
    val depositDesc: String?,
    val withdrawDesc: String?,
    val specialTips: String?
)

data class BinanceSubscription(
    val method: String = "SUBSCRIBE",
    val params: List<String> = listOf(),
    val id: Int = 1
)

data class StreamResponse(
    val stream: String,
    val data: String
)

data class MiniTicker(
    @JsonAlias("s") val symbol: String,
    @JsonAlias("c") val value: String,
    @JsonAlias("e") val eventType: String,
    @JsonAlias("E") @JsonDeserialize(using = UnixTimestampDeserializer::class) val eventTime: LocalDateTime,
    @JsonAlias("o") val openPrice: BigDecimal,
    @JsonAlias("h") val highPrice: BigDecimal,
    @JsonAlias("l") val lowPrice: BigDecimal,
    @JsonAlias("v") val totalTradedBaseAssetVolume: BigDecimal,
    @JsonAlias("q") val totalTradedQuoteAssetVolume: BigDecimal
)


data class AggTrade(
    @JsonAlias("s") val symbol: String,
    @JsonAlias("p") val price: BigDecimal,
    @JsonAlias("q") val quantity: BigDecimal,
    @JsonAlias("m") val isBuyerMarketMaker: Boolean,
    @JsonAlias("e") val eventType: String,
    @JsonAlias("E") @JsonDeserialize(using = UnixTimestampDeserializer::class) val eventTime: LocalDateTime,
    @JsonAlias("a") val aggregateTradeID: Long,
    @JsonAlias("f") val firstTradeID: Long,
    @JsonAlias("l") val lastTradeID: Long,
    @JsonAlias("T") @JsonDeserialize(using = UnixTimestampDeserializer::class) val tradeTime: LocalDateTime,
    @JsonAlias("M") val ignore: Boolean
)

// Orders
enum class OrderSide { BUY, SELL }

enum class OrderType { LIMIT, MARKET }

enum class OrderTimeInForce {
    GTC, // Good Till Cancel
    IOC, // Immediate or Cancel
    FOK, // Fill or Kill
    GTX  // Good Till Crossing (Post Only)
}


data class OrderRequest(
    val symbol: String,
    val side: OrderSide,
    val type: OrderType,
    val quantity: String,
    val price: String,
    val timeInForce: OrderTimeInForce = OrderTimeInForce.GTX
)
