import bisq.gradle.common.getPlatform
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

import java.util.Properties

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
    alias(libs.plugins.openjfx)
    alias(libs.plugins.shadow)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

application {
    mainClass.set("bisq.desktop_app.DesktopApp")
}

val properties = readPropertiesFile("../../../gradle.properties")
val rootVersion = properties.getProperty("version", "unspecified")
version = rootVersion
// println("version is ${version}")

javafx {
    version = "21.0.6"
    modules = listOf("javafx.controls", "javafx.media")
}

dependencies {
    implementation("bisq:persistence")
    implementation("bisq:java-se")
    implementation("bisq:i18n")
    implementation("bisq:security")
    implementation("bisq:identity")
    implementation("bisq:account")
    implementation("bisq:offer")
    implementation("bisq:contract")
    implementation("bisq:trade")
    implementation("bisq:bonded-roles")
    implementation("bisq:settings")
    implementation("bisq:user")
    implementation("bisq:chat")
    implementation("bisq:support")
    implementation("bisq:settings")
    implementation("bisq:presentation")
    implementation("bisq:bisq-easy")
    implementation("bisq:application")
    implementation("bisq:evolution")
    implementation("bisq:os-specific")
    implementation("bisq:http-api")

    implementation(project(":desktop"))

    implementation("network:network")
    implementation("bitcoind:core")
    implementation("wallets:wallet")
    // implementation("wallets:electrum")
    // implementation("wallets:bitcoind")

    implementation(libs.typesafe.config)
    implementation(libs.bundles.rest.api.libs)
}

tasks {
    named<Jar>("jar") {
        manifest {
            // doFirst {
            //     println("project version is ${project.version}");
            // }
            attributes(
                    mapOf(
                            Pair("Implementation-Title", project.name),
                            Pair("Implementation-Version", project.version),
                            Pair("Main-Class", "bisq.desktop_app.DesktopApp")
                    )
            )
        }
    }

    named<ShadowJar>("shadowJar") {
        val platformName = getPlatform().platformName
        archiveClassifier.set("$platformName-all")
    }

    distZip {
        enabled = false
    }

    distTar {
        enabled = false
    }
}
