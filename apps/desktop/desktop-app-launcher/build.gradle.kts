import java.util.Properties
import java.io.File

// Function to read properties from a file - TODO find a way to reuse this code instead of copying when needed
fun readPropertiesFile(filePath: String): Properties {
    val properties = Properties()
    file(filePath).inputStream().use { properties.load(it) }
    return properties
}

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
    val properties = readPropertiesFile("../../../gradle.properties")
    val rootVersion = properties.getProperty("version", "unspecified")
    version.set(rootVersion.toString())
    // println("version is ${version.get()}")
}

javafx {
    version = "22.0.1"
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
