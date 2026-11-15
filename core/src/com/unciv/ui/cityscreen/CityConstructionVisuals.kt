package com.unciv.ui.cityscreen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.utils.Scaling
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.BaseScreen

/** Ресурсы утверждённого каталога: явные игровые имена, штатная иконка при отсутствии замены. */
internal object CityConstructionVisuals {
    private const val root = "UI_bundles/Heroic-Civs-LibGDX-Handoff/heroic-civs-libgdx-handoff/libgdx-ready/"
    private val entities = mapOf(
        "Worker" to "worker", "Settler" to "settler", "Scout" to "scout",
        "Warrior" to "warrior", "Peasant" to "peasant", "Granary" to "granary",
        "Workshop" to "workshop", "Library" to "library"
    )

    /** Текстуры принадлежат Skin; каждый вызов возвращает отдельный Actor. */
    fun portrait(name: String, size: Float): Actor {
        val filename = entities[name] ?: return ImageGetter.getPortraitImage(name, size)
        val path = root + "icons/entities/$filename.png"
        if (!Gdx.files.internal(path).exists()) return ImageGetter.getPortraitImage(name, size)
        val key = "heroic-city-entity:$filename"
        val skin = BaseScreen.skin
        val texture = if (skin.has(key, Texture::class.java)) skin.get(key, Texture::class.java)
        else Texture(Gdx.files.internal(path)).also {
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            skin.add(key, it, Texture::class.java)
        }
        return Image(texture).apply {
            setScaling(Scaling.fit)
            setSize(size, size)
        }
    }
}
