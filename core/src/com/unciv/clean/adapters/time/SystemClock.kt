package com.unciv.clean.adapters.time

import com.unciv.clean.application.ports.Clock

class SystemClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
