package com.unciv.ui.mapeditor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.unciv.pure.domain.hero.ScenarioHero
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.tilegroups.TileGroup
import com.unciv.ui.utils.BaseScreen

/** Отображает исходное размещение героя без создания MapUnit и изменения клетки карты. */
class ScenarioHeroPreview(tile: TileGroup, hero: ScenarioHero) : Group() {
    init {
        name = "scenario-hero-preview"
        touchable = Touchable.disabled
        isTransform = false
        setSize(tile.width, tile.height)
        // Нация назначается при запуске: здесь используются нейтральные цвета и спрайт типа войска.
        val imagePath = generateSequence(tile.tileSetStrings) { it.fallback }
            .map { it.unitsLocation + hero.unitName }
            .firstOrNull { ImageGetter.imageExists(it) }
        if (imagePath != null) {
            for (image in ImageGetter.getLayeredImageColored(imagePath, null, Color.GOLD, Color.DARK_GRAY)) {
                image.name = "scenario-hero-sprite"
                image.touchable = Touchable.disabled
                addActor(image)
                tile.sizeAndPlaceOverHex(image)
            }
        } else {
            val path = if (ImageGetter.unitIconExists(hero.unitName)) "UnitIcons/${hero.unitName}"
                else "OtherIcons/Shield"
            val image = ImageGetter.getImage(path).apply {
                name = "scenario-hero-sprite"
                touchable = Touchable.disabled
                setSize(tile.width * 0.6f, tile.height * 0.6f)
                setPosition((tile.width - width) / 2f, (tile.height - height) / 2f)
            }
            addActor(image)
        }
        addActor(Label(hero.name.ifBlank { hero.id }, BaseScreen.skin).apply {
            name = "scenario-hero-name"
            touchable = Touchable.disabled
            color = Color.GOLD
            setFontScale(0.65f)
            pack()
            setPosition((tile.width - width) / 2f, 0f)
        })
    }
}
