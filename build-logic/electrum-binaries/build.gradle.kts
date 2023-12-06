plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("BisqElectrumPlugin") {
            id = "bisq.gradle.electrum.BisqElectrumPlugin"
            implementationClass = "bisq.gradle.electrum.BisqElectrumPlugin"
        }
    }
}

dependencies {
    implementation(project(":gradle-tasks"))
}