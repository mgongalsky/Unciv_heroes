package com.unciv.clean.adapters.log

import com.unciv.clean.application.ports.LoggerPort
import com.unciv.utils.debug

class LoggerAdapter : LoggerPort {
    override fun debug(message: String, vararg args: Any?) {
        debug(message, *args)
    }
}
