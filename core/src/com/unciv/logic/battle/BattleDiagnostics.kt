package com.unciv.logic.battle

/** Optional battle diagnostics shared by interactive battles and headless simulations. */
class BattleDiagnostics(
    private val isEnabled: () -> Boolean = { ENABLED },
    private val sink: (String) -> Unit = { println(it) }
) {
    /** Disabled logging must not evaluate message builders or inspect additional battle state. */
    fun log(message: () -> String) {
        if (isEnabled()) sink("[Battle] ${message()}")
    }

    companion object {
        /** Single switch for combat diagnostics. Enable for manual debugging. */
        const val ENABLED = false
        val shared = BattleDiagnostics()
    }
}
