import com.unciv.build.BuildConfig

plugins {
    id("kotlin")
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(project(":core"))
    implementation(project(":battle-protocol"))
    implementation("com.badlogicgames.gdx:gdx:${BuildConfig.gdxVersion}")
    testImplementation("junit:junit:4.13.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
}

application {
    mainClass.set("com.unciv.battle.server.BattleServerMain")
    applicationDefaultJvmArgs = listOf("-Xmx256m", "-Djava.awt.headless=true")
}

distributions {
    main {
        contents {
            from(rootProject.file("android/assets/jsons")) {
                into("assets/jsons")
            }
            from("deploy") {
                into("deploy")
            }
            from(rootProject.file("docs/Developers/Battle-Server-Ubuntu.md")) {
                into("docs")
            }
        }
    }
}

tasks.named<JavaExec>("run") {
    args(rootProject.file("android/assets").absolutePath)
}

tasks.test {
    useJUnit()
    systemProperty("battle.assets", rootProject.file("android/assets").absolutePath)
}
