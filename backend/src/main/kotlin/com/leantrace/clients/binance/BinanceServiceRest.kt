package com.leantrace.clients.binance

import AppObjectMapper
import com.fasterxml.jackson.core.type.TypeReference
import com.leantrace.AppConfiguration
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.apache.commons.codec.binary.Hex
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Service
import java.time.Instant
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
@EnableConfigurationProperties(AppConfiguration::class)
class BinanceServiceRest(val config: AppConfiguration) : BinanceServiceRestApi {

    companion object {
        val client: HttpClient = HttpClient {}
    }

    private val logger = KotlinLogging.logger {}

    private fun signRequest(data: String) = createSignature(data, config.binanceApiSecret)

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

    override suspend fun createOrder(orderRequest: OrderRequest): Order {
        val r = client.post<String>("${config.binanceRestUrl}/api/v3/order") {
            header("X-MBX-APIKEY", config.binanceApiKey)
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
        /*val r = """
            {
              "symbol": "BTCUSDT",
              "orderId": 28,
              "orderListId": -1,
              "clientOrderId": "6gCrw2kRUAF9CvJDGP16IP",
              "transactTime": 1507725176595,
              "price": "0.00000000",
              "origQty": "10.00000000",
              "executedQty": "10.00000000",
              "cummulativeQuoteQty": "10.00000000",
              "status": "FILLED",
              "timeInForce": "GTC",
              "type": "MARKET",
              "side": "SELL"
            }
            """
        */
        return withContext(Dispatchers.IO) {
            AppObjectMapper.mapper().readValue(r, Order::class.java)
        }
    }

    suspend fun openOrders(symbol: String, recvWindow: Long? = null): List<Order> {
        val r = client.get<String>("${config.binanceRestUrl}/api/v3/openOrders") {
            header("X-MBX-APIKEY", config.binanceApiKey)
            url {
                signedParameters {
                    parameter("symbol", symbol)
                    recvWindow?.let { parameter("recvWindow", recvWindow) }
                    parameter("timestamp", Instant.now().toEpochMilli())
                }
            }
        }
        return withContext(Dispatchers.IO) {
            AppObjectMapper.mapper().readValue(r, object : TypeReference<List<Order>>() {})
        }
    }

    override suspend fun coinsInformation(): List<CoinInformation> {
        val r = client.get<String>("${config.binanceRestUrl}/sapi/v1/capital/config/getall") {
            header("X-MBX-APIKEY", config.binanceApiKey)
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

        return client.post("${config.binanceRestUrl}/batchOrders") {
            header("X-MBX-APIKEY", config.binanceApiKey)
            url {
                signedParameters {
                    parameter("batchOrders", batchOrdersParamValue)
                    parameter("recvWindow", 5000)
                    parameter("timestamp", Instant.now().toEpochMilli())
                }
            }
        }

    }

    override suspend fun getAccount(): Account {
        val r = client.get<String>("${config.binanceRestUrl}/api/v3/account") {
            header("X-MBX-APIKEY", config.binanceApiKey)
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



    suspend fun getLatestPrice(): TickerPrice {
        val r = client.get<String>("${config.binanceRestUrl}/api/v3/ticker/price") {
            header("X-MBX-APIKEY", config.binanceApiKey)
            url {
                signedParameters {
                    //parameter("recvWindow", 1L)
                    parameter("timestamp", Instant.now().toEpochMilli())
                }
            }
        }
        return withContext(Dispatchers.IO) {
            AppObjectMapper.mapper().readValue(r, TickerPrice::class.java)
        }
    }

    override suspend fun userDataStream(): String {
        val r = client.post<String>("${config.binanceRestUrl}/api/v3/userDataStream") {
            header("X-MBX-APIKEY", config.binanceApiKey)
        }
        return AppObjectMapper.mapper().readValue(r, Map::class.java)["listenKey"]!!.toString()
    }

    override suspend fun getLatestPrice(symbol: String): Price {
        val r = client.get<String>("${config.binanceRestUrl}/api/v3/ticker/price") {
            header("X-MBX-APIKEY", config.binanceApiKey)
            url {
                parameter("symbol", symbol)
            }
        }
        return AppObjectMapper.mapper().readValue(r, Price::class.java)
    }

    override suspend fun getCandlestickBars(symbol: String, interval: String, limit: Int?,
                                            startTime: Long?, endTime: Long?
    ): List<CandlestickArr> {
        val r = client.get<String>("${config.binanceRestUrl}/api/v1/klines") {
            header("X-MBX-APIKEY", config.binanceApiKey)
            url {
                parameter("symbol", symbol)
                parameter("interval", interval)
                limit?.let { parameter("limit", limit) }
                startTime?.let { parameter("startTime", startTime) }
                endTime?.let { parameter("endTime", endTime) }
            }
        }
        logger.debug { r }
        return withContext(Dispatchers.IO) {
            AppObjectMapper.mapper().readValue(r, object : TypeReference<List<CandlestickArr>>() {})
        }
    }



    override suspend fun getOrderBook(symbol: String, limit: OrderBookLimit?): OrderBook {
        val r = client.get<String>("${config.binanceRestUrl}/api/v3/depth") {
            header("X-MBX-APIKEY", config.binanceApiKey)
            url {
                parameter("symbol", symbol)
                limit?.let { parameter("limit", limit.limit) }
            }
        }
        return withContext(Dispatchers.IO) {
            AppObjectMapper.mapper().readValue(r, OrderBook::class.java)
        }
    }

}
