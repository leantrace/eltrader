package com.leantrace.strategies

import com.leantrace.AppConfiguration
import com.leantrace.clients.binance.*
import com.leantrace.domain.binanceorder.BinanceOrderService
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import java.math.BigDecimal
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
        private val depthCache = emptyMap<String, NavigableMap<BigDecimal, BigDecimal>>().toMutableMap()
    }
    private val BIDS_KEY = "BIDS"
    private val ASKS_KEY = "ASKS"
    private lateinit var asks: NavigableMap<BigDecimal, BigDecimal>
    private lateinit var bids: NavigableMap<BigDecimal, BigDecimal>
    private lateinit var accountBalanceCache: TreeMap<String, Balance>

    private var lastUpdateId: Long = 0
    private lateinit var listenKey: String

    init {
        config.tradeables.split(",")
        val symbol = "${config.tradeables.split(",")[0]}${config.bridge}"
        runBlocking {
            listenKey = initializeAssetBalanceCacheAndStreamSession()
            //startAccountBalanceEventStreaming(listenKey)
            initializeDepthCache(symbol)
            asks = depthCache[ASKS_KEY]!!
            bids = depthCache[BIDS_KEY]!!
            startDepthEventStreaming(symbol)
        }
    }

    private suspend fun initializeDepthCache(symbol: String) {
        logger.info("initialize DepthCache... for $symbol")
        val orderBook = apirest.getOrderBook(symbol.toUpperCase(), OrderBookLimit.TEN)
        val a: NavigableMap<BigDecimal, BigDecimal> = TreeMap(Comparator.reverseOrder())
        val b: NavigableMap<BigDecimal, BigDecimal> = TreeMap(Comparator.reverseOrder())
        orderBook.asks.forEach { a[it.price] = it.qty }
        orderBook.bids.forEach { b[it.price] = it.qty }
        depthCache[ASKS_KEY] = a
        depthCache[BIDS_KEY] = b
        logger.info("DepthCache... for $symbol initialized: $depthCache")
    }

    private suspend fun initializeAssetBalanceCacheAndStreamSession(): String {
        val account = apirest.getAccount()
        account.balances.forEach { accountBalanceCache[it.asset] = it }
        return apirest.startUserDataStream()
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

    /*private fun startAccountBalanceEventStreaming(listenKey: String) {
        val webSocketClient: BinanceApiWebSocketClient =
            BinanceApiClientFactory.newInstance(apiKey, secret).newWebSocketClient()
        webSocketClient.onUserDataUpdateEvent(listenKey) { response ->
            if (response.getEventType() === UserDataUpdateEvent.UserDataUpdateEventType.ACCOUNT_UPDATE) {
                // Override cached asset balances
                for (assetBalance in response.getAccountUpdateEvent().getBalances()) {
                    accountBalanceCache[assetBalance.getAsset()] = assetBalance
                }
                println(accountBalanceCache)
            }
        }
    }*/

    private fun updateOrderBook(lastOrderBookEntries: NavigableMap<BigDecimal, BigDecimal>, orderBookDeltas: List<OrderBookEntry>) {
        orderBookDeltas.forEach {
            if (it.qty == BigDecimal.ZERO) {
                lastOrderBookEntries.remove(it.price)
            } else {
                lastOrderBookEntries[it.price] = it.qty
            }
        }
    }


    private val logger = LoggerFactory.getLogger(javaClass)
    suspend fun execute() {
        val fullMarketName = config.tradeables
        logger.info("$fullMarketName Checking order status..")
        // val bestBidPrice: BigDecimal = apiService.bestBid.key
        // val bestAskPrice: BigDecimal = apiService.bestAsk.key
    }
}
