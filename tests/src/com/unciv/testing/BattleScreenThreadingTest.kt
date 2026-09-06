package com.unciv.testing

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.glutils.HdpiMode
import com.unciv.UncivGame
import com.unciv.UncivGameParameters
import com.unciv.app.desktop.NativeFontDesktop
import com.unciv.di.gameModule
import com.unciv.logic.UncivFiles
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.ui.battlescreen.BattleScreen
import com.unciv.ui.tilegroups.TileGroup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.Fonts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Exercises the live battle loop, rather than only drawing a static battle preview. */
class BattleScreenThreadingTest {
    @Test
    fun turnsRunOnRenderThreadAndStopWhenScreenIsDisposed() {
        val failure = AtomicReference<Throwable?>()
        var passed = false
        stopKoin()
        startKoin {
            allowOverride(true)
            modules(gameModule)
        }
        try {
            val config = Lwjgl3ApplicationConfiguration().apply {
                setTitle("Battle threading regression")
                setHdpiMode(HdpiMode.Logical)
                setWindowedMode(1280, 800)
                setInitialVisible(false)
                setForegroundFPS(60)
                setIdleFPS(60)
                disableAudio(true)
            }
            Lwjgl3Application(object : ApplicationAdapter() {
                private lateinit var game: UncivGame
                private var battle: BattleScreen? = null
                private var scope: CoroutineScope? = null
                private var manager: BattleManager? = null
                private val probeComplete = AtomicBoolean(false)
                private var frames = 0
                private var submitted = 0
                private var closedAt: Int? = null
                private val started = System.nanoTime()

                private fun field(screen: BattleScreen, name: String): Any? =
                    BattleScreen::class.java.getDeclaredField(name).apply { isAccessible = true }
                        .get(screen)

                override fun create() {
                    try {
                        Gdx.graphics.isContinuousRendering = true
                        val settings = UncivFiles.getSettingsForPlatformLaunchers()
                        game = UncivGame(
                            UncivGameParameters(
                            fontImplementation = NativeFontDesktop(
                                Fonts.ORIGINAL_FONT_SIZE.toInt(), settings.fontFamily
                            ),
                            initialScreenFactory = {
                                val attacker = ArmyInfo(CivilizationInfo(), maxSlots = 5).apply {
                                    check(addUnits("Peasant", 10000))
                                }
                                val defender = ArmyInfo(CivilizationInfo(), maxSlots = 5).apply {
                                    check(addUnits("Peasant", 10000))
                                }
                                BattleScreen.forTesting(
                                    attacker,
                                    defender,
                                    luckProbability = 0.0,
                                    moraleProbability = 0.0
                                ).also { battle = it }
                            }
                        ))
                        game.create()
                    } catch (error: Throwable) {
                        failure.compareAndSet(null, error)
                        Gdx.app.exit()
                    }
                }

                override fun render() {
                    try {
                        failure.get()?.let { throw it }
                        check(System.nanoTime() - started < 30_000_000_000L) {
                            "Battle did not complete the threading regression within 30 seconds"
                        }
                        Gdx.graphics.isContinuousRendering = true
                        game.render()
                        frames++
                        val screen = battle ?: return
                        val closed = closedAt
                        if (closed != null) {
                            check(scope!!.coroutineContext[Job]?.isActive == false) {
                                "Battle scope is still active after screen disposal"
                            }
                            if (frames - closed >= 8) {
                                passed = true
                                Gdx.app.exit()
                            }
                            return
                        }
                        check(game.screen === screen) { "Battle unexpectedly switched screens (possibly a crash)" }
                        if (scope == null) {
                            val getter =
                                BattleScreen::class.java.getDeclaredMethod("getBattleScope")
                                    .apply { isAccessible = true }
                            val battleScope = getter.invoke(screen) as CoroutineScope
                            scope = battleScope
                            manager = field(screen, "manager") as BattleManager
                            val renderThread = Thread.currentThread()
                            battleScope.launch {
                                try {
                                    check(Thread.currentThread() === renderThread) {
                                        "Battle coroutine started outside the render thread"
                                    }
                                    yield()
                                    check(Thread.currentThread() === renderThread) {
                                        "Battle coroutine resumed outside the render thread"
                                    }
                                    probeComplete.set(true)
                                } catch (error: Throwable) {
                                    failure.compareAndSet(null, error)
                                }
                            }
                        }
                        if (!probeComplete.get() || frames % 3 != 0) return
                        @Suppress("UNCHECKED_CAST")
                        val handler = field(screen, "onPlayerActionReceived") as?
                                ((Pair<BattleCommand, TileGroup>) -> Unit) ?: return
                        if (submitted >= 8) {
                            // Close while the loop is suspended waiting for player input.
                            game.replaceCurrentScreen(EmptyScreen())
                            closedAt = frames
                            return
                        }
                        val troop = manager!!.getCurrentTroop() ?: error("No active troop")
                        val tile = manager!!.getTroopTile(troop) ?: error("No active tile")
                        val group = screen.daTileGroups.first { it.tileInfo == tile }
                        val action =
                            Pair<BattleCommand, TileGroup>(BattleCommand.Skip(troop.id), group)
                        handler(action)
                        check(field(screen, "onPlayerActionReceived") == null) {
                            "Input slot was not consumed synchronously"
                        }
                        // A cached callback must not resume the same continuation twice.
                        handler(action)
                        submitted++
                    } catch (error: Throwable) {
                        failure.compareAndSet(null, error)
                        Gdx.app.exit()
                    }
                }

                override fun dispose() {
                    if (::game.isInitialized) game.dispose()
                }
            }, config)
        } finally {
            stopKoin()
        }
        failure.get()?.let { throw AssertionError("Battle threading regression failed", it) }
        assertTrue("Battle must process eight player turns and stop cleanly", passed)
    }

    private class EmptyScreen : BaseScreen()
}
