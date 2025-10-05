package com.unciv.clean.application.ports

interface LoggerPort {
    fun debug(message: String, vararg args: Any?)
}
