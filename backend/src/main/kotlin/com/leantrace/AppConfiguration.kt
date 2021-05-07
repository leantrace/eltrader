package com.leantrace

/**
 * Created on 01.05.21
 * @author: Alexander Schamne <alexander.schamne@leantrace.ch>
 *
 */

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.scheduling.annotation.EnableScheduling
import java.math.BigDecimal

@ConstructorBinding
@ConfigurationProperties("app.env")
data class AppConfiguration(
    val binanceRestUrl: String,
    val binanceWssUrl: String,
    val binanceApiKey: String,
    val binanceApiSecret: String,
    val tradeables: String,
    val bridge: String,
    val bridgeAmount: BigDecimal
)
