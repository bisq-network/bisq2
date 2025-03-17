import java.util.*

// Function to read properties from a file - TODO find a way to reuse this code instead of copying when needed
fun readPropertiesFile(filePath: String): Properties {
    val properties = Properties()
    file(filePath).inputStream().use { properties.load(it) }
    return properties
}

plugins {
    id("bisq.java-library")
    id("bisq.java-integration-tests")
    id("bisq.gradle.tor_binary.BisqTorBinaryPlugin")
}

val properties = readPropertiesFile("../../../gradle.properties")
val bisqVersion: String = properties.getProperty("version", "unspecified")

tor {
    val torVersion = properties.getProperty("tor.version", "unspecified")
    version.set(torVersion)
}

sourceSets {
    main {
        resources {
            srcDir(layout.buildDirectory.file("generated/src/main/resources"))
        }
    }
}

dependencies {
    implementation(project(":tor-common"))

    implementation("bisq:security:$bisqVersion")

    implementation("network:network-identity:$bisqVersion")
    implementation("network:socks5-socket-channel:$bisqVersion")

    implementation(libs.bouncycastle)
    implementation(libs.failsafe)
    implementation(libs.tukaani)
    implementation(libs.typesafe.config)

    implementation(libs.chimp.jsocks)
    implementation(libs.chimp.jtorctl)
}