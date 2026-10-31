package com.marzec.logger

interface Logger {

    fun log(tag: String, message: String)

    fun log(tag: String, message: String, t: Throwable)

    companion object : Logger {

        lateinit var logger: Logger

        override fun log(tag: String, message: String) {
            logger.log(tag, message)
        }

        override fun log(tag: String, message: String, t: Throwable) {
            logger.log(tag, message, t)
        }
    }
}

class MultiLogger(private val loggers: List<Logger>) : Logger {
    override fun log(tag: String, message: String) {
        loggers.forEach { it.log(tag, message) }
    }

    override fun log(tag: String, message: String, t: Throwable) {
        loggers.forEach { it.log(tag, message, t) }
    }
}