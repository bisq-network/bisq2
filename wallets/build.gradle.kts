plugins {
    java
}

tasks.named("clean") {
    dependsOn(subprojects.map { it.tasks.named("clean") })
}

tasks.named("build") {
    dependsOn(subprojects.map { it.tasks.named("build") })
}

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}