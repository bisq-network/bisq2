plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("BisqDevSetupPlugin") {
            id = "bisq.gradle.dev.setup.BisqDevSetupPlugin"
            implementationClass = "bisq.gradle.dev.setup.BisqDevSetupPlugin"
        }
    }
}