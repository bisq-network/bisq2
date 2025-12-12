plugins {
    java
    id("bisq.gradle.maven_publisher.LocalMavenPublishPlugin")
}

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}

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