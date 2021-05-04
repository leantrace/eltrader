package com.leantrace.clients.binance

import AppObjectMapper
import com.fasterxml.jackson.core.type.TypeReference
import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.apache.commons.codec.binary.Hex
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


/**
 *
 *
 * Created on 30.04.21
 * @author: Alexander Schamne <alexander.schamne@gmail.com>
 *
 */
@Service
class BinanceService : BinanceServiceRestApi, BinanceServiceWSApi {

    val baseUrl = "https://api.binance.com"
    val wsBaseUrl = "wss://stream.binance.com:9443/stream"
    final val dotenv = dotenv()
    var apiKey = dotenv["BINANCE_API_KEY"]
    val apiSecret = dotenv["BINANCE_API_SECRET"]
    final val tradables = dotenv["TRADEABLES"].split(",")
    val bridge = dotenv["BRIDGE"]
    val defaultTickers = tradables.map { "$it$bridge" }

    val miniTickers = mutableMapOf<String, MiniTicker>()


    private lateinit var listenKey: String
    private val INTERVAL: CandlestickInterval = CandlestickInterval.ONE_MINUTE
    private val lastUpdateId: Long = 0
    private val secret: String? = null
    private val symbol: String? = null
    private var candlesticksCache: Map<Long, Candlestick> = emptyMap()
    private val accountBalanceCache: Map<String, Balance> = emptyMap()

    init {
        //listenKey = initializeAssetBalanceCacheAndStreamSession()
    }


    val client = HttpClient {
        install(WebSockets)
    }

    private val logger = KotlinLogging.logger {}

    private fun signRequest(data: String) = createSignature(data, apiSecret)

    private fun createSignature(data: String, key: String): String {
        val sha256Hmac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key.toByteArray(), "HmacSHA256")
        sha256Hmac.init(secretKey)
        return Hex.encodeHexString(sha256Hmac.doFinal(data.toByteArray()))
    }

    private fun extractQueryParamsAsText(sourceBuilder: URLBuilder): String {
        val builderCopy = URLBuilder(sourceBuilder)
        return builderCopy.build().fullPath.substringAfter("?")
    }

    private fun URLBuilder.signedParameters(block: URLBuilder.() -> Unit) {
        val currentBuilder = this
        currentBuilder.block()
        val queryParamsAsText = extractQueryParamsAsText(currentBuilder)
        parameters.append("signature", signRequest(queryParamsAsText))
    }

    override suspend fun createOrder(orderRequest: OrderRequest): HttpResponse {
        return client.post("$baseUrl/order") {
            header("X-MBX-APIKEY", apiKey)
            url {
                signedParameters {
                    parameter("symbol", orderRequest.symbol)
                    parameter("side", orderRequest.side)
                    parameter("type", orderRequest.type)
                    parameter("quantity", orderRequest.quantity)
                    parameter("price", orderRequest.price)
                    parameter("timeInForce", orderRequest.timeInForce)
                    parameter("recvWindow", 5000)
                    parameter("timestamp", Instant.now().toEpochMilli())
                }
            }
        }
    }
    /**
     *
     *
     *
     *
     * APIs
     *
     *
     *
     *
     *
     */
    override suspend fun coinsInformation(): List<CoinInformation> {
        val r = client.get<String>("$baseUrl/sapi/v1/capital/config/getall") {
            header("X-MBX-APIKEY", apiKey)
            url {
                signedParameters {
                    //parameter("recvWindow", 1L)
                    parameter("timestamp", Instant.now().toEpochMilli())
                }
            }
        }
        return withContext(Dispatchers.IO) {
            AppObjectMapper.mapper().readValue(r, object : TypeReference<List<CoinInformation>>() {})
        }
    }

    override suspend fun batchOrders(orderRequests: List<OrderRequest>): HttpResponse {
        val batchOrdersParamValue = ""//format.encodeToString(orderRequests)
        logger.info { batchOrdersParamValue }

        return client.post("$baseUrl/batchOrders") {
            header("X-MBX-APIKEY", apiKey)
            url {
                signedParameters {
                    parameter("batchOrders", batchOrdersParamValue)
                    parameter("recvWindow", 5000)
                    parameter("timestamp", Instant.now().toEpochMilli())
                }
            }
        }

    }

    override suspend fun performWebSocket() {
        logger.info { "Connecting..." }
        client.wss(
            urlString = wsBaseUrl
        ) {
            logger.info { "Connected!" }

            val binanceSubscription = BinanceSubscription(
                //params = listOf("!miniTicker@arr")
                params = defaultTickers.map { symbol -> "${symbol.toLowerCase()}@miniTicker" }
                    .plus(defaultTickers.map { symbol -> "${symbol.toLowerCase()}@aggTrade" })
                //    .plus(defaultTickers.map { symbol -> "${symbol.toLowerCase()}@forceOrder" }
                //        .plus(defaultTickers.map { symbol -> "${symbol.toLowerCase()}@kline_1m" })))
            )

            //logger.info { format.encodeToString(binanceSubscription) }
            withContext(Dispatchers.IO) {
                send(AppObjectMapper.mapper().writeValueAsString(binanceSubscription))
            }

            try {
                for (frame in incoming) {
                    val text = (frame as Frame.Text).readText()
                    val binanceElement = AppObjectMapper.mapper().readValue(text, Map::class.java)
                    val stream = binanceElement["stream"]
                    val data = binanceElement["data"]
                    if (stream == null) {
                        logger.info { "NULL: $binanceElement" }
                    } else {
                        when (val it = stream.toString().split('@').last()) {
                            "miniTicker" -> {
                                val miniTicker = AppObjectMapper.mapper().convertValue(data, MiniTicker::class.java)
                                //logger.info { miniTicker }
                                miniTickers[miniTicker.symbol] = miniTicker
                            }
                            "aggTrade" -> {
                                val aggTrade = AppObjectMapper.mapper().convertValue(data, AggTrade::class.java)
                                logger.info { aggTrade }
                                //logger.info { aggTrade.quantity * aggTrade.price }
                                if (aggTrade.quantity * aggTrade.price >= BigDecimal(10000)) {
                                    logger.info {
                                        "[${aggTrade.symbol}] ${aggTrade.quantity * aggTrade.price} " +
                                                if (aggTrade.isBuyerMarketMaker) "SELL" else "BUY"
                                    }

                                    val orderRequest = OrderRequest(
                                        aggTrade.symbol, OrderSide.BUY, OrderType.LIMIT,
                                        aggTrade.price.toString(), "34000.0"
                                    )
                                    //callWithRetries { createOrder(orderRequest) }
                                }
                            }
                            else -> logger.info { "Could not decode $it" }
                        }

                    }

                }
            } catch (e: ClosedReceiveChannelException) {
                logger.warn(e) { "Error received" }
                logger.warn("onClose ${closeReason.await()}")
            } catch (e: Throwable) {
                logger.warn(e) { "Error received" }
                logger.warn("onError ${closeReason.await()}")
            }

        }

    }

    override suspend fun getAccount(): Account {
        val r = client.get<String>("$baseUrl/api/v3/account") {
            header("X-MBX-APIKEY", apiKey)
            url {
                signedParameters {
                    //parameter("recvWindow", 1L)
                    parameter("timestamp", Instant.now().toEpochMilli())
                }
            }
        }
        return withContext(Dispatchers.IO) {
            AppObjectMapper.mapper().readValue(r, Account::class.java)
        }
    }

    override suspend fun startUserDataStream(): String {
        val r = client.post<String>("$baseUrl/api/v3/userDataStream") {
            header("X-MBX-APIKEY", apiKey)
        }
        return AppObjectMapper.mapper().readValue(r, Map::class.java)["listenKey"]!!.toString()
    }

    override suspend fun getLatestPrice(symbol: String): Price {
        val r = client.get<String>("$baseUrl/api/v3/ticker/price") {
            header("X-MBX-APIKEY", apiKey)
            url {
                parameter("symbol", symbol)
            }
        }
        return AppObjectMapper.mapper().readValue(r, Price::class.java)
    }

    override suspend fun getCandlestickBars(symbol: String, interval: String, limit: Int?,
                                            startTime: Long?, endTime: Long?
    ): List<Candlestick> {
        val r = client.get<String>("$baseUrl/api/v1/klines") {
            header("X-MBX-APIKEY", apiKey)
            url {
                parameter("symbol", symbol)
                parameter("interval", interval)
                limit?.let { parameter("limit", limit) }
                startTime?.let { parameter("startTime", startTime) }
                endTime?.let { parameter("endTime", endTime) }
            }
        }
        return withContext(Dispatchers.IO) {
            AppObjectMapper.mapper().readValue(r, object : TypeReference<List<Candlestick>>() {})
        }
    }



    override suspend fun getOrderBook(symbol: String, limit: OrderBookLimit?): OrderBook {
        val r = client.get<String>("$baseUrl/api/v3/depth") {
            header("X-MBX-APIKEY", apiKey)
            url {
                parameter("symbol", symbol)
                limit?.let { parameter("limit", limit.limit) }
            }
        }
        return withContext(Dispatchers.IO) {
            AppObjectMapper.mapper().readValue(r, OrderBook::class.java)
        }
    }

    /**
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     */

    /**
     * Initializes the asset balance cache by using the REST API and starts a new user data streaming session.
     *
     * @return a listenKey that can be used with the user data streaming API.
     */
    private suspend fun initializeAssetBalanceCacheAndStreamSession(): String {
        val account= getAccount()
        account.balances.forEach { accountBalanceCache.toMutableMap()[it.asset] = it }
        return startUserDataStream()
    }

    /**
     * Initializes the candlestick cache by using the REST API.
     */
    suspend fun initializeCandlestickCache(symbol: String) {
        val candlestickBars = getCandlestickBars(symbol.toUpperCase(), INTERVAL.intervalId)
        candlesticksCache = TreeMap<Long, Candlestick>()
        candlestickBars.forEach { candlesticksCache.toMutableMap()[it.openTime] = it }
    }
}
