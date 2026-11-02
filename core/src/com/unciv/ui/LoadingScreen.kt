package com.unciv.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.unciv.Constants
import com.unciv.ui.images.ImageWithCustomSize
import com.unciv.ui.popup.Popup
import com.unciv.ui.popup.popups
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.RoyalPanelDrawable
import com.unciv.ui.utils.extensions.toLabel

/** A loading screen that creates a screenshot of the current screen and adds a "Loading..." popup on top of that */
class LoadingScreen(
    previousScreen: BaseScreen? = null
) : BaseScreen() {
    val screenshot: Texture
    init {
        screenshot = takeScreenshot(previousScreen)
        val image = ImageWithCustomSize(
            TextureRegion(
                screenshot,
                0,
                screenshot.height,
                screenshot.width,
                -screenshot.height
            )
        )
        image.width = stage.width
        image.height = stage.height
        stage.addActor(image)
        stage.addAction(Actions.sequence(
            Actions.delay(1000f),
            Actions.run {
                createLoadingPopup().open()
            }
        ))
    }

    private fun createLoadingPopup(): Popup = Popup(stage).apply {
        if (game.settings.skin == "Minimal") {
            innerTable.background = RoyalPanelDrawable(BaseScreen.skin)
            innerTable.pad(52f).padTop(58f)
            val scroll = children.filterIsInstance<ScrollPane>().single()
            val style = scroll.style
            val chrome = innerTable.padLeft + innerTable.padRight + 10f +
                maxOf(style.vScroll?.minWidth ?: 0f, style.vScrollKnob?.minWidth ?: 0f) +
                (style.background?.leftWidth ?: 0f) + (style.background?.rightWidth ?: 0f)
            val contentWidth = minOf(400f, stageToShowOn.width - 24f - chrome).coerceAtLeast(1f)
            getCell(scroll).width(contentWidth + chrome)
                .maxHeight((stageToShowOn.height - 24f).coerceAtLeast(1f))
            addGoodSizedLabel(Constants.loading).apply {
                actor.color = Color.valueOf("453925")
                width(contentWidth)
            }
        } else {
            add(Constants.loading.toLabel())
        }
    }

    private fun takeScreenshot(previousScreen: BaseScreen?): Texture {
        if (previousScreen != null) {
            for (popup in previousScreen.popups) popup.isVisible = false
            previousScreen.render(Gdx.graphics.getDeltaTime())
        }
        val pixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight)
        val screenshot = Texture(pixmap)
        pixmap.dispose()

        if (previousScreen != null) {
            for (popup in previousScreen.popups) popup.isVisible = true
        }
        return screenshot
    }

    override fun dispose() {
        screenshot.dispose()
        super.dispose()
    }
}
