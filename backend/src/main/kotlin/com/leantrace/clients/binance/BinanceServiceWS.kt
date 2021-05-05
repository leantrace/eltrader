package com.leantrace.clients.binance

import AppObjectMapper
import com.leantrace.AppConfiguration
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service
import java.math.BigDecimal


/**
 *
 *
 * Created on 30.04.21
 * @author: Alexander Schamne <alexander.schamne@gmail.com>
 *
 */
@Service
@EnableConfigurationProperties(AppConfiguration::class)
class BinanceServiceWS (val config: AppConfiguration) : BinanceServiceWSApi  {


    companion object {
        val client: HttpClient = HttpClient {
            install(WebSockets)
        }
    }

    private val logger = KotlinLogging.logger {}


    // Update Speed: 1000ms or 100ms
    suspend fun depth(symbols: List<String>, callback: BinanceApiCallback<DiffDepth>) = onChannel(symbols, "@depth", DiffDepth::class.java, callback)

    suspend fun miniTicker(symbols: List<String>, callback: BinanceApiCallback<MiniTicker>) = onChannel(symbols, "@miniTicker", MiniTicker::class.java, callback)

    suspend fun aggTrade(symbols: List<String>, callback: BinanceApiCallback<AggTrade>) = onChannel(symbols, "@aggTrade", AggTrade::class.java, callback)

    suspend fun userData(userkey: String, callback: BinanceApiCallback<Map<*, *>>) = onChannel(listOf(""), userkey, Map::class.java, callback)


    suspend fun <T> onChannel(symbols: List<String>, channel: String, clazz: Class<T>, callback: BinanceApiCallback<T>) {
        logger.info { "Connecting to $channel with base url ${config.binanceWssUrl}" }
        client.wss(
            urlString = config.binanceWssUrl
        ) {
            logger.info { "Connected to $channel" }
            val binanceSubscription = BinanceSubscription(
                params = symbols.map { "${it.toLowerCase()}${channel}" }
            )
            logger.info { "Subscribe to $binanceSubscription" }
            withContext(Dispatchers.IO) {
                send(AppObjectMapper.mapper().writeValueAsString(binanceSubscription))
            }
            try {
                for (frame in incoming) {
                    val text = (frame as Frame.Text).readText()
                    val binanceElement = withContext(Dispatchers.IO) {
                        AppObjectMapper.mapper().readValue(text, Map::class.java)
                    }
                    val stream = binanceElement["stream"]
                    val data = binanceElement["data"]
                    logger.info { binanceElement }
                    if (stream != null) {
                        logger.info{stream}
                        when (val it = stream.toString().split('@').last()) {
                            channel.replace("@","") -> {
                                logger.info { data }
                                val ret = AppObjectMapper.mapper().convertValue(data, clazz)
                                callback.onResponse(ret)
                            }
                            else -> logger.info { "Could not decode $it" }
                        }

                    }

                }
            } catch (e: ClosedReceiveChannelException) {
                logger.warn(e) { "Error received" }
                logger.warn("onClose ${closeReason.await()}")
                callback.onFailure(e)
            } catch (e: Throwable) {
                logger.warn(e) { "Error received" }
                logger.warn("onError ${closeReason.await()}")
                callback.onFailure(e)
            }

        }
    }

    override suspend fun performWebSocket() {
        val defaultTickers = config.tradeables.split(",").map { "$it${config.bridge}" }
        logger.info { "Connecting..." }
        client.wss(
            urlString = config.binanceWssUrl
        ) {
            logger.info { "Connected!" }

            val binanceSubscription = BinanceSubscription(
                //params = listOf("!miniTicker@arr")
                params = defaultTickers.map { symbol -> "${symbol.toLowerCase()}@aggTrade" }
                 //   .plus(defaultTickers.map { symbol -> "${symbol.toLowerCase()}@miniTicker" })
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
                                //miniTickers[miniTicker.symbol] = miniTicker
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
}
