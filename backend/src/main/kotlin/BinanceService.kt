import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.leantrace.converters.UnixTimestampDeserializer
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
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 *
 *
 * Created on 30.04.21
 * @author: Alexander Schamne <alexander.schamne@leantrace.ch>
 *
 */
class BinanceService {
    companion object {

        const val baseUrl = "https://api.binance.com"
        const val wsBaseUrl = "wss://stream.binance.com:9443/stream"
        val dotenv = dotenv()
        val apiKey = dotenv["BINANCE_API_KEY"]
        val apiSecret = dotenv["BINANCE_API_SECRET"]
        val tradables = dotenv["TRADEABLES"].split(",")
        val bridge = dotenv["BRIDGE"]
        val defaultTickers = tradables.map { "$it$bridge" }


        val client = HttpClient {
            install(WebSockets)
        }

        data class BinanceSubscription(
            val method: String = "SUBSCRIBE",
            val params: List<String> = listOf(),
            val id: Int = 1
        )

        // Types of streams


        data class StreamResponse(
            val stream: String,
            val data: String
        )

        data class MiniTicker(
            @JsonAlias("s") val symbol: String,
            @JsonAlias("c") val value: String,
            @JsonAlias("e") val eventType: String,
            @JsonAlias("E")
            @JsonDeserialize(using = UnixTimestampDeserializer::class)
            val eventTime: LocalDateTime,
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
            @JsonAlias("E")
            @JsonDeserialize(using = UnixTimestampDeserializer::class)
            val eventTime: LocalDateTime,
            @JsonAlias("a") val aggregateTradeID: Long,
            @JsonAlias("f") val firstTradeID: Long,
            @JsonAlias("l") val lastTradeID: Long,
            @JsonAlias("T")
            @JsonDeserialize(using = UnixTimestampDeserializer::class)
            val tradeTime: LocalDateTime,
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

        val miniTickers = mutableMapOf<String, MiniTicker>()

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

        suspend fun createOrder(orderRequest: OrderRequest): HttpResponse {
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

        data class CoinInformation(
            val coin: String,
            val free : BigDecimal,
            val depositAllEnable : Boolean,
            val freeze : BigDecimal,
            val ipoable : BigDecimal,
            val ipoing : BigDecimal,
            val isLegalMoney : Boolean,
            val locked : BigDecimal,
            val name : String,
            val networkList : List<NetworkList>,
            val storage : BigDecimal,
            val trading : Boolean,
            val withdrawAllEnable : Boolean,
            val withdrawing : BigDecimal
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

        suspend fun coinsInformation(): List<CoinInformation> {
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

        suspend fun batchOrders(orderRequests: List<OrderRequest>): HttpResponse {
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

        suspend fun performWebSocket() {
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

                                        val orderRequest = OrderRequest(aggTrade.symbol, OrderSide.BUY, OrderType.LIMIT,
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

    }
}
