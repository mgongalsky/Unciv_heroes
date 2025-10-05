package testsupport

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration

object GdxHeadless {
    private var started = false
    fun ensure() {
        if (started) return
        HeadlessApplication(object : ApplicationAdapter() {}, HeadlessApplicationConfiguration())
        started = true
    }
}