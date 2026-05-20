import org.gradle.api.tasks.Delete
import org.gradle.api.GradleException

import java.io.File
import java.nio.charset.StandardCharsets
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

apply(from = "gradle/dependency-verification.gradle.kts")

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

tasks.register("verifyGithubActionsSecurity") {
    group = "verification"
    description = "Verifies GitHub Actions workflows use pinned actions and non-floating build environments."

    val workflowFiles = fileTree(".github/workflows") {
        include("*.yml")
        include("*.yaml")
    }

    inputs.files(workflowFiles)
    outputs.upToDateWhen { false }

    doLast {
        val violations = mutableListOf<String>()
        val usesRegex = Regex("""^\s*-?\s*uses:\s*([^#\s]+)(?:\s+#\s*(.+))?\s*$""")
        val floatingRunnerRegex = Regex("""\b(ubuntu|macos|windows)-latest\b""", RegexOption.IGNORE_CASE)
        val javaVersionRegex = Regex("""^\s*java-version:\s*([^#]+?)(?:\s+#.*)?$""")
        val javaMatrixRegex = Regex("""^\s*java:\s*\[([^]]+)]\s*(?:#.*)?$""")
        val exactJavaVersionRegex = Regex("""\d+\.\d+\.\d+""")
        val checkLatestTrueRegex = Regex("""^\s*check-latest:\s*['"]?true['"]?\s*$""", RegexOption.IGNORE_CASE)
        val checkLatestFalseRegex = Regex("""^\s*check-latest:\s*['"]?false['"]?\s*$""", RegexOption.IGNORE_CASE)
        val persistCredentialsFalseRegex = Regex("""^\s*persist-credentials:\s*['"]?false['"]?\s*$""", RegexOption.IGNORE_CASE)
        val stepStartRegex = Regex("""^(\s*)-\s+[A-Za-z_][A-Za-z0-9_-]*\s*:.*$""")

        fun cleanYamlScalar(value: String): String {
            return value.trim().trim('\'', '"')
        }

        fun stepLinesAfter(lines: List<String>, startIndex: Int): List<String> {
            val stepStartIndent = (startIndex downTo 0)
                .mapNotNull { stepStartRegex.find(lines[it]) }
                .firstOrNull()
                ?.groupValues
                ?.get(1)
                ?.length

            return lines.drop(startIndex + 1).takeWhile { line ->
                val nextStepStart = stepStartRegex.find(line)
                stepStartIndent == null ||
                    nextStepStart == null ||
                    nextStepStart.groupValues[1].length != stepStartIndent
            }
        }

        workflowFiles.files.sortedBy { it.path }.forEach { workflowFile ->
            val workflowPath = rootProject.relativePath(workflowFile)
            val lines = workflowFile.readLines(StandardCharsets.UTF_8)

            lines.forEachIndexed { index, line ->
                val lineNumber = index + 1
                val usesMatch = usesRegex.find(line)
                if (usesMatch != null) {
                    val actionRef = usesMatch.groupValues[1]
                    val versionComment = usesMatch.groupValues[2].trim()

                    if (!actionRef.startsWith("./")) {
                        val ref = actionRef.substringAfterLast('@', missingDelimiterValue = "")
                        if (!ref.matches(Regex("""[0-9a-fA-F]{40}"""))) {
                            violations += "$workflowPath:$lineNumber uses '$actionRef' without a full 40-character commit SHA"
                        }
                        if (versionComment.isEmpty()) {
                            violations += "$workflowPath:$lineNumber uses '$actionRef' without an inline version comment"
                        }
                    }

                    if (actionRef.startsWith("actions/checkout@")) {
                        val checkoutStepLines = stepLinesAfter(lines, index)
                        if (checkoutStepLines.none { persistCredentialsFalseRegex.matches(it) }) {
                            violations += "$workflowPath:$lineNumber uses actions/checkout without persist-credentials: false"
                        }
                    }

                    if (actionRef.startsWith("actions/setup-java@")) {
                        val setupJavaStepLines = stepLinesAfter(lines, index)
                        if (setupJavaStepLines.none { checkLatestFalseRegex.matches(it) }) {
                            violations += "$workflowPath:$lineNumber uses actions/setup-java without check-latest: false"
                        }
                    }
                }

                if (floatingRunnerRegex.containsMatchIn(line)) {
                    violations += "$workflowPath:$lineNumber uses a floating GitHub runner label: ${line.trim()}"
                }

                val javaVersionMatch = javaVersionRegex.find(line)
                if (javaVersionMatch != null) {
                    val javaVersion = cleanYamlScalar(javaVersionMatch.groupValues[1])
                    if (!javaVersion.startsWith("\${{") && !exactJavaVersionRegex.matches(javaVersion)) {
                        violations += "$workflowPath:$lineNumber uses a floating Java version: ${line.trim()}"
                    }
                }

                val javaMatrixMatch = javaMatrixRegex.find(line)
                if (javaMatrixMatch != null) {
                    javaMatrixMatch.groupValues[1]
                        .split(',')
                        .map { cleanYamlScalar(it) }
                        .filter { it.isNotEmpty() && !it.startsWith("\${{") }
                        .filterNot { exactJavaVersionRegex.matches(it) }
                        .forEach {
                            violations += "$workflowPath:$lineNumber uses a floating Java matrix version: $it"
                        }
                }

                if (checkLatestTrueRegex.matches(line)) {
                    violations += "$workflowPath:$lineNumber sets check-latest: true"
                }
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "GitHub Actions security verification failed:\n" +
                    violations.joinToString(separator = "\n") { "  - $it" }
            )
        }

        logger.lifecycle("Verified ${workflowFiles.files.size} GitHub Actions workflow files.")
    }
}

tasks.named("check") {
    dependsOn("verifyGithubActionsSecurity")
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
