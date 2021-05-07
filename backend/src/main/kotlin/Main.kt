import com.leantrace.AppConfiguration
import com.leantrace.clients.binance.*
import com.sun.tools.javac.Main
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.logging.Logger

/**
 * leantrace GmbH
 * http://www.leantrace.ch/
 *
 * (c) Copyright leantrace GmbH. All rights reserved.
 *
 * This product is the proprietary and sole property of leantrace GmbH.
 * Use, duplication or dissemination is subject to prior written consent of
 * leantrace GmbH.
 *
 * Created on 30.04.21
 * @author: Alexander Schamne <alexander.schamne@gmail.com>
 *
 */

// https://binance-docs.github.io/apidocs/spot/en/#individual-symbol-ticker-streams

fun main() {
    val logger = Logger.getLogger(Main::class.java.name)
    val dotenv = dotenv()
    val config = AppConfiguration(
        dotenv["BINANCE_REST_URL"],
        dotenv["BINANCE_WSS_URL"],
        dotenv["BINANCE_API_KEY"],
        dotenv["BINANCE_API_SECRET"],
        dotenv["TRADEABLES"],
        dotenv["BRIDGE"])

    /* GlobalScope.launch {
        repeat(Int.MAX_VALUE) {
            logger.info {
                BinanceService().miniTickers.map { entry ->
                    "[${entry.key}] ${entry.value.value}"
                }.joinToString()
            }
            delay(1000L)
        }
    }*/

    GlobalScope.launch {
        //BinanceServiceWS(config).depth(listOf("XRPUSDT")) { response -> logger.info { "HELLO: $response" } }
        //BinanceServiceWS(config).miniTicker(listOf("XRPUSDT"))
        //BinanceServiceWS(config).aggTrade(listOf("XRPUSDT"))
        //BinanceServiceWS(config).performWebSocket()

        /*val listenKey = BinanceServiceRest(config).startUserDataStream()
        BinanceServiceWS(config).userData(listenKey){
            response ->
                if (response["e"]?.toString()?.equals(UserDataEventTypes.executionReport.toString()) == true) {
                    val ret = AppObjectMapper.mapper().convertValue(response, UserDataExecutionUpdate::class.java)
                    logger.info { ret.toString() }
                } else {
                    logger.info { "HELLO: $response" }
                }

        }
         */

        /* BinanceServiceWS(config).candlestick(listOf("XRPUSDT"), CandlestickInterval.ONE_MINUTE){
                response -> logger.info { "HELLO $response" }
        }*/

    }


    runBlocking {
        val s: BinanceServiceRestApi = BinanceServiceRest(config)
        //val r = BinanceService().coinsInformation()
        //r.filter { it.free > BigDecimal.ZERO }.forEach { println(it) }
        //println(s.getAccount())
        println(s.getCandlestickBars("XRPUSDT", CandlestickInterval.ONE_MINUTE.intervalId))
        //println(s.startUserDataStream())
        //println(s.getLatestPrice("XRPUSDT"))
        //println(s.getOrderBook("XRPUSDT"))
        delay(10000000L)
    }
}
