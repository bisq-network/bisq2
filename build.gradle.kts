import org.gradle.api.tasks.Delete

import java.io.File
import java.util.Locale.getDefault

plugins {
    java
    id("bisq.gradle.maven_publisher.LocalMavenPublishPlugin")
}

data class CompositeBuild(
    val rootDir: File,
    val taskPath: String
)

val compositeBuilds = listOf(
    CompositeBuild(file("build-logic"), ":build-logic"),
    CompositeBuild(file("network"), ":network"),
    CompositeBuild(file("network/tor"), ":network:tor"),
    CompositeBuild(file("apps"), ":apps"),
    CompositeBuild(file("apps/desktop"), ":apps:desktop"),
)

fun getGradleCommand(): String {
    return if (System.getProperty("os.name").lowercase(getDefault()).contains("win")) "gradlew.bat" else "./gradlew"
}

fun readIncludedProjectPaths(settingsDir: File): List<String> {
    val settingsFile = settingsDir.resolve("settings.gradle.kts")
    if (!settingsFile.isFile) {
        return emptyList()
    }

    val includeRegex = Regex("""^\s*include\s*\(([^)]*)\)""")
    val projectPathRegex = Regex(""""([^"]+)"""")

    return settingsFile.readLines().flatMap { line ->
        val match = includeRegex.find(line) ?: return@flatMap emptyList<String>()
        projectPathRegex.findAll(match.groupValues[1])
            .map { it.groupValues[1] }
            .toList()
    }
}

fun File.resolveProjectPath(projectPath: String): File {
    return resolve(projectPath.removePrefix(":").replace(':', File.separatorChar))
}

fun buildDirectories(settingsDir: File): List<File> {
    val projectDirs = listOf(settingsDir) + readIncludedProjectPaths(settingsDir).map {
        settingsDir.resolveProjectPath(it)
    }

    return projectDirs.map { it.resolve("build") }
}

fun allBuildDirectories(): List<File> {
    return buildDirectories(rootDir) + compositeBuilds.flatMap { buildDirectories(it.rootDir) }
}

tasks.register("buildAll") {
    group = "build"
    description = "Build the entire project leaving it ready to work with."

    doLast {
        (listOf(
            ":build-logic:build",
            "build",
        ) + compositeBuilds
            .filter { it.taskPath != ":build-logic" }
            .map { "${it.taskPath}:build" }).forEach {
            exec {
                println("Executing Build: $it")
                commandLine(getGradleCommand(), it)
            }
        }
    }
}

tasks.register<Delete>("cleanAll") {
    group = "build"
    description = "Cleans the entire project."
    delete(allBuildDirectories())
}

tasks.register("publishAll") {
    group = "publishing"
    description = "Publish all the jars in the following modules to local maven repository"

    doLast {
        listOf(
            ":account:publishToMavenLocal",
            ":burningman:publishToMavenLocal",
            ":application:publishToMavenLocal",
            ":bonded-roles:publishToMavenLocal",
            ":chat:publishToMavenLocal",
            ":common:publishToMavenLocal",
            ":contract:publishToMavenLocal",
            ":i18n:publishToMavenLocal",
            ":identity:publishToMavenLocal",
            ":network:publishToMavenLocal",
            ":network:tor:publishToMavenLocal",
            ":notifications:publishToMavenLocal",
            ":offer:publishToMavenLocal",
            ":persistence:publishToMavenLocal",
            ":platform:publishToMavenLocal",
            ":presentation:publishToMavenLocal",
            ":security:publishToMavenLocal",
            ":settings:publishToMavenLocal",
            ":support:publishToMavenLocal",
            ":trade:publishToMavenLocal",
            ":user:publishToMavenLocal",
            ":bisq-easy:publishToMavenLocal",
        ).forEach {
            exec {
                println("Executing Publish To Maven Local: $it")
                commandLine(getGradleCommand(), it)
            }
        }
    }
}

// for jitpack publishing
tasks.named("publishToMavenLocal").configure {
    dependsOn("publishAll")
}

extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}
