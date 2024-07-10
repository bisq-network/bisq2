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
        create("CopyWebcamAppVersionPlugin") {
            id = "bisq.gradle.copy_version.CopyWebcamAppVersionPlugin"
            implementationClass = "bisq.gradle.copy_version.CopyWebcamAppVersionPlugin"
        }
    }
}

dependencies {
    implementation(project(":gradle-tasks"))
}