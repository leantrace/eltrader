package com.leantrace.exceptions

class StrategyException : Exception {
    constructor(msg: String?) : super(msg) {}
    constructor(e: Throwable?) : super(e) {}
    constructor(msg: String?, e: Throwable?) : super(msg, e) {}

    companion object {
        private const val serialVersionUID = 1L
    }
}
