plugins {
    id("application")
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral() // Для загрузки FasterXML и JavaFX
    google() // Если нужен Android
}

dependencies {
    // FasterXML (Jackson)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2") // Укажите актуальную версию
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2") // Для поддержки Kotlin

    // JavaFX зависимости
    implementation("org.openjfx:javafx-controls:24-ea+19")
    implementation("org.openjfx:javafx-fxml:24-ea+19")
    implementation("org.openjfx:javafx-graphics:24-ea+19")

    // Kotlin Standard Library
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    // Тестовые зависимости (если нужны)
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

application {
    mainClass.set("com.example.jsoneditor.JsonEditorKt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}


// Аргументы JVM для добавления модулей JavaFX
tasks.withType<JavaExec> {
    jvmArgs = listOf("--add-modules", "javafx.controls,javafx.fxml")
}
