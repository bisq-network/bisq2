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