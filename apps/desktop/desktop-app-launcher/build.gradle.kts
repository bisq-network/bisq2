plugins {
    id("bisq.java-library")
    id("bisq.gradle.desktop.regtest.BisqDesktopRegtestPlugin")
    application
    id("bisq.gradle.packaging.PackagingPlugin")
    alias(libs.plugins.openjfx)
}

application {
    mainClass.set("bisq.desktop_app_launcher.DesktopAppLauncher")
}

packaging {
    name.set("Bisq2")
    version.set("2.0.4")
}

javafx {
    version = "17.0.1"
    modules = listOf("javafx.controls", "javafx.media")
}

dependencies {
    implementation("bisq:common")
    implementation("bisq:security")
    implementation("bisq:application")

    implementation(project(":desktop-app"))
}

tasks {
    named<Jar>("jar") {
        manifest {
            attributes(
                mapOf(
                    Pair("Implementation-Title", project.name),
                    Pair("Implementation-Version", project.version),
                    Pair("Main-Class", "bisq.desktop_app_launcher.DesktopAppLauncher")
                )
            )
        }
    }

    distZip {
        enabled = false
    }

    distTar {
        enabled = false
    }
}
