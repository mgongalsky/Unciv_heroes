import com.unciv.build.BuildConfig
import java.util.*
import com.unciv.build.AndroidImagePacker

plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "com.unciv.app"
    compileSdk = 32
    sourceSets {
        getByName("main").apply {
            manifest.srcFile("AndroidManifest.xml")
            java.srcDirs("src")
            aidl.srcDirs("src")
            renderscript.srcDirs("src")
            res.srcDirs("res")
            assets.srcDirs("assets")
            jniLibs.srcDirs("libs")
        }
    }
    packagingOptions {
        resources.excludes += "META-INF/robovm/ios/robovm.xml"
        resources.excludes += "DebugProbesKt.bin"
    }
    defaultConfig {
        applicationId = "com.unciv.app"
        minSdk = 21
        targetSdk = 32
        versionCode = BuildConfig.appCodeNumber
        versionName = BuildConfig.appVersion
        base.archivesName.set("Unciv")
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storePassword = "android"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    lint {
        disable += "MissingTranslation"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }
    androidResources {
        ignoreAssetsPattern = "!SaveFiles:!fonts:!maps:!music:!mods"
    }
    buildToolsVersion = "33.0.1"
}

fun registerImagePackingTask(taskName: String, force: Boolean, taskDescription: String) =
        tasks.register(taskName) {
            group = "graphics"
            description = taskDescription
            doLast {
                AndroidImagePacker.packImages(
                    workingPath = projectDir.path,
                    force = force,
                    atlasName = providers.gradleProperty("atlas").orNull,
                    allowIncompleteSources = providers.gradleProperty("allowIncompleteSources")
                        .orNull
                        ?.toBooleanStrictOrNull()
                        ?: false
                )
            }
        }

val packImages by registerImagePackingTask(
    taskName = "packImages",
    force = false,
    taskDescription = "Pack changed source images into texture atlases. Use -Patlas=Name to select one atlas."
)

registerImagePackingTask(
    taskName = "repackImages",
    force = true,
    taskDescription = "Rebuild texture atlases even when their source images have not changed. Use -Patlas=Name to select one atlas."
)

tasks.register("texturePacker") {
    group = "graphics"
    description = "Compatibility alias for packImages."
    dependsOn(packImages)
}

task("copyAndroidNatives") {
    val natives: Configuration by configurations
    doFirst {
        val rx = Regex(""".*natives-([^.]+)\.jar$""")
        natives.forEach { jar ->
            if (rx.matches(jar.name)) {
                val outputDir = file(rx.replace(jar.name) { "libs/" + it.groups[1]!!.value })
                outputDir.mkdirs()
                copy {
                    from(zipTree(jar))
                    into(outputDir)
                    include("*.so")
                }
            }
        }
    }
    dependsOn(packImages)
}

tasks.whenTaskAdded {
    if (name.startsWith("merge") && name.endsWith("Assets")) {
        dependsOn(packImages)
    }
    if ("package" in name || "assemble" in name || "bundleRelease" in name) {
        dependsOn("copyAndroidNatives")
    }
}

tasks.register<JavaExec>("run") {
    val localProperties = project.file("../local.properties")
    val path = if (localProperties.exists()) {
        val properties = Properties()
        localProperties.inputStream().use { properties.load(it) }
        properties.getProperty("sdk.dir") ?: System.getenv("ANDROID_HOME")
    } else {
        System.getenv("ANDROID_HOME")
    }
    val adb = "$path/platform-tools/adb"
    doFirst {
        project.exec {
            commandLine(adb, "shell", "am", "start", "-n", "com.unciv.app/AndroidLauncher")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.work:work-runtime-ktx:2.7.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")
}
