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

// TODO Just added to get the commons dependency into the (java) desktopApp project used for the shadowJar task.
// The desktopApp project uses BisqDesktopRegtestPlugin, thus we can resolve the dependency.
// Common has no plugin and I did not manage to add the gradle common package as gradle-build dependency to desktopApp
dependencies {
    implementation(project(":commons"))
}