import com.unciv.build.BuildConfig
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("java")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    testImplementation(project(":desktop"))
}

tasks {
    test {
        workingDir = file("../android/assets")
        systemProperty("golden.projectRoot", rootDir.absolutePath)
        testLogging.lifecycle {
            events(
                TestLogEvent.FAILED,
                TestLogEvent.STANDARD_ERROR,
                TestLogEvent.STANDARD_OUT
            )
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    compileJava {
        options.encoding = "UTF-8"
    }
    compileTestJava {
        options.encoding = "UTF-8"
    }
}

sourceSets {
    test {
        java.srcDir("src")
    }
}

tasks.register<JavaExec>("runBattleBalance") {
    group = "application"
    description = "Launch the battle balance GUI with an isolated headless worker."
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.unciv.testing.simulations.BattleBalanceWindow")
    workingDir = rootProject.projectDir
}

tasks.register<JavaExec>("runBattleBalanceCli") {
    group = "application"
    description =
        "Run a headless balance experiment; supply source, battles, max amount, seed and report directory via --args."
    dependsOn(tasks.named("testClasses"))
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.unciv.testing.simulations.BattleBalanceCli")
    workingDir = rootProject.projectDir
}

eclipse.project {
    name = "${BuildConfig.appName}-tests"
}
