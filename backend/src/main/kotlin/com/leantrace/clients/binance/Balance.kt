package com.leantrace.clients.binance

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty

data class Balance(
    @JsonAlias("a", "asset") val asset: String,
    @JsonAlias("f", "free") val free: String,
    @JsonAlias("l", "locked") val locked: String
)
