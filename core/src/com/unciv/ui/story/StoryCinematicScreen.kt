package com.unciv.ui.story

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.unciv.pure.domain.story.StoryDialogueNode
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.onActivation

/** Отдельный экран ролика. Уход с экрана освобождает плеер, но не изменяет очередь партии. */
class StoryCinematicScreen(
    private val assetPath: String,
    private val fallbackText: String,
    private val skippable: Boolean,
    private val onCompleted: () -> Unit
) : BaseScreen() {
    private val session =
            StoryVideoSession { game.platformSpecificHelper?.createStoryVideoPlayer(assetPath) }
    private var videoActor: Actor? = null
    private var fallbackShown = false
    private var submitted = false
    private var disposed = false

    override fun render(delta: Float) {
        if (disposed) return
        if (session.state == StoryVideoSession.State.Ready) {
            session.start()
            session.player?.let { player ->
                videoActor = player.actor
                stage.addActor(player.actor)
                fitVideo()
                if (skippable) {
                    val controls = Table().apply { setFillParent(true); top().right() }
                    val skip = TextButton("Skip", skin)
                    skip.onActivation { session.skip() }
                    controls.add(skip).pad(20f).minWidth(140f).minHeight(48f)
                    stage.addActor(controls)
                }
            }
        }
        session.update(delta)
        when (session.state) {
            StoryVideoSession.State.Completed -> {
                complete()
                return
            }
            StoryVideoSession.State.Fallback -> showFallback()
            else -> Unit
        }
        super.render(delta)
    }

    /** Fallback требует подтверждения даже для ролика, который нельзя пропустить. */
    private fun showFallback() {
        if (fallbackShown) return
        fallbackShown = true
        videoActor = null
        stage.clear()
        val node = StoryDialogueNode().apply {
            speaker = "Story"
            text = fallbackText
        }
        StoryDialoguePopup(this, node, emptyList()) {
            complete()
            true
        }.open(force = true)
    }

    private fun complete() {
        if (submitted || disposed || game.screen !== this) return
        submitted = true
        try {
            onCompleted()
        } catch (failure: Exception) {
            submitted = false
            throw failure
        }
        if (game.screen === this) game.popScreen()
    }

    private fun fitVideo() {
        videoActor?.setBounds(0f, 0f, stage.width, stage.height)
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        fitVideo()
    }

    override fun pause() { session.pause() }
    override fun hide() { session.pause() }
    override fun resume() { session.resume() }
    override fun show() { session.resume() }

    override fun dispose() {
        if (disposed) return
        disposed = true
        session.dispose()
        super.dispose()
    }
}
