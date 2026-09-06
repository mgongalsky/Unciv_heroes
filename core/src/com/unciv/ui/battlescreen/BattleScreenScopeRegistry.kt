package com.unciv.ui.battlescreen

import com.unciv.utils.concurrency.Dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.util.WeakHashMap

internal object BattleScreenScopeRegistry {
    private val scopes = WeakHashMap<BattleScreen, CoroutineScope>()

    @Synchronized
    fun scopeFor(screen: BattleScreen): CoroutineScope =
        scopes.getOrPut(screen) { CoroutineScope(SupervisorJob() + Dispatcher.GL) }
}
