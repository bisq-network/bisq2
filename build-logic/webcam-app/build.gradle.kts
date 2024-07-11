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
        create("WebcamAppPlugin") {
            id = "bisq.gradle.webcam_app.WebcamAppPlugin"
            implementationClass = "bisq.gradle.webcam_app.WebcamAppPlugin"
        }
    }
}

dependencies {
    implementation(project(":gradle-tasks"))
}