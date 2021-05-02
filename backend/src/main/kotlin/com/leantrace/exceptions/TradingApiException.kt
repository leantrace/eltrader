package com.leantrace.exceptions

class TradingApiException : Exception {
    constructor(msg: String?) : super(msg) {}
    constructor(msg: String?, e: Throwable?) : super(msg, e) {}

    companion object {
        private const val serialVersionUID = -8279304672615688060L
    }
}
