package com.leantrace.clients.binance

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyOrder
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

data class TickerPrice(
    val symbol: String,
    val price: BigDecimal,
)

data class UserData(
    val outboundAccountPosition: UserDataOutboundAccountPosition? = null,
    val executionReport: UserDataOutboundAccountPosition? = null,
    val balanceUpdate: UserDataOutboundAccountPosition? = null,
    val listStatus: UserDataOutboundAccountPosition? = null
)

data class CandlestickRec(
    @JsonAlias("e") val eventType: String,
    @JsonAlias("E") @JsonDeserialize(using = UnixTimestampDeserializer::class) val eventTime: LocalDateTime,
    @JsonAlias("k") val candlestick: Candlestick,
    @JsonAlias("s") val symbol: String,
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

interface CandlestickI {
    val openPrice: BigDecimal
    val highPrice: BigDecimal
    val lowPrice: BigDecimal
    val closePrice: BigDecimal
    val baseAssetVolume: BigDecimal
    val closeTime: LocalDateTime
    val quoteAssetVolume: BigDecimal
    val numberOfTrades: BigDecimal
    val takerBuyQuoteAssetVolume: BigDecimal
    val takerBuyBaseAssetVolume: BigDecimal
}

data class Candlestick(
    @JsonAlias("o") override val openPrice: BigDecimal,
    @JsonAlias("h") override val highPrice: BigDecimal,
    @JsonAlias("l") override val lowPrice: BigDecimal,
    @JsonAlias("c") override val closePrice: BigDecimal,
    @JsonAlias("v") override val baseAssetVolume: BigDecimal,
    @JsonAlias("q") override val quoteAssetVolume: BigDecimal,
    @JsonAlias("n") override val numberOfTrades: BigDecimal,
    @JsonAlias("Q") override val takerBuyQuoteAssetVolume: BigDecimal,
    @JsonAlias("V") override val takerBuyBaseAssetVolume: BigDecimal,
    @JsonAlias("T") override @JsonDeserialize(using = UnixTimestampDeserializer::class) val closeTime: LocalDateTime,
    @JsonAlias("t") @JsonDeserialize(using = UnixTimestampDeserializer::class) val openTime: LocalDateTime,
    @JsonAlias("s") val symbol: String,
    @JsonAlias("B") val bIgnore: Long,
    @JsonAlias("f") val firstTradeID: Long,
    @JsonAlias("i") val interval: String,
    @JsonAlias("L") val lastTradeID: Long,
    @JsonAlias("x") val isCandlestickClosed: Boolean
) : CandlestickI

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder
@JsonIgnoreProperties(ignoreUnknown = true)
data class CandlestickArr(
    @JsonDeserialize(using = UnixTimestampDeserializer::class) val openTime: LocalDateTime,
    override val openPrice: BigDecimal,
    override val highPrice: BigDecimal,
    override val lowPrice: BigDecimal,
    override val closePrice: BigDecimal,
    override val baseAssetVolume: BigDecimal,
    override @JsonDeserialize(using = UnixTimestampDeserializer::class) val closeTime: LocalDateTime,
    override val quoteAssetVolume: BigDecimal,
    override val numberOfTrades: BigDecimal,
    override val takerBuyQuoteAssetVolume: BigDecimal,
    override val takerBuyBaseAssetVolume: BigDecimal,
) : CandlestickI


enum class UserDataEventTypes {
    listStatus, executionReport, balanceUpdate, outboundAccountPosition
}

data class UserDataListStatusOrders(
    @JsonAlias("c") val clientOrderId: String,
    @JsonAlias("i") val orderId: Int,
    @JsonAlias("s") val symbol: String
)
data class UserDataListStatus(
    @JsonAlias("e") val eventType: String,
    @JsonAlias("E") @JsonDeserialize(using = UnixTimestampDeserializer::class) val eventTime: LocalDateTime,
    @JsonAlias("c") val contingencyType: String,
    @JsonAlias("C") val listClientOrderID: String,
    @JsonAlias("g") val orderListId: Long,
    @JsonAlias("l") val listStatusType: String,
    @JsonAlias("L") val listOrderStatus: String,
    @JsonAlias("O") val orders: List<UserDataListStatusOrders>,
    @JsonAlias("r") val orderRejectReason: String,
    @JsonAlias("s") val symbol: String,
    @JsonAlias("T") @JsonDeserialize(using = UnixTimestampDeserializer::class) val transactionTime: LocalDateTime,
)
data class UserDataOutboundAccountPosition(
    @JsonAlias("e") val eventType: String,
    @JsonAlias("E") @JsonDeserialize(using = UnixTimestampDeserializer::class) val eventTime: LocalDateTime,
    @JsonAlias("u") @JsonDeserialize(using = UnixTimestampDeserializer::class) val lastAccountUpdate: LocalDateTime,
    @JsonAlias("B") val balances: List<Balance>
)

data class UserDataBalanceUpdate(
    @JsonAlias("e") val eventType: String,
    @JsonAlias("E") @JsonDeserialize(using = UnixTimestampDeserializer::class) val eventTime: LocalDateTime,
    @JsonAlias("a") val asset: String,
    @JsonAlias("d") val balanceDelta: BigDecimal,
    @JsonAlias("T") @JsonDeserialize(using = UnixTimestampDeserializer::class) val clearTime: LocalDateTime,
)

data class UserDataExecutionUpdate(
    @JsonAlias("e") val eventType: String,
    @JsonAlias("E") @JsonDeserialize(using = UnixTimestampDeserializer::class) val eventTime: LocalDateTime,
    @JsonAlias("c") val clientOrderID: String,
    @JsonAlias("C") val originalClientOrderID: String,
    @JsonAlias("f") val timeInForce: String,
    @JsonAlias("F") val icebergQuantity: BigDecimal,
    @JsonAlias("g") val orderListId: Long,
    @JsonAlias("i") val orderID: Long,
    @JsonAlias("I") val iIgnore: Long,
    @JsonAlias("l") val lastExecutedQuantity: BigDecimal,
    @JsonAlias("L") val lastExecutedPrice: BigDecimal,
    @JsonAlias("m") val isTradeMakerSide: Boolean,
    @JsonAlias("M") val mIgnore: Boolean,
    @JsonAlias("n") val commissionAmount: BigDecimal,
    @JsonAlias("N") val commissionAsset: String?,
    @JsonAlias("o") val orderType: String,
    @JsonAlias("O") @JsonDeserialize(using = UnixTimestampDeserializer::class) val orderCreationTime: LocalDateTime,
    @JsonAlias("p") val orderPrice: BigDecimal,
    @JsonAlias("P") val stopPrice: BigDecimal,
    @JsonAlias("q") val orderQuantity: BigDecimal,
    @JsonAlias("Q") val quoteOrderQuantity: BigDecimal,
    @JsonAlias("r") val orderRejectReason: String,
    @JsonAlias("s") val symbol: String,
    @JsonAlias("S") val side: String,
    @JsonAlias("T") @JsonDeserialize(using = UnixTimestampDeserializer::class) val transactionTime: LocalDateTime,
    @JsonAlias("t") val tradeID: Long,
    @JsonAlias("w") val isOrderOnTheBook: Boolean,
    @JsonAlias("x") val currentExecutionType: String,
    @JsonAlias("X") val currentOrderStatus: String,
    @JsonAlias("Y") val lastQuoteAssetTransactedQuantity: BigDecimal,
    @JsonAlias("z") val cumulativeFilledQuantity: BigDecimal,
    @JsonAlias("Z") val cumulativeQuoteAssetTransactedQuantity: BigDecimal
)


data class DiffDepth(
    @JsonAlias("e") val eventType: String,
    @JsonAlias("E") @JsonDeserialize(using = UnixTimestampDeserializer::class) val eventTime: LocalDateTime,
    @JsonAlias("s") val symbol: String,
    @JsonAlias("U") val firstUpdateIDInEvent: Long,
    @JsonAlias("u") val finalUpdateIDInEvent: Long,
    @JsonAlias("b") val bids: List<OrderBookEntry>,
    @JsonAlias("a") val asks: List<OrderBookEntry>
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
    val quantity: BigDecimal,
    val price: BigDecimal? = null,
    val timeInForce: OrderTimeInForce = OrderTimeInForce.GTX
)
