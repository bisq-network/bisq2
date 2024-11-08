plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

allprojects {
    plugins.apply("java")
    // Configure Java toolchain for Java 17
    tasks.withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    // Alternatively, use the toolchain configuration
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
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