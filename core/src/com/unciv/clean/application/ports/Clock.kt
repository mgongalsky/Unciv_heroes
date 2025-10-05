package com.unciv.clean.application.ports

interface Clock {
    fun nowMillis(): Long
}
