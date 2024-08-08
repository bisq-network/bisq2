plugins {
    java
}

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}

tasks.named("clean") {
    dependsOn(subprojects.map { it.tasks.named("clean") })
}