package com.unciv.ui.story

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.AutoScrollPane
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.onActivation
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton

/** Подготовка, проигрывание или явная видеозаглушка. Закрытие не завершает событие партии. */
class StoryCinematicScreen(
    private val assetPath: String,
    private val fallbackText: String,
    private val skippable: Boolean,
    private val onCompleted: () -> Unit
) : BaseScreen() {
    private var preparationReason: String? = null
    private val session = StoryVideoSession {
        when {
            assetPath.isBlank() -> {
                preparationReason = "No video file has been assigned to this scene."
                null
            }
            !Gdx.files.internal(assetPath).exists() -> {
                preparationReason = "The video file was not found."
                null
            }
            else -> game.platformSpecificHelper?.createStoryVideoPlayer(assetPath)
        }
    }
    private var videoActor: Actor? = null
    private var preparationDrawn = false
    private var fallbackShown = false
    private var submitted = false
    private var disposed = false
    private val placeholder = Table().apply { setFillParent(true) }
    private val content = Table().apply { defaults().pad(8f).growX() }
    private var scrollCell: com.badlogic.gdx.scenes.scene2d.ui.Cell<AutoScrollPane>? = null

    init {
        stage.addActor(placeholder)
        val scroll = AutoScrollPane(content).apply { setScrollingDisabled(true, false) }
        scrollCell = placeholder.add(scroll).width(contentWidth()).growY().pad(20f)
        showPreparation()
    }

    private fun contentWidth() = (stage.width - 40f).coerceIn(240f, 760f)

    private fun showPreparation() {
        content.add("Preparing video scene…".toLabel(Color.SKY).apply {
            name = "story-video-preparing"
            wrap = true
        }).width(contentWidth() - 32f).row()
        content.add("Checking the video file and playback support".toLabel().apply { wrap = true })
            .width(contentWidth() - 32f).row()
    }

    override fun render(delta: Float) {
        if (disposed) return
        // Сначала действительно рисуем подготовку; искусственной задержки или процентов загрузки нет.
        if (!preparationDrawn) {
            preparationDrawn = true
            super.render(delta)
            return
        }
        if (session.state == StoryVideoSession.State.Ready) {
            session.start()
            session.player?.let { player ->
                placeholder.remove()
                videoActor = player.actor
                stage.addActor(player.actor)
                fitVideo()
                if (skippable) {
                    val controls = Table().apply { setFillParent(true); top().right() }
                    val skip = "Skip".toTextButton()
                    skip.onActivation { session.skip() }
                    controls.add(skip).pad(20f).minWidth(140f).minHeight(48f)
                    stage.addActor(controls)
                }
            }
        }
        session.update(delta)
        when (session.state) {
            StoryVideoSession.State.Completed -> { complete(); return }
            StoryVideoSession.State.Fallback -> if (!fallbackShown) showFallback()
            else -> Unit
        }
        super.render(delta)
    }

    /** Заглушка остаётся до подтверждения, в том числе для сцены без разрешённого пропуска. */
    private fun showFallback() {
        fallbackShown = true
        videoActor = null
        stage.clear()
        content.clear()
        stage.addActor(placeholder)
        val frame = Table().apply {
            name = "story-video-placeholder"
            background = ImageGetter.getDrawable("OtherIcons/whiteDot").tint(Color(0.08f, 0.1f, 0.15f, 1f))
            add("▶".toLabel(Color.SKY).apply { setFontScale(2f) }).pad(20f).row()
            add("Video scene placeholder".toLabel(Color.SKY)).pad(12f).row()
        }
        content.add(frame).height(180f).row()
        val reason = preparationReason ?: when (session.fallbackReason) {
            StoryVideoSession.FallbackReason.Unavailable -> "Video playback is not available on this platform."
            StoryVideoSession.FallbackReason.StartupFailed -> "The video could not be opened."
            StoryVideoSession.FallbackReason.PlaybackFailed -> "Video playback failed."
            StoryVideoSession.FallbackReason.ControlFailed -> "Video playback was interrupted by a player error."
            null -> "Video playback is unavailable."
        }
        content.add(reason.toLabel(Color.GOLD).apply {
            name = "story-video-reason"; wrap = true
        }).width(contentWidth() - 32f).row()
        if (assetPath.isNotBlank()) content.add(assetPath.toLabel(Color.LIGHT_GRAY).apply {
            name = "story-video-path"; wrap = true; setFontScale(0.75f)
        }).width(contentWidth() - 32f).row()
        content.add(fallbackText.toLabel().apply {
            name = "story-text"; wrap = true
        }).width(contentWidth() - 32f).row()
        content.add("Continue".toTextButton().apply {
            name = "story-choice:continue"
            onActivation { complete() }
        }).minHeight(48f).row()
    }

    private fun complete() {
        if (submitted || disposed || game.screen !== this) return
        submitted = true
        try { onCompleted() }
        catch (failure: Exception) { submitted = false; throw failure }
        if (game.screen === this) game.popScreen()
    }

    private fun fitVideo() { videoActor?.setBounds(0f, 0f, stage.width, stage.height) }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        fitVideo()
        scrollCell?.width(contentWidth())
        // Содержимое перестраивается только после fallback; подготовка не запускает плеер повторно.
        if (fallbackShown) showFallback()
        placeholder.invalidateHierarchy()
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
