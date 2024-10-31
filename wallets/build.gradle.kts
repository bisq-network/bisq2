import java.util.*

// Function to read properties from a file - TODO find a way to reuse this code instead of copying when needed
fun readPropertiesFile(filePath: String): Properties {
    val properties = Properties()
    file(filePath).inputStream().use { properties.load(it) }
    return properties
}

plugins {
    java
    id("bisq.gradle.maven_publisher.LocalMavenPublishPlugin")
}

tasks.named("clean") {
    dependsOn(subprojects.map { it.tasks.named("clean") })
}

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}

val properties = readPropertiesFile("../gradle.properties")
val rootVersion: String = properties.getProperty("version", "unspecified")
version = rootVersion