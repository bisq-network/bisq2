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

apply(from = "../gradle/dependency-security-overrides.gradle.kts")
apply(from = "../gradle/build-environment-verification.gradle.kts")
apply(from = "../gradle/dependency-verification.gradle.kts")

tasks.named("clean") {
    dependsOn(subprojects.map { it.tasks.named("clean") })
}

tasks.named("build") {
    dependsOn(subprojects.map {
        it.tasks.named("build")
    })
}

// The network modules depend on the tor modules, so they are published together. The root build cannot reach the tor
// build, which only this build includes.
tasks.named("publishToMavenLocal") {
    dependsOn(gradle.includedBuild("tor").task(":publishToMavenLocal"))
}

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}

val properties = readPropertiesFile("../gradle.properties")
val rootVersion = properties.getProperty("version", "unspecified")
version = rootVersion
