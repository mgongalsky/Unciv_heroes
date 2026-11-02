package com.unciv.ui.utils

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.LifecycleListener
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import java.util.IdentityHashMap

/**
 * Хранит поколения Skin до завершения приложения: живые экраны могут использовать старые стили.
 * Все операции выполняются в GL-потоке. Общий атлас имеет отдельного владельца.
 * Освобождение неиспользуемых поколений во время работы требует отдельного учёта потребителей.
 */
object ApplicationSkinResources {
    private val skins = IdentityHashMap<Application, MutableList<SharedAtlasSkin>>()

    fun create(atlas: TextureAtlas): SharedAtlasSkin {
        val application = Gdx.app
        val owned = skins.getOrPut(application) {
            application.addLifecycleListener(object : LifecycleListener {
                override fun pause() {}
                override fun resume() {}
                override fun dispose() {
                    dispose(application)
                }
            })
            mutableListOf()
        }
        return SharedAtlasSkin(atlas).also { owned.add(it) }
    }

    /** Вызывать после прекращения работы экранов, пока GL-контекст ещё доступен. */
    fun dispose(application: Application = Gdx.app) {
        val owned = skins.remove(application) ?: return
        var failure: Throwable? = null
        for (skin in owned) {
            try {
                skin.dispose()
            } catch (error: Throwable) {
                val previous = failure
                if (previous == null) failure = error else previous.addSuppressed(error)
            }
        }
        failure?.let { throw it }
    }
}
