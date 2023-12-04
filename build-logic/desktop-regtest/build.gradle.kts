plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("BisqDesktopRegtestPlugin") {
            id = "bisq.gradle.desktop.regtest.BisqDesktopRegtestPlugin"
            implementationClass = "bisq.gradle.desktop.regtest.BisqDesktopRegtestPlugin"
        }
    }
}