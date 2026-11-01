plugins {
    id("kotlin")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(project(":battle-protocol"))
    // Версия движка задаётся проектом; видеобиблиотека не обновляет её транзитивно.
    implementation("com.badlogicgames.gdx-video:gdx-video:1.3.3") {
        exclude(group = "com.badlogicgames.gdx")
    }
}

sourceSets {
    main {
        java.srcDir("src/")
    }
}
