package com.unciv.ui.utils

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.ui.Skin

/**
 * Владеет зарегистрированными ресурсами, но заимствует регионы общего атласа.
 * Владелец атласа освобождает его отдельно, после завершения работы интерфейса.
 * Освобождать этот Skin можно только после прекращения использования его стилей.
 */
class SharedAtlasSkin(atlas: TextureAtlas) : Skin() {
    private var disposed = false

    init {
        addRegions(atlas)
    }

    override fun dispose() {
        if (disposed) return
        disposed = true
        super.dispose()
    }
}
