import com.unciv.build.BuildConfig
import com.unciv.build.BuildConfig.gdxVersion

plugins {
    id("kotlin")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

sourceSets {
    main {
        java.srcDir("src/")
    }
}

dependencies {
    api("com.badlogicgames.gdx:gdx-lwjgl3-glfw-awt-macos:$gdxVersion")
}

val mainClassName = "com.unciv.app.desktop.DesktopLauncher"
val assetsDir = file("../android/assets")
val discordDir = file("discord_rpc")
val deployFolder = file("../deploy")

fun JavaExec.configureUiGoldenRun(update: Boolean) {
    dependsOn(tasks.getByName("classes"))
    mainClass.set("com.unciv.app.desktop.UiGoldenTestRunner")
    classpath = sourceSets.main.get().runtimeClasspath
    workingDir = assetsDir
    systemProperty("golden.projectRoot", rootDir.absolutePath)
    if (update) args("--update")
}

tasks.register<JavaExec>("goldenTest") {
    group = "verification"
    description = "Render UI scenarios and compare them pixel-for-pixel with golden PNG files."
    configureUiGoldenRun(update = false)
}

tasks.register<JavaExec>("goldenUpdate") {
    group = "verification"
    description = "Render UI scenarios and replace the platform-specific golden PNG files."
    configureUiGoldenRun(update = true)
}

tasks.register<JavaExec>("run") {
    dependsOn(tasks.getByName("classes"))
    mainClass.set(mainClassName)
    classpath = sourceSets.main.get().runtimeClasspath
    standardInput = System.`in`
    workingDir = assetsDir
    isIgnoreExitValue = true
}

tasks.register<JavaExec>("debug") {
    dependsOn(tasks.getByName("classes"))
    mainClass.set(mainClassName)
    classpath = sourceSets.main.get().runtimeClasspath
    standardInput = System.`in`
    workingDir = assetsDir
    isIgnoreExitValue = true
    debug = true
}

tasks.register<Jar>("dist") {
    dependsOn(tasks.getByName("classes"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(files(sourceSets.main.get().output.resourcesDir))
    from(files(sourceSets.main.get().output.classesDirs))
    from({
        (
                configurations.runtimeClasspath.get().resolve() +
                        configurations.compileClasspath.get().resolve()
                ).map { if (it.isDirectory) it else zipTree(it) }
    })
    from(files(assetsDir))
    from(files(discordDir))
    archiveFileName.set("${BuildConfig.appName}.jar")
    manifest {
        attributes(
            mapOf(
                "Main-Class" to mainClassName,
                "Specification-Version" to BuildConfig.appVersion
            )
        )
    }
}

enum class Platform(val desc: String) {
    Windows32("windows32"), Windows64("windows64"), Linux32("linux32"), Linux64("linux64"), MacOS("mac")
}

class PackrConfig(
    var platform: Platform? = null,
    var jdk: String? = null,
    var executable: String? = null,
    var classpath: List<String>? = null,
    var removePlatformLibs: List<String>? = null,
    var mainClass: String? = null,
    var vmArgs: List<String>? = null,
    var minimizeJre: String? = null,
    var cacheJre: File? = null,
    var resources: List<File>? = null,
    var outDir: File? = null,
    var platformLibsOutDir: File? = null,
    var iconResource: File? = null,
    var bundleIdentifier: String? = null
)

for (platform in Platform.values()) {
    val platformName = platform.toString()
    tasks.create("packr${platformName}") {
        dependsOn(tasks.getByName("dist"))
        val jarFile = "$rootDir/desktop/build/libs/${BuildConfig.appName}.jar"
        val config = PackrConfig().apply {
            this.platform = platform
            executable = "Unciv"
            classpath = listOf(jarFile)
            removePlatformLibs = classpath
            mainClass = mainClassName
            vmArgs = listOf("Xmx1G")
            minimizeJre = "desktop/packrConfig.json"
            outDir = file("packr")
        }
        doLast {
            fun String.runCommand(workingDir: File) {
                val process = ProcessBuilder(*split(" ").toTypedArray())
                    .directory(workingDir)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()
                if (!process.waitFor(30, TimeUnit.SECONDS)) {
                    process.destroy()
                    throw RuntimeException("execution timed out: $this")
                }
                if (process.exitValue() != 0)
                    throw RuntimeException("execution failed with code ${process.exitValue()}: $this")
                println(process.inputStream.bufferedReader().readText())
            }
            if (config.outDir!!.exists()) delete(config.outDir)
            val jdkFile = when (platform) {
                Platform.Linux64 -> "jre-linux-64.tar.gz"
                Platform.Windows64 -> "jdk-windows-64.zip"
                else -> "jre-macOS.tar.gz"
            }
            val platformNameForPackrCmd =
                if (platform == Platform.MacOS) "mac" else platform.name.toLowerCase()
            val command = "java -jar $rootDir/packr-all-4.0.0.jar" +
                    " --platform $platformNameForPackrCmd" +
                    " --jdk $jdkFile" +
                    " --executable Unciv" +
                    " --classpath $jarFile" +
                    " --mainclass $mainClassName" +
                    " --vmargs Xmx1G " +
                    " --output ${config.outDir}"
            command.runCommand(rootDir)
        }
        tasks.register<Zip>("zip${platformName}") {
            archiveFileName.set("${BuildConfig.appName}-${platformName}.zip")
            from(config.outDir)
            destinationDirectory.set(deployFolder)
        }
        finalizedBy("zip${platformName}")
    }
}

tasks.register<Zip>("zipLinuxFilesForJar") {
    archiveFileName.set("linuxFilesForJar.zip")
    from(file("linuxFilesForJar"))
    destinationDirectory.set(deployFolder)
}
