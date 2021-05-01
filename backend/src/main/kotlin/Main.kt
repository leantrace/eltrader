import com.sun.tools.javac.Main
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
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
    val apiKey = dotenv["BINANCE_API_KEY"]
    val apiSecret = dotenv["BINANCE_API_SECRET"]

    println(apiKey + apiSecret)

    GlobalScope.launch {
        repeat(Int.MAX_VALUE) {
            logger.info {
                BinanceService.miniTickers.map { entry ->
                    "[${entry.key}] ${entry.value.value}"
                }.joinToString()
            }
            delay(1000L)
        }
    }

    GlobalScope.launch {
        //BinanceService.performWebSocket()
    }


    runBlocking {
        val r = BinanceService.coinsInformation()
        r.filter { it.free > BigDecimal.ZERO }.forEach { println(it) }
        delay(100L)
    }
}
