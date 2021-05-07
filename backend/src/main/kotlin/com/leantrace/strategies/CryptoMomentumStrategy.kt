package com.leantrace.strategies

import AppObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.leantrace.AppConfiguration
import com.leantrace.clients.binance.*
import com.leantrace.converters.UnixTimestampDeserializer
import com.leantrace.domain.BinanceOrder
import com.leantrace.domain.OrderStatus
import com.leantrace.domain.TimeInForce
import com.leantrace.domain.binanceorder.BinanceOrderService
import com.leantrace.exceptions.StrategyException
import com.leantrace.exceptions.TradingApiException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import org.ta4j.core.*
import org.ta4j.core.indicators.*
import org.ta4j.core.indicators.helpers.ClosePriceIndicator
import org.ta4j.core.num.DecimalNum
import org.ta4j.core.num.Num
import org.ta4j.core.rules.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.*
import java.util.*

@Component
@EnableConfigurationProperties(AppConfiguration::class)
class CryptoMomentumStrategy(
    val config: AppConfiguration,
    val binanceOrderService: BinanceOrderService,
    val apirest: BinanceServiceRest,
    val apiws: BinanceServiceWS
) {
    companion object {

        private val strategyName = "Crypto momentum strategy"
        private const val DECIMAL_FORMAT = "#.########"
        private val ZERO_DOT_ZERO_THREE: Num = DecimalNum.valueOf(0.03)
        private const val DURATION_OF_BAR = 1L

        private val depthCache = emptyMap<String, NavigableMap<BigDecimal, BigDecimal>>().toMutableMap()
        private val BIDS_KEY = "BIDS"
        private val ASKS_KEY = "ASKS"
        private lateinit var asks: NavigableMap<BigDecimal, BigDecimal>
        private lateinit var bids: NavigableMap<BigDecimal, BigDecimal>
        private val accountBalanceCache: TreeMap<String, Balance> = TreeMap()
        private var candlesticksCache: TreeMap<Long, CandlestickI> = TreeMap()
    }

    private var lastUpdateId: Long = 0
    private lateinit var listenKey: String

    private val tradingRecord: TradingRecord = BaseTradingRecord()
    private var latestPrice: BigDecimal = BigDecimal.ZERO
    private lateinit var series: BarSeries
    private lateinit var strategy: Strategy
    private lateinit var symbol: String
    private lateinit var baseCurrency: String
    private var lastOrder: Order? = null
    private var endIndex = 0
    private var amountOfBaseCurrencyToBuy: BigDecimal = BigDecimal.ZERO


    suspend fun init() {
        config.tradeables.split(",")
        baseCurrency = config.tradeables.split(",")[0]
        symbol = "${baseCurrency}${config.bridge}"
        listenKey = initializeAssetBalanceCacheAndStreamSession()
        GlobalScope.launch {
            startAccountBalanceEventStreaming(listenKey)
        }
        GlobalScope.launch {
            startDepthEventStreaming(symbol)
        }
        GlobalScope.launch {
            startCandlestickEventStreaming(symbol)
        }
        initializeDepthCache(symbol)
        initializeCandlestickCache(symbol)
        series = setupBarsSeries(symbol)
        strategy = buildStrategy(series)
    }

    private suspend fun initializeDepthCache(symbol: String) {
        logger.info("initialize DepthCache... for $symbol")
        val orderBook = apirest.getOrderBook(symbol.toUpperCase(), OrderBookLimit.TEN)
        asks = TreeMap(Comparator.reverseOrder())
        bids = TreeMap(Comparator.reverseOrder())
        orderBook.asks.forEach { asks[it.price] = it.qty }
        orderBook.bids.forEach { bids[it.price] = it.qty }
        depthCache[ASKS_KEY] = asks
        depthCache[BIDS_KEY] = bids
        logger.info("DepthCache... for $symbol initialized: $depthCache")
    }

    private suspend fun initializeAssetBalanceCacheAndStreamSession(): String {
        val account = apirest.getAccount()
        account.balances.forEach { accountBalanceCache[it.asset] = it }
        return apirest.userDataStream()
    }

    private suspend fun startDepthEventStreaming(symbol: String) {
        apiws.depth(listOf(symbol.toLowerCase())) { response ->
            if (response.finalUpdateIDInEvent > lastUpdateId) {
                lastUpdateId = response.finalUpdateIDInEvent
                updateOrderBook(asks, response.asks)
                updateOrderBook(bids, response.bids)
            }
        }
    }

    private suspend fun startAccountBalanceEventStreaming(listenKey: String) {
        apiws.userData(listenKey) { response ->
            if (response["e"]?.toString()?.equals(UserDataEventTypes.outboundAccountPosition.toString()) == true) {
                val ret = AppObjectMapper.mapper().convertValue(response, UserDataOutboundAccountPosition::class.java)
                // Override cached asset balances
                ret.balances.forEach {
                    accountBalanceCache[it.asset] = it
                }
                logger.info(accountBalanceCache.toString())
            }
        }
    }

    private fun updateOrderBook(
        lastOrderBookEntries: NavigableMap<BigDecimal, BigDecimal>,
        orderBookDeltas: List<OrderBookEntry>
    ) {
        orderBookDeltas.forEach {
            if (it.qty == BigDecimal.ZERO) {
                lastOrderBookEntries.remove(it.price)
            } else {
                lastOrderBookEntries[it.price] = it.qty
            }
        }
    }

    suspend fun startCandlestickEventStreaming(symbol: String) {
        apiws.candlestick(listOf(symbol), CandlestickInterval.ONE_MINUTE) { response ->
            val cs = response.candlestick
            val openTime: Long = cs.openTime.toInstant(ZoneOffset.UTC).toEpochMilli()
            candlesticksCache[openTime] = response.candlestick
            latestPrice = response.candlestick.closePrice
        }
    }

    suspend fun initializeCandlestickCache(symbol: String) {
        val candlestickBars: List<CandlestickArr> = apirest.getCandlestickBars(
            symbol.toUpperCase(),
            CandlestickInterval.ONE_MINUTE.intervalId
        )
        candlesticksCache = TreeMap<Long, CandlestickI>()
        candlestickBars.forEach {
            candlesticksCache[it.openTime.toInstant(ZoneOffset.UTC).toEpochMilli()] = it
        }
    }

    private suspend fun setupBarsSeries(symbol: String): BarSeries {
        series = BaseBarSeriesBuilder().withName(symbol).withNumTypeOf(DecimalNum::class.java).build()
        series.maximumBarCount = 50
        val candlesticks: List<CandlestickArr> = apirest.getCandlestickBars(symbol, CandlestickInterval.ONE_MINUTE.intervalId, series.maximumBarCount)
        candlesticks.forEach { series.addBar(convertCandleStickToBaseBar(it)) }
        return series
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    @Throws(StrategyException::class)
    suspend fun execute() {
        logger.info(symbol + " Checking order status..")
        val bestBidPrice: BigDecimal = bids.lastEntry().key
        val bestAskPrice: BigDecimal = asks.lastEntry().key
        val newestCandleStick = candlesticksCache[Collections.max(candlesticksCache.keys)]!!
        /*
         * Is this the first time the Strategy has been called? If yes, we initialise the OrderState so
         * we can keep track of orders during later cycles.
         */
        if (lastOrder == null) {
            logger.debug("$symbol First time Strategy has been called - creating new empty Order object.")
        }
        series.addBar(convertCandleStickToBaseBar(newestCandleStick))
        endIndex = series.endIndex
        logger.info(
            "<<< Adding new bar [" + newestCandleStick.openPrice + " - "
                    + newestCandleStick.highPrice + " - " + newestCandleStick.lowPrice + " - "
                    + newestCandleStick.closePrice + " to series] >>>"
        )
        logger.info((("AccountDetails: <<< Free amount of $baseCurrency on account to trade with: "
                + accountBalanceCache[baseCurrency.toUpperCase()]?.free) + " >>>"))
        logger.info((("AccountDetails: <<< Free amount of ${config.bridge} on account to trade with: "
                + accountBalanceCache[config.bridge.toUpperCase()]?.free) + " >>>"))
        logger.info("Best BID price=" + DecimalFormat(DECIMAL_FORMAT).format(bestBidPrice))
        logger.info("Best ASK price=" + DecimalFormat(DECIMAL_FORMAT).format(bestAskPrice))

        // Execute the appropriate algorithm based on the last order type.
        when {
            lastOrder == null -> {
                executeAlgoForWhenLastOrderWasNone(bestBidPrice)
            }
            lastOrder?.side === OrderSide.BUY -> {
                // executeAlgoForWhenLastOrderWasBuy(bestBidPrice)
            }
            lastOrder?.side === OrderSide.SELL -> {
                //executeAlgoForWhenLastOrderWasSell(bestBidPrice, bestAskPrice)
            }
        }
    }

    /*
   * Builds the buy and sell rules for this strategy
   *
   *
   * Buys when the trend is up and the shorts SMA crosses the long SMA or when the price dropped
   * with 5 % and the MACD is going up.Then check if the stochasticOscillator crossed down the value
   * of 20 and short the EMA is already over long the EMA.
   *
   *
   *
   * Sells if short EMA is going under long EMA, Signal 1. And check if MACD is going down or when
   * the loss is 3% or when a profit of 2% is made.
   *
   * @parameter Barseries series
   *
   * @return Strategy
   */
    fun buildStrategy(series: BarSeries): Strategy {
        val closePrice = ClosePriceIndicator(series)
        val cmo = CMOIndicator(closePrice, 9)
        val shortEma = EMAIndicator(closePrice, 9)
        val longEma = EMAIndicator(closePrice, 26)
        val stochasticOscillK = StochasticOscillatorKIndicator(series, 14)
        val macd = MACDIndicator(closePrice)
        val emaMacd = EMAIndicator(macd, 18)
        val shortSma = SMAIndicator(closePrice, 41)
        val longSma = SMAIndicator(closePrice, 14)
        val rsi = RSIIndicator(closePrice, 2)

        // Trend
        val momentumEntry: Rule = OverIndicatorRule(shortSma, longSma)
            .and(CrossedDownIndicatorRule(cmo, DecimalNum.valueOf(0)))
            .and(OverIndicatorRule(shortEma, closePrice))

        val percentage = series.lastBar.closePrice.multipliedBy(ZERO_DOT_ZERO_THREE)
        val closePriceMinPercentage = series.lastBar.closePrice.minus(percentage)

        val buy: Rule = CrossedUpIndicatorRule(shortSma, longSma).or(
            CrossedDownIndicatorRule(closePrice, closePriceMinPercentage)
                .and(OverIndicatorRule(macd, emaMacd))
                .and(CrossedDownIndicatorRule(stochasticOscillK, DecimalNum.valueOf(20)))
                .and(OverIndicatorRule(shortEma, longEma)).and(momentumEntry)
        )
        val momentumExit: Rule = UnderIndicatorRule(shortSma, longSma)
            .and(CrossedUpIndicatorRule(cmo, DecimalNum.valueOf(0)))
            .and(UnderIndicatorRule(shortSma, closePrice))
        val sell: Rule = UnderIndicatorRule(shortEma, longEma)
            .and(CrossedUpIndicatorRule(stochasticOscillK, DecimalNum.valueOf(80)))
            .and(UnderIndicatorRule(macd, emaMacd)).or(momentumExit)
            .or(StopLossRule(closePrice, DecimalNum.valueOf(3.0)))
            .or(
                StopGainRule(closePrice, DecimalNum.valueOf(2.0))
                    .and(UnderIndicatorRule(shortSma, closePrice))
                    .or(CrossedUpIndicatorRule(rsi, 95))
            )
        return BaseStrategy("Crypto Momentum Strategy ", buy, sell)
    }


    private fun convertCandleStickToBaseBar(cs: CandlestickI): Bar {
        return BaseBar(
            Duration.ofMinutes(DURATION_OF_BAR),
            ZonedDateTime.now(ZoneId.of("UTC")),
            cs.openPrice,
            cs.highPrice,
            cs.lowPrice,
            cs.closePrice,
            cs.baseAssetVolume,
            cs.numberOfTrades
        )
    }

    /**
     * Algo for executing when the Trading Strategy is invoked for the first time. We start off with a
     * buy order at current BID price.
     */
    @Throws(StrategyException::class)
    private suspend fun executeAlgoForWhenLastOrderWasNone(currentBidPrice: BigDecimal) {
        logger.info("$symbol OrderType is NONE - looking for new BUY order at [${DecimalFormat(DECIMAL_FORMAT).format(currentBidPrice)}]")
        try {
            if (strategy.shouldEnter(endIndex) ||true) {
                logger.info("$strategyName Sending initial BUY order to exchange --->")
                amountOfBaseCurrencyToBuy = getAmountOfBaseCurrencyToBuyForGivenCounterCurrencyAmount(config.bridgeAmount)
                val entered = tradingRecord.enter(
                    endIndex, DecimalNum.valueOf(currentBidPrice),
                    DecimalNum.valueOf(amountOfBaseCurrencyToBuy)
                )
                if (entered) {
                    logger.info("$strategyName should ENTER on $endIndex")
                    val newOrder = apirest.createOrder(OrderRequest(symbol, OrderSide.BUY, OrderType.MARKET, amountOfBaseCurrencyToBuy, currentBidPrice))
                    val binanceBuyOrder = BinanceOrder(
                        clientOrderId = newOrder.clientOrderId,
                        symbol = newOrder.symbol,
                        orderId = newOrder.orderId,
                        transactTime = newOrder.transactTime,
                        price = newOrder.price,
                        origQty = newOrder.origQty,
                        executedQty = newOrder.executedQty,
                        cummulativeQuoteQty = newOrder.cummulativeQuoteQty,
                        status = newOrder.status,
                        timeInForce = newOrder.timeInForce,
                        type = com.leantrace.domain.OrderType.valueOf(newOrder.type.toString()),
                        side = newOrder.side
                    )
                    lastOrder = newOrder
                    logger.info(
                        (strategyName
                                + " <<<<< ***** Yes, we've made a trade! Lets wait and see.. ****** \n " +
                                "Initial BUY Order sent successfully. ID: "
                                + lastOrder?.orderId + "********>>>>>")
                    )
                    binanceOrderService.create(binanceBuyOrder)
                }
            }
        } catch (e: Exception) {
            logger.error(symbol, e)
            throw StrategyException(e)
        }
    }

    @Throws(TradingApiException::class)
    private suspend fun getAmountOfBaseCurrencyToBuyForGivenCounterCurrencyAmount(amountOfCounterCurrencyToTrade: BigDecimal): BigDecimal {
        logger.info(
            symbol + " Calculating amount of base currency ($baseCurrency) to buy for amount of counter "
                    + "currency " + DecimalFormat(DECIMAL_FORMAT).format(amountOfCounterCurrencyToTrade)
                    + " " + config.bridge
        )

        // Fetch the last trade price
        val lastTradePrice = apirest.getLatestPrice(symbol).price
        logger.info(
            (symbol + " Last trade price for 1 " + baseCurrency + " was: "
                    + DecimalFormat(DECIMAL_FORMAT).format(lastTradePrice) + config.bridge)
        )
        val amountOfBaseCurrencyToBuy =
            amountOfCounterCurrencyToTrade.divide(lastTradePrice, 8, RoundingMode.HALF_DOWN)
        /**
         * Some pairs have a minimum trade size. We have to round the base currency to buy to the
         * minimum trade size if the base currency size is smaller then the minimum trade size.
         */
        // TODO: fix ^
        if ((symbol.toLowerCase() == "bnbbtc")) {
            val newAmountOfBaseCurrencyToBuy = amountOfBaseCurrencyToBuy.setScale(0, RoundingMode.UP)
            logger.info(
                (symbol + " Amount of base currency (" + baseCurrency
                        + ") to BUY for " + DecimalFormat(DECIMAL_FORMAT).format(newAmountOfBaseCurrencyToBuy)
                        + " " + config.bridge + " based on last market trade price: "
                        + newAmountOfBaseCurrencyToBuy)
            )
            return newAmountOfBaseCurrencyToBuy
        }
        logger.info(
            (symbol + " Amount of base currency (" + baseCurrency
                    + ") to BUY for " + DecimalFormat(DECIMAL_FORMAT).format(amountOfCounterCurrencyToTrade)
                    + " " + config.bridge + " based on last market trade price: "
                    + amountOfBaseCurrencyToBuy)
        )
        return amountOfBaseCurrencyToBuy
    }


}
