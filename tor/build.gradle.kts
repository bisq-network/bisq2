import java.util.*

plugins {
    java
    id("bisq.gradle.maven_publisher.LocalMavenPublishPlugin")
}

apply(from = "../gradle/dependency-security-overrides.gradle.kts")
apply(from = "../gradle/build-environment-verification.gradle.kts")
apply(from = "../gradle/dependency-verification.gradle.kts")

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}

val properties = Properties().apply {
    file("../gradle.properties").inputStream().use { load(it) }
}
version = properties.getProperty("version", "unspecified")

tasks.named("clean") {
    dependsOn(subprojects.map {
        it.tasks.named("clean")
    })
}
tasks.named("build") {
    dependsOn(subprojects.map {
        it.tasks.named("build")
    })
}
