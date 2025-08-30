plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    alias(libs.plugins.shadow)
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("Bi2pAppPlugin") {
            id = "bisq.gradle.bi2p.Bi2pAppPlugin"
            implementationClass = "bisq.gradle.bi2p.Bi2pAppPlugin"
        }
    }
}

dependencies {
    implementation(project(":commons"))
}
