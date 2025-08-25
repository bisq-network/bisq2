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
        create("I2pRouterAppPlugin") {
            id = "bisq.gradle.i2p_router.I2pRouterAppPlugin"
            implementationClass = "bisq.gradle.i2p_router.I2pRouterAppPlugin"
        }
    }
}

dependencies {
    implementation(project(":commons"))
}
