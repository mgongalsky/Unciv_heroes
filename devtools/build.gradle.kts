plugins {
    id("application")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("com.example.devtools.DesktopLauncherKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.21")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.21")
    implementation("com.badlogicgames.gdx:gdx:1.13.0")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:1.13.0")
    implementation("com.badlogicgames.gdx:gdx-platform:1.13.0:natives-desktop")
    implementation("com.badlogicgames.gdx:gdx-freetype:1.13.0")
    implementation("com.badlogicgames.gdx:gdx-freetype-platform:1.13.0:natives-desktop")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")

}

tasks.named<JavaExec>("run") {
    mainClass.set("com.example.devtools.DesktopLauncherKt")
    classpath = sourceSets["main"].runtimeClasspath
}
