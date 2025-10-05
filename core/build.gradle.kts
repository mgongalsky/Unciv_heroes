
import com.unciv.build.BuildConfig.gdxVersion

plugins {
    id("kotlin")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}


sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        java.exclude("test/**")
    }
    test {
        java.setSrcDirs(listOf("src/test/kotlin"))
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    testImplementation("com.badlogicgames.gdx:gdx-backend-headless:$gdxVersion")
    testRuntimeOnly("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
}

tasks.test {
    useJUnitPlatform()
    // Run tests from the assets directory so any Gdx file lookups work if needed
    workingDir = file("../android/assets")
    systemProperty("headless", "true")
}
