package com.unciv.ui.battlescreen

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.WeakHashMap

internal object BattleScreenScopeRegistry {
    private val scopes = WeakHashMap<BattleScreen, CoroutineScope>()

    @Synchronized
    fun scopeFor(screen: BattleScreen): CoroutineScope =
        scopes.getOrPut(screen) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
}
