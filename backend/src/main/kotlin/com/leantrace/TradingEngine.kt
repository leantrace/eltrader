package com.leantrace

import com.leantrace.strategies.CryptoMomentumStrategy
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime


@Component
class TradingEngine(val strategy: CryptoMomentumStrategy) {

    companion object {
        private val logger = LoggerFactory.getLogger(javaClass)
    }

    init {
        GlobalScope.launch {
            strategy.init()
        }
    }

    @Scheduled(fixedDelay = 10*1000)
    fun scheduleFixedDelayTask() {
        logger.info("Execute: "+LocalDateTime.now())
        GlobalScope.launch {
            strategy.execute()
        }
    }
}
