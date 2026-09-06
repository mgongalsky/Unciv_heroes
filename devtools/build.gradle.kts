plugins {
    id("application")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.example.devtools.UnitEditor")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.badlogicgames.gdx:gdx:1.13.0")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:1.13.0")
    implementation("com.badlogicgames.gdx:gdx-platform:1.13.0:natives-desktop")
    implementation("com.badlogicgames.gdx:gdx-freetype:1.13.0")
    implementation("com.badlogicgames.gdx:gdx-freetype-platform:1.13.0:natives-desktop")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    testImplementation("junit:junit:4.13.2")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}

tasks.register<JavaExec>("runUnitEditor") {
    group = "application"
    description = "Launch the unit editor."
    mainClass.set(application.mainClass)
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = rootProject.projectDir
}

tasks.test {
    useJUnit()
}
