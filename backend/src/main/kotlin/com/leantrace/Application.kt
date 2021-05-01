package com.leantrace
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
 * @author: Alexander Schamne <alexander.schamne@leantrace.ch>
 *
 */

import com.sun.tools.javac.Main
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.util.logging.Logger

@SpringBootApplication
class Application

fun main(args: Array<String>) {

    val logger = Logger.getLogger(Main::class.java.name)
    val dotenv = dotenv()
    val apiKey = dotenv["BINANCE_API_KEY"]
    val apiSecret = dotenv["BINANCE_API_SECRET"]

    println(apiKey + apiSecret)

    GlobalScope.launch {
        repeat(Int.MAX_VALUE) {
            logger.info { BinanceService.miniTickers.map {
                    entry -> "[${entry.key}] ${entry.value.value}"
            }.joinToString() }
            delay(1000L)
        }
    }

    GlobalScope.launch {
        //performWebSocket()
        BinanceService.performWebSocket()
    }
    runApplication<Application>(*args)
}
