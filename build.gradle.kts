import org.gradle.api.GradleException
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Delete

import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Properties
import java.util.TreeMap
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

val gradleWrapperChecksums = layout.projectDirectory.file("gradle/wrapper/gradle-wrapper.sha256")
val expectedReleaseJavaVersion = providers.gradleProperty("releaseBuild.javaVersion")
val expectedReleaseJavaVendor = providers.gradleProperty("releaseBuild.javaVendor")
val expectedReleaseGradleVersion = providers.gradleProperty("releaseBuild.gradleVersion")

fun getGradleCommand(): String {
    return if (System.getProperty("os.name").lowercase(getDefault()).contains("win")) "gradlew.bat" else "./gradlew"
}

fun requiredGradleProperty(propertyName: String, property: Provider<String>): String {
    val value = property.orNull?.trim()
    if (value.isNullOrEmpty()) {
        throw GradleException("Missing $propertyName in gradle.properties")
    }
    return value
}

fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val read = input.read(buffer)
            if (read == -1) {
                break
            }
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

fun loadGradleWrapperProperties(): Properties {
    val wrapperProperties = Properties()
    val wrapperPropertiesFile = file("gradle/wrapper/gradle-wrapper.properties")
    if (wrapperPropertiesFile.isFile) {
        wrapperPropertiesFile.inputStream().use { wrapperProperties.load(it) }
    }
    return wrapperProperties
}

fun isSafeRelativePath(path: String): Boolean {
    if (path.isBlank() || path.contains('\\') || path.startsWith("/") || path.contains(":")) {
        return false
    }

    val segments = path.split("/")
    if (segments.any { it.isBlank() || it == "." || it == ".." }) {
        return false
    }

    val rootPath = rootDir.toPath().normalize()
    return rootPath.resolve(path).normalize().startsWith(rootPath)
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

tasks.register("verifyBuildEnvironment") {
    group = "verification"
    description = "Verifies the local Gradle and Java runtime match the pinned build environment."

    inputs.property("expectedReleaseJavaVersion", expectedReleaseJavaVersion.orElse(""))
    inputs.property("expectedReleaseJavaVendor", expectedReleaseJavaVendor.orElse(""))
    inputs.property("expectedReleaseGradleVersion", expectedReleaseGradleVersion.orElse(""))
    outputs.upToDateWhen { false }

    doLast {
        val expectedJavaVersion = requiredGradleProperty("releaseBuild.javaVersion", expectedReleaseJavaVersion)
        val expectedJavaVendor = requiredGradleProperty("releaseBuild.javaVendor", expectedReleaseJavaVendor)
        val expectedGradleVersion = requiredGradleProperty("releaseBuild.gradleVersion", expectedReleaseGradleVersion)
        val actualJavaVersion = System.getProperty("java.version")
        val actualJavaVendor = System.getProperty("java.vendor")
        val violations = mutableListOf<String>()

        if (actualJavaVersion != expectedJavaVersion) {
            violations += "Java version expected $expectedJavaVersion but was $actualJavaVersion"
        }
        if (actualJavaVendor != expectedJavaVendor) {
            violations += "Java vendor expected $expectedJavaVendor but was $actualJavaVendor"
        }
        if (gradle.gradleVersion != expectedGradleVersion) {
            violations += "Gradle version expected $expectedGradleVersion but was ${gradle.gradleVersion}"
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Build environment verification failed:\n" +
                        violations.joinToString("\n") { "  - $it" }
            )
        }

        logger.lifecycle(
            "Verified build environment: Gradle ${gradle.gradleVersion}, Java $actualJavaVersion ($actualJavaVendor)."
        )
    }
}

tasks.register("verifyReleaseEnvironment") {
    group = "verification"
    description = "Verifies the local Gradle and Java runtime match the pinned release build environment."
    dependsOn("verifyBuildEnvironment")
}

tasks.register("verifyGradleWrapperSecurity") {
    group = "verification"
    description = "Verifies Gradle wrapper checksums and pinned distribution metadata."

    val requiredPaths = listOf(
        "gradlew",
        "gradlew.bat",
        "gradle/wrapper/gradle-wrapper.jar",
        "gradle/wrapper/gradle-wrapper.properties",
    )

    inputs.file(gradleWrapperChecksums)
    inputs.property("expectedReleaseGradleVersion", expectedReleaseGradleVersion.orElse(""))
    inputs.files(requiredPaths.map { file(it) })
    outputs.upToDateWhen { false }

    doLast {
        val checksumFile = gradleWrapperChecksums.asFile
        if (!checksumFile.isFile) {
            throw GradleException("Missing $checksumFile. Record the approved Gradle wrapper file checksums first.")
        }

        val requiredPathSet = requiredPaths.toSet()
        val expectedHashes = TreeMap<String, String>()
        val duplicatePaths = mutableSetOf<String>()
        val entryRegex = Regex("""^([0-9a-f]{64}) {2}(.+)$""")

        checksumFile.useLines(StandardCharsets.UTF_8) { lines ->
            lines.forEachIndexed { index, line ->
                val lineNumber = index + 1
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    return@forEachIndexed
                }

                val match = entryRegex.matchEntire(line)
                    ?: throw GradleException(
                        "Invalid Gradle wrapper checksum entry at $checksumFile:$lineNumber. " +
                                "Expected '<sha256>  <path>'."
                    )

                val path = match.groupValues[2]
                if (!isSafeRelativePath(path)) {
                    throw GradleException("Invalid Gradle wrapper checksum path at $checksumFile:$lineNumber: $path")
                }
                if (expectedHashes.put(path, match.groupValues[1]) != null) {
                    duplicatePaths += path
                }
            }
        }

        val missingExpectedPaths = requiredPathSet.filterNot { expectedHashes.containsKey(it) }
        val unexpectedPaths = expectedHashes.keys.filterNot { requiredPathSet.contains(it) }
        val violations = mutableListOf<String>()

        if (duplicatePaths.isNotEmpty()) {
            violations += "Duplicate checksum entries: ${duplicatePaths.sorted().joinToString(", ")}"
        }
        if (missingExpectedPaths.isNotEmpty()) {
            violations += "Missing checksum entries: ${missingExpectedPaths.sorted().joinToString(", ")}"
        }
        if (unexpectedPaths.isNotEmpty()) {
            violations += "Unexpected checksum entries: ${unexpectedPaths.sorted().joinToString(", ")}"
        }

        requiredPaths.forEach { path ->
            val wrapperFile = file(path)
            if (!wrapperFile.isFile) {
                violations += "Missing Gradle wrapper file: $path"
                return@forEach
            }

            val expectedHash = expectedHashes[path] ?: return@forEach
            val actualHash = sha256(wrapperFile)
            if (actualHash != expectedHash) {
                violations += "$path expected $expectedHash but was $actualHash"
            }
        }

        val wrapperProperties = loadGradleWrapperProperties()
        val distributionUrl = wrapperProperties.getProperty("distributionUrl")
        val distributionSha256Sum = wrapperProperties.getProperty("distributionSha256Sum")
        val expectedGradleVersion = expectedReleaseGradleVersion.orNull?.trim()
        val distributionUrlRegex =
            Regex("""^https://services[.]gradle[.]org/distributions/gradle-[A-Za-z0-9][A-Za-z0-9_.+-]*-(bin|all)[.]zip$""")
        val sha256Regex = Regex("""^[0-9a-f]{64}$""")

        if (distributionUrl == null || !distributionUrlRegex.matches(distributionUrl)) {
            violations += "Gradle wrapper distributionUrl must use " +
                    "https://services.gradle.org/distributions/gradle-<version>-bin.zip or -all.zip"
        }
        if (distributionSha256Sum == null || !sha256Regex.matches(distributionSha256Sum)) {
            violations += "Gradle wrapper distributionSha256Sum must be a pinned lowercase SHA-256 value"
        }
        if (expectedGradleVersion.isNullOrEmpty()) {
            violations += "Missing releaseBuild.gradleVersion in gradle.properties"
        } else {
            val pinnedDistributionUrlRegex =
                Regex("""^https://services[.]gradle[.]org/distributions/gradle-${Regex.escape(expectedGradleVersion)}-(bin|all)[.]zip$""")
            if (distributionUrl != null && !pinnedDistributionUrlRegex.matches(distributionUrl)) {
                violations += "Gradle wrapper distributionUrl must use releaseBuild.gradleVersion $expectedGradleVersion"
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Gradle wrapper security verification failed:\n" +
                        violations.joinToString("\n") { "  - $it" }
            )
        }

        logger.lifecycle("Verified Gradle wrapper checksums and distribution metadata.")
    }
}

tasks.register("verifyGithubActionsSecurity") {
    group = "verification"
    description = "Verifies GitHub Actions workflows use pinned actions and non-floating build environments."

    val workflowFiles = fileTree(".github/workflows") {
        include("*.yml")
        include("*.yaml")
    }

    inputs.files(workflowFiles)
    inputs.property("expectedReleaseJavaVersion", expectedReleaseJavaVersion.orElse(""))
    outputs.upToDateWhen { false }

    doLast {
        val expectedJavaVersion = expectedReleaseJavaVersion.orNull?.trim()
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

        if (expectedJavaVersion.isNullOrEmpty()) {
            violations += "Missing releaseBuild.javaVersion in gradle.properties"
        }

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
                    } else if (!javaVersion.startsWith("\${{") &&
                        !expectedJavaVersion.isNullOrEmpty() &&
                        javaVersion != expectedJavaVersion) {
                        violations += "$workflowPath:$lineNumber uses Java version $javaVersion but releaseBuild.javaVersion is $expectedJavaVersion"
                    }
                }

                val javaMatrixMatch = javaMatrixRegex.find(line)
                if (javaMatrixMatch != null) {
                    javaMatrixMatch.groupValues[1]
                        .split(',')
                        .map { cleanYamlScalar(it) }
                        .filter { it.isNotEmpty() && !it.startsWith("\${{") }
                        .forEach {
                            if (!exactJavaVersionRegex.matches(it)) {
                                violations += "$workflowPath:$lineNumber uses a floating Java matrix version: $it"
                            } else if (!expectedJavaVersion.isNullOrEmpty() && it != expectedJavaVersion) {
                                violations += "$workflowPath:$lineNumber uses Java matrix version $it but releaseBuild.javaVersion is $expectedJavaVersion"
                            }
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
    dependsOn("verifyBuildEnvironment")
    dependsOn("verifyGradleWrapperSecurity")
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
