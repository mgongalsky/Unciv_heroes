plugins {
    id("kotlin")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(project(":battle-protocol"))
}

sourceSets {
    main {
        java.srcDir("src/")
    }
}
