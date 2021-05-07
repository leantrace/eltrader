package com.leantrace.domain

import com.fasterxml.jackson.annotation.JsonInclude
import com.leantrace.clients.binance.OrderSide
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document("orders")
class BinanceOrder(
    override var id: BinanceOrderId = createId(),
    created: LocalDateTime = LocalDateTime.now(),
    updated: LocalDateTime = LocalDateTime.now(),
    clientOrderId: String,
    symbol: String,
    orderId: Long,
    transactTime: LocalDateTime,
    price: BigDecimal,
    origQty: BigDecimal,
    executedQty: BigDecimal,
    cummulativeQuoteQty: BigDecimal,
    status: OrderStatus,
    timeInForce: TimeInForce,
    type: OrderType,
    side: OrderSide?,
    version: Int? = null
) : Trackable<BinanceOrder>(created, updated, version) {



    var clientOrderId = clientOrderId
        private set
    var symbol = symbol
        private set
    var orderId = orderId
        private set
    var transactTime = transactTime
        private set
    var price = price
        private set
    var origQty = origQty
        private set
    var executedQty = executedQty
        private set
    var cummulativeQuoteQty = cummulativeQuoteQty
        private set
    var status = status
        private set
    var timeInForce = timeInForce
        private set
    var type = type
        private set
    var side = side
        private set


}

enum class OrderType {
    LIMIT, MARKET, STOP_LOSS, STOP_LOSS_LIMIT, TAKE_PROFIT, TAKE_PROFIT_LIMIT, LIMIT_MAKER
}

enum class OrderStatus {
    NEW, PARTIALLY_FILLED, FILLED, CANCELED, PENDING_CANCEL, REJECTED, EXPIRED
}

enum class OrderSide {
    BUY, SELL
}

/**
 * Time in force to indicate how long an order will remain active before it is executed or expires.
 *
 * GTC (Good-Til-Canceled) orders are effective until they are executed or canceled.
 * IOC (Immediate or Cancel) orders fills all or part of an order immediately and cancels the remaining part of the order.
 * FOK (Fill or Kill) orders fills all in its entirety, otherwise, the entire order will be cancelled.
 *
 * @see <a href="http://www.investopedia.com/terms/t/timeinforce.asp">http://www.investopedia.com/terms/t/timeinforce.asp</a>
 */
enum class TimeInForce {
    GTC, IOC, FOK
}

typealias BinanceOrderId = String
