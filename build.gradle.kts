import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Delete

import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
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

apply(from = "gradle/dependency-security-overrides.gradle.kts")
apply(from = "gradle/dependency-verification.gradle.kts")

val gradleWrapperChecksums = layout.projectDirectory.file("gradle/wrapper/gradle-wrapper.sha256")
val expectedReleaseJavaVersion = providers.gradleProperty("releaseBuild.javaVersion")
val expectedReleaseJavaVendor = providers.gradleProperty("releaseBuild.javaVendor")
val expectedReleaseGradleVersion = providers.gradleProperty("releaseBuild.gradleVersion")
val releaseReadinessReportFile = layout.buildDirectory.file("reports/release/release-readiness.md")

data class CommandResult(
    val exitValue: Int,
    val stdout: String,
    val stderr: String,
) {
    fun failureDetails(): String {
        return listOf(stderr, stdout)
            .filter { it.trim().isNotEmpty() }
            .joinToString("\n")
            .trim()
    }
}

data class HttpResponse(
    val status: Int,
    val url: String,
    val body: ByteArray,
)

data class ProbeResult(
    val ok: Boolean,
    val status: String,
    val method: String,
    val details: String? = null,
)

data class ReadinessResult(
    val status: String,
    val name: String,
    val details: String,
)

data class GpgKeyInfo(
    val keyId: String,
    val created: String,
    val expires: String,
    val fingerprint: String,
)

val bisq2ReleaseKeyIds = listOf("E222AA02", "387C8307")
val bisq2InstallerReleaseAssets = listOf(
    "Bisq-%s-linux_x86_64.deb",
    "Bisq-%s-linux_arm64.deb",
    "Bisq-%s-linux_x86_64.rpm",
    "Bisq-%s-linux_arm64.rpm",
    "Bisq-%s-macos_x86_64.dmg",
    "Bisq-%s-macos_arm64.dmg",
    "Bisq-%s-win_x86_64.exe",
    "Bisq-%s-win_x86_64.msi",
)
val bisq2DesktopUpdateJarReleaseAssets = listOf(
    "desktop-app-%s-linux_x86_64-all.jar",
    "desktop-app-%s-linux_arm64-all.jar",
    "desktop-app-%s-macos_x86_64-all.jar",
    "desktop-app-%s-macos_arm64-all.jar",
    "desktop-app-%s-win_x86_64-all.jar",
)
val bisq2ReleaseArtifactExtensions = listOf("deb", "dmg", "exe", "jar", "msi", "rpm", "sha256")

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

fun htmlEscape(value: Any?): String {
    return value?.toString()
        ?.replace("&", "&amp;")
        ?.replace("<", "&lt;")
        ?.replace(">", "&gt;")
        ?.replace("|", "&#124;")
        ?: ""
}

fun normalizeReleaseVersion(rawReleaseVersion: String?): String {
    if (rawReleaseVersion == null || rawReleaseVersion.trim().isEmpty()) {
        throw GradleException("Missing required -PreleaseVersion=<version>, for example -PreleaseVersion=2.1.11")
    }

    val releaseVersion = rawReleaseVersion.trim().removePrefix("v")
    if (!Regex("""\d+\.\d+\.\d+([.-][A-Za-z0-9_.-]+)?""").matches(releaseVersion)) {
        throw GradleException(
            "Invalid release version '$rawReleaseVersion'. Expected a version such as 2.1.11 or v2.1.11."
        )
    }
    return releaseVersion
}

fun resolveGpgExecutable(configuredGpgExecutable: String?): String {
    val configured = configuredGpgExecutable?.trim()
    if (!configured.isNullOrEmpty()) {
        return configured
    }

    return listOf(
        "/opt/homebrew/bin/gpg",
        "/usr/local/bin/gpg",
        "/usr/bin/gpg",
    ).firstOrNull { File(it).canExecute() } ?: "gpg"
}

fun normalizeGpgFingerprintOrNull(fingerprint: String): String? {
    val normalizedFingerprint = fingerprint.replace(Regex("\\s+"), "")
        .uppercase(Locale.ROOT)
    return if (normalizedFingerprint.matches(Regex("[0-9A-F]{40}"))) {
        normalizedFingerprint
    } else {
        null
    }
}

fun parseGpgKeyInfo(gpgOutput: String): GpgKeyInfo? {
    val lines = gpgOutput.lines()
    val pubLine = lines.find { it.startsWith("pub:") } ?: return null
    val fprLine = lines.find { it.startsWith("fpr:") } ?: return null
    val pubFields = pubLine.split(':')
    val fprFields = fprLine.split(':')
    return GpgKeyInfo(
        keyId = pubFields.getOrElse(4) { "" },
        created = pubFields.getOrElse(5) { "" },
        expires = pubFields.getOrElse(6) { "" },
        fingerprint = fprFields.getOrElse(9) { "" },
    )
}

fun parseGpgValidSigFingerprints(statusOutput: String): Set<String> {
    return statusOutput.lineSequence()
        .filter { it.startsWith("[GNUPG:] VALIDSIG ") }
        .flatMap { line ->
            val tokens = line.split(Regex("\\s+"))
            listOfNotNull(tokens.getOrNull(2), tokens.lastOrNull())
        }
        .mapNotNull(::normalizeGpgFingerprintOrNull)
        .toSet()
}

fun gpgAssertSignerVerifyCommand(gpgExecutable: String,
                                 expectedSignerFingerprints: Collection<String>,
                                 signatureFile: File,
                                 artifactFile: File,
                                 extraOptions: List<String> = emptyList()): List<String> {
    val normalizedExpectedSignerFingerprints = expectedSignerFingerprints
        .map { fingerprint ->
            normalizeGpgFingerprintOrNull(fingerprint)
                ?: throw GradleException("Expected signer fingerprint must be a full 40-character GPG fingerprint: $fingerprint")
        }
        .distinct()
    if (normalizedExpectedSignerFingerprints.isEmpty()) {
        throw GradleException("At least one expected signer fingerprint is required")
    }

    return listOf(gpgExecutable) +
        extraOptions +
        listOf("--batch", "--status-fd", "1", "--with-colons") +
        normalizedExpectedSignerFingerprints.flatMap { listOf("--assert-signer", it) } +
        listOf("--verify", signatureFile.absolutePath, artifactFile.absolutePath)
}

fun validateGpgVerifiedExpectedSigner(verifyResult: CommandResult,
                                      expectedSignerFingerprints: Collection<String>): String? {
    val expectedSignerFingerprintSet = expectedSignerFingerprints
        .map { fingerprint ->
            normalizeGpgFingerprintOrNull(fingerprint)
                ?: return "Invalid expected signer fingerprint: $fingerprint"
        }
        .toSet()
    val validSigFingerprints = parseGpgValidSigFingerprints(verifyResult.stdout)
    val matchingFingerprints = validSigFingerprints.intersect(expectedSignerFingerprintSet)
    if (matchingFingerprints.isEmpty()) {
        return "VALIDSIG fingerprints ${validSigFingerprints.joinToString().ifEmpty { "<none>" }} " +
            "did not match expected signer fingerprints ${expectedSignerFingerprintSet.joinToString()}"
    }
    return null
}

fun readBisq2ActiveSigningKeyId(signingKeyFile: File): String {
    if (!signingKeyFile.isFile) {
        throw GradleException("Missing active signing key id marker: $signingKeyFile")
    }

    val activeSigningKeyId = signingKeyFile.readText(StandardCharsets.UTF_8)
        .trim()
        .uppercase(Locale.ROOT)
    if (!bisq2ReleaseKeyIds.contains(activeSigningKeyId)) {
        throw GradleException(
            "Active signing key id marker $signingKeyFile points to " +
                "${activeSigningKeyId.ifEmpty { "<blank>" }}, expected one of ${bisq2ReleaseKeyIds.joinToString(", ")}"
        )
    }
    return activeSigningKeyId
}

fun loadBisq2MaintainerPublicKeyInfo(gpgExecutable: String,
                                     keyId: String,
                                     keyFile: File,
                                     runCommand: (List<String>) -> CommandResult): GpgKeyInfo {
    if (!keyFile.isFile) {
        throw GradleException("Missing public key file for $keyId: $keyFile")
    }

    val gpgResult = runCommand(
        listOf(
            gpgExecutable,
            "--show-keys",
            "--with-colons",
            "--fingerprint",
            keyFile.absolutePath,
        )
    )
    if (gpgResult.exitValue != 0) {
        throw GradleException(
            "$gpgExecutable --show-keys failed for $keyFile. " +
                "Install GnuPG or set -PgpgExecutable=/path/to/gpg. ${gpgResult.failureDetails()}"
        )
    }

    val keyInfo = parseGpgKeyInfo(gpgResult.stdout)
        ?: throw GradleException("Could not parse fingerprint from gpg --show-keys output for $keyFile")
    val normalizedFingerprint = normalizeGpgFingerprintOrNull(keyInfo.fingerprint)
        ?: throw GradleException("Could not parse full fingerprint from gpg --show-keys output for $keyFile")
    if (!normalizedFingerprint.endsWith(keyId.uppercase(Locale.ROOT))) {
        throw GradleException("$keyFile has fingerprint $normalizedFingerprint, expected it to end with $keyId")
    }

    return keyInfo.copy(fingerprint = normalizedFingerprint)
}

fun expectedBisq2ReleaseAssets(releaseVersion: String): List<String> {
    val signableArtifacts = expectedBisq2SignableReleaseAssets(releaseVersion)
    val keyAssets = bisq2ReleaseKeyIds.map { "$it.asc" } + "signingkey.asc"
    return (keyAssets + signableArtifacts + signableArtifacts.map { "$it.asc" }).sorted()
}

fun expectedBisq2SignableReleaseAssets(releaseVersion: String): List<String> {
    val installers = bisq2InstallerReleaseAssets.map { it.format(releaseVersion) }
    val updateJars = bisq2DesktopUpdateJarReleaseAssets.map { it.format(releaseVersion) }
    val jarHash = "Bisq-$releaseVersion-all-jars.sha256"
    return installers + updateJars + jarHash
}

fun isExpectedBisq2ReleaseArtifact(candidate: File): Boolean {
    if (!candidate.isFile || candidate.name.startsWith(".") || candidate.name.endsWith(".asc")) {
        return false
    }

    val lowerName = candidate.name.lowercase(Locale.ROOT)
    return bisq2ReleaseArtifactExtensions.any { extension ->
        lowerName.endsWith(".${extension.lowercase(Locale.ROOT)}")
    }
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

tasks.register("signReleaseArtifacts") {
    group = "distribution"
    description = "Signs Bisq 2 release artifacts in -Pbisq.release.dir with detached armored GPG signatures."

    val releaseDirProperty = providers.gradleProperty("bisq.release.dir")
        .orElse(providers.gradleProperty("releaseDir"))
    val gpgUserProperty = providers.gradleProperty("gpgUser")
        .orElse(providers.gradleProperty("bisqGpgUser"))
        .orElse(providers.environmentVariable("BISQ_GPG_USER"))
    val gpgExecutableProperty = providers.gradleProperty("gpgExecutable")
        .orElse(providers.environmentVariable("GPG_EXECUTABLE"))

    inputs.property("releaseDir", releaseDirProperty.orElse(""))
    inputs.property("gpgUser", gpgUserProperty.orElse(""))
    inputs.property("gpgExecutable", gpgExecutableProperty.orElse(""))
    outputs.upToDateWhen { false }

    doLast {
        fun execResult(commandLine: List<String>): CommandResult {
            val stdout = ByteArrayOutputStream()
            val stderr = ByteArrayOutputStream()
            val result = exec {
                commandLine(commandLine)
                standardOutput = stdout
                errorOutput = stderr
                isIgnoreExitValue = true
            }
            return CommandResult(
                result.exitValue,
                stdout.toString(StandardCharsets.UTF_8.name()),
                stderr.toString(StandardCharsets.UTF_8.name())
            )
        }

        val rawReleaseDir = releaseDirProperty.orNull
        if (rawReleaseDir == null || rawReleaseDir.trim().isEmpty()) {
            throw GradleException(
                "Missing required -Pbisq.release.dir=<directory>. You can also use -PreleaseDir=<directory>."
            )
        }

        val gpgUser = gpgUserProperty.orNull?.trim()
        if (gpgUser.isNullOrEmpty()) {
            throw GradleException(
                "Missing required -PgpgUser=<key-id-or-email>. " +
                    "You can also use -PbisqGpgUser=<key-id-or-email> or BISQ_GPG_USER."
            )
        }

        val releaseDir = file(rawReleaseDir.trim())
        if (!releaseDir.isDirectory) {
            throw GradleException("Release directory is not a directory: $releaseDir")
        }

        val releaseDirFiles = releaseDir.listFiles()
            ?: throw GradleException("Cannot list release directory: $releaseDir. Check that it is readable.")

        val artifacts = releaseDirFiles
            .filter(::isExpectedBisq2ReleaseArtifact)
            .sortedBy { it.name }

        if (artifacts.isEmpty()) {
            throw GradleException("No Bisq 2 release artifacts found to sign in $releaseDir")
        }

        val gpgExecutable = resolveGpgExecutable(gpgExecutableProperty.orNull)
        val activeSigningKeyId = readBisq2ActiveSigningKeyId(
            file("apps/desktop/desktop-app-launcher/maintainer_public_keys/signingkey.asc")
        )
        val activeSigningKeyInfo = loadBisq2MaintainerPublicKeyInfo(
            gpgExecutable,
            activeSigningKeyId,
            file("apps/desktop/desktop-app-launcher/maintainer_public_keys/$activeSigningKeyId.asc"),
            ::execResult
        )
        val expectedSignerFingerprints = listOf(activeSigningKeyInfo.fingerprint)

        logger.lifecycle("Signing ${artifacts.size} Bisq 2 release artifact(s) in ${releaseDir.absolutePath}")
        artifacts.forEach { artifact ->
            val signatureFile = File(artifact.parentFile, "${artifact.name}.asc")
            val signResult = execResult(
                listOf(
                    gpgExecutable,
                    "--yes",
                    "--digest-algo", "SHA256",
                    "--local-user", gpgUser,
                    "--output", signatureFile.absolutePath,
                    "--detach-sig",
                    "--armor",
                    artifact.absolutePath,
                )
            )
            if (signResult.exitValue != 0) {
                throw GradleException("Failed to sign ${artifact.name}: ${signResult.failureDetails()}")
            }
            if (!signatureFile.isFile || signatureFile.length() == 0L) {
                throw GradleException("GPG did not create a non-empty signature file for ${artifact.name}: $signatureFile")
            }

            val verifyResult = execResult(
                gpgAssertSignerVerifyCommand(
                    gpgExecutable,
                    expectedSignerFingerprints,
                    signatureFile,
                    artifact,
                )
            )
            if (verifyResult.exitValue != 0) {
                throw GradleException("Failed to verify ${signatureFile.name}: ${verifyResult.failureDetails()}")
            }
            validateGpgVerifiedExpectedSigner(verifyResult, expectedSignerFingerprints)?.let { message ->
                throw GradleException("Failed to verify signer for ${signatureFile.name}: $message")
            }

            logger.lifecycle("Signed and verified ${artifact.name} -> ${signatureFile.name}")
        }
    }
}

tasks.register("verifyReleaseReadiness") {
    group = "verification"
    description = "Read-only check for Bisq 2 release assets, signer keys, key expiry, and download URLs."

    val releaseVersionProperty = providers.gradleProperty("releaseVersion")
    val gpgExecutableProperty = providers.gradleProperty("gpgExecutable")
        .orElse(providers.environmentVariable("GPG_EXECUTABLE"))

    inputs.property("releaseVersion", releaseVersionProperty.orElse(""))
    inputs.property("gpgExecutable", gpgExecutableProperty.orElse(""))
    outputs.file(releaseReadinessReportFile)
    outputs.upToDateWhen { false }

    doLast {
        fun addResult(results: MutableList<ReadinessResult>, status: String, name: String, details: String?) {
            results += ReadinessResult(status, name, details.orEmpty())
        }

        fun escapeMarkdownCell(value: Any?): String {
            return htmlEscape(value)
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\n", "<br>")
        }

        fun ensureDirectory(directory: File, label: String) {
            if (!directory.mkdirs() && !directory.isDirectory) {
                throw GradleException("Failed to create $label: $directory")
            }
        }

        fun resetTemporaryDirectory(directory: File, label: String) {
            if (directory.exists() && !directory.deleteRecursively()) {
                throw GradleException("Failed to delete $label before reuse: $directory")
            }
            ensureDirectory(directory, label)
        }

        fun httpRequest(requestUrl: String,
                        requestMethod: String,
                        headers: Map<String, String>,
                        readBody: Boolean): HttpResponse {
            var currentUrl = requestUrl
            var method = requestMethod
            var redirectCount = 0

            while (true) {
                val connection = URL(currentUrl).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.requestMethod = method
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.setRequestProperty("User-Agent", "Bisq 2 release readiness Gradle task")
                headers.forEach { (name, value) ->
                    if (value.isNotEmpty()) {
                        connection.setRequestProperty(name, value)
                    }
                }

                val status = connection.responseCode
                if (status in listOf(301, 302, 303, 307, 308)) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (location.isNullOrEmpty()) {
                        return HttpResponse(status, currentUrl, ByteArray(0))
                    }
                    redirectCount += 1
                    if (redirectCount > 5) {
                        throw GradleException("Too many redirects while requesting $requestUrl")
                    }
                    currentUrl = URL(URL(currentUrl), location).toString()
                    if (status == 303) {
                        method = "GET"
                    }
                    continue
                }

                val body = if (readBody) {
                    val stream = if (status >= 400) connection.errorStream else connection.inputStream
                    stream?.use { it.readBytes() } ?: ByteArray(0)
                } else {
                    ByteArray(0)
                }
                connection.disconnect()
                return HttpResponse(status, currentUrl, body)
            }
        }

        fun httpGetBytes(url: String, headers: Map<String, String> = emptyMap()): ByteArray {
            val response = httpRequest(url, "GET", headers, true)
            if (response.status < 200 || response.status >= 300) {
                var body = if (response.body.isNotEmpty()) {
                    String(response.body, StandardCharsets.UTF_8).trim()
                } else {
                    ""
                }
                if (body.length > 300) {
                    body = body.substring(0, 300) + "..."
                }
                throw GradleException("GET $url returned HTTP ${response.status}${if (body.isNotEmpty()) ": $body" else ""}")
            }
            return response.body
        }

        fun httpDownloadToFile(requestUrl: String, outputFile: File, headers: Map<String, String> = emptyMap()) {
            var currentUrl = requestUrl
            var redirectCount = 0

            while (true) {
                val connection = URL(currentUrl).openConnection() as HttpURLConnection
                try {
                    connection.instanceFollowRedirects = false
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 15_000
                    connection.readTimeout = 120_000
                    connection.setRequestProperty("User-Agent", "Bisq 2 release readiness Gradle task")
                    headers.forEach { (name, value) ->
                        if (value.isNotEmpty()) {
                            connection.setRequestProperty(name, value)
                        }
                    }

                    val status = connection.responseCode
                    if (status in listOf(301, 302, 303, 307, 308)) {
                        val location = connection.getHeaderField("Location")
                        if (location.isNullOrEmpty()) {
                            throw GradleException("GET $requestUrl returned HTTP $status without a Location header")
                        }
                        redirectCount += 1
                        if (redirectCount > 5) {
                            throw GradleException("Too many redirects while requesting $requestUrl")
                        }
                        currentUrl = URL(URL(currentUrl), location).toString()
                        continue
                    }

                    if (status < 200 || status >= 300) {
                        var body = connection.errorStream?.use { String(it.readBytes(), StandardCharsets.UTF_8).trim() }.orEmpty()
                        if (body.length > 300) {
                            body = body.substring(0, 300) + "..."
                        }
                        throw GradleException("GET $requestUrl returned HTTP $status${if (body.isNotEmpty()) ": $body" else ""}")
                    }

                    outputFile.parentFile.mkdirs()
                    connection.inputStream.use { input ->
                        outputFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    return
                } finally {
                    connection.disconnect()
                }
            }
        }

        fun httpProbe(url: String): ProbeResult {
            var headFailure: String? = null
            try {
                val response = httpRequest(url, "HEAD", emptyMap(), false)
                if (response.status in 200..399) {
                    return ProbeResult(true, response.status.toString(), "HEAD")
                }
                if (response.status !in listOf(403, 405)) {
                    return ProbeResult(false, response.status.toString(), "HEAD")
                }
            } catch (exception: Exception) {
                headFailure = exception.message
            }

            return try {
                val response = httpRequest(url, "GET", mapOf("Range" to "bytes=0-0"), false)
                ProbeResult(
                    response.status in 200..399,
                    response.status.toString(),
                    "GET Range",
                    headFailure
                )
            } catch (exception: Exception) {
                ProbeResult(false, "n/a", "GET Range", exception.message ?: headFailure)
            }
        }

        fun runCommand(commandLine: List<String>): CommandResult {
            val stdout = ByteArrayOutputStream()
            val stderr = ByteArrayOutputStream()
            return try {
                val processResult = exec {
                    commandLine(commandLine)
                    standardOutput = stdout
                    errorOutput = stderr
                    isIgnoreExitValue = true
                }
                CommandResult(
                    processResult.exitValue,
                    stdout.toString(StandardCharsets.UTF_8.name()),
                    stderr.toString(StandardCharsets.UTF_8.name())
                )
            } catch (exception: Exception) {
                CommandResult(
                    -1,
                    stdout.toString(StandardCharsets.UTF_8.name()),
                    exception.message.orEmpty()
                )
            }
        }

        fun readLauncherKeyIds(): List<String> {
            val launcherFile = file("apps/desktop/desktop-app-launcher/src/main/java/bisq/desktop_app_launcher/DesktopAppLauncher.java")
            if (!launcherFile.isFile) {
                return emptyList()
            }

            val keyIdRegex = Regex("private\\s+static\\s+final\\s+String\\s+FINGER_PRINT_[A-Z_]+\\s*=\\s*\"([0-9A-Fa-f]{8})\"")
            return keyIdRegex.findAll(launcherFile.readText(StandardCharsets.UTF_8))
                .map { it.groupValues[1].uppercase(Locale.ROOT) }
                .distinct()
                .sorted()
                .toList()
        }

        fun checkVersionValue(results: MutableList<ReadinessResult>,
                              releaseVersion: String,
                              label: String,
                              sourceFile: File,
                              regex: Regex) {
            if (!sourceFile.isFile) {
                addResult(results, "FAIL", label, "Missing ${rootProject.relativePath(sourceFile)}")
                return
            }
            val match = regex.find(sourceFile.readText(StandardCharsets.UTF_8))
            if (match == null) {
                addResult(results, "FAIL", label, "Could not find version declaration in ${rootProject.relativePath(sourceFile)}")
                return
            }
            val actualVersion = match.groupValues[1]
            if (actualVersion == releaseVersion) {
                addResult(results, "PASS", label, "${rootProject.relativePath(sourceFile)} is set to $actualVersion")
            } else {
                addResult(results, "FAIL", label, "${rootProject.relativePath(sourceFile)} is $actualVersion, expected $releaseVersion")
            }
        }

        val releaseVersion = normalizeReleaseVersion(releaseVersionProperty.orNull)
        val releaseTag = "v$releaseVersion"
        val expectedAssets = expectedBisq2ReleaseAssets(releaseVersion)
        val results = mutableListOf<ReadinessResult>()

        checkVersionValue(
            results,
            releaseVersion,
            "gradle.properties version",
            file("gradle.properties"),
            Regex("""(?m)^\s*version\s*=\s*([^\s#]+)""")
        )

        val updaterUtilsFile = file("evolution/src/main/java/bisq/evolution/updater/UpdaterUtils.java")
        if (!updaterUtilsFile.isFile) {
            addResult(results, "FAIL", "Updater URL constants", "Missing ${rootProject.relativePath(updaterUtilsFile)}")
        } else {
            val updaterUtils = updaterUtilsFile.readText(StandardCharsets.UTF_8)
            val expectedConstants = mapOf(
                "RELEASES_URL" to "RELEASES_URL = \"https://github.com/bisq-network/bisq2/releases/tag/v\"",
                "GITHUB_DOWNLOAD_URL" to "GITHUB_DOWNLOAD_URL = \"https://github.com/bisq-network/bisq2/releases/download/v\"",
                "PUB_KEYS_URL" to "PUB_KEYS_URL = \"https://bisq.network/pubkey/\"",
            )
            val missingConstants = expectedConstants
                .filterValues { !updaterUtils.contains(it) }
                .keys
                .sorted()
            if (missingConstants.isEmpty()) {
                addResult(results, "PASS", "Updater URL constants", "UpdaterUtils points to Bisq 2 GitHub releases and bisq.network pubkeys")
            } else {
                addResult(results, "FAIL", "Updater URL constants", "Unexpected or missing constants: ${missingConstants.joinToString(", ")}")
            }
        }

        val githubToken = providers.gradleProperty("githubToken").orNull
            ?: providers.environmentVariable("GITHUB_TOKEN").orNull
        val githubHeaders = mutableMapOf(
            "Accept" to "application/vnd.github+json",
            "X-GitHub-Api-Version" to "2022-11-28",
        )
        if (!githubToken.isNullOrEmpty()) {
            githubHeaders["Authorization"] = "Bearer $githubToken"
        }

        var releaseAssetsByName: Map<String, Map<*, *>> = emptyMap()
        var releaseAssetsLoaded = false
        val githubReleaseApiUrl = "https://api.github.com/repos/bisq-network/bisq2/releases/tags/$releaseTag"
        try {
            val releaseJson = String(httpGetBytes(githubReleaseApiUrl, githubHeaders), StandardCharsets.UTF_8)
            val release = JsonSlurper().parseText(releaseJson) as Map<*, *>
            if (release["tag_name"] == releaseTag) {
                addResult(results, "PASS", "GitHub release tag", "Found $releaseTag on GitHub release page")
            } else {
                addResult(results, "FAIL", "GitHub release tag", "GitHub API returned tag '${release["tag_name"]}', expected '$releaseTag'")
            }
            if (release["draft"] == true) {
                addResult(results, "WARN", "GitHub release state", "$releaseTag is still marked as a draft")
            } else {
                addResult(results, "PASS", "GitHub release state", "$releaseTag is published")
            }
            if (release["prerelease"] == true) {
                addResult(results, "WARN", "GitHub prerelease flag", "$releaseTag is marked as a prerelease")
            }

            val assets = release["assets"] as? List<*> ?: emptyList<Any>()
            releaseAssetsByName = assets
                .mapNotNull { it as? Map<*, *> }
                .associateBy { it["name"].toString() }
            releaseAssetsLoaded = true
            addResult(results, "PASS", "GitHub release API", "Loaded ${releaseAssetsByName.size} uploaded asset(s) from $githubReleaseApiUrl")
        } catch (exception: Exception) {
            addResult(results, "FAIL", "GitHub release API", exception.message)
        }

        if (releaseAssetsLoaded) {
            expectedAssets.forEach { assetName ->
                val asset = releaseAssetsByName[assetName]
                if (asset == null) {
                    addResult(results, "FAIL", "GitHub asset $assetName", "Missing from uploaded release assets")
                    return@forEach
                }

                val browserDownloadUrl = asset["browser_download_url"]?.toString().orEmpty()
                if (browserDownloadUrl.isEmpty()) {
                    addResult(results, "FAIL", "GitHub asset $assetName", "Missing browser_download_url")
                    return@forEach
                }

                val assetSize = (asset["size"] as? Number)?.toLong() ?: 0L
                if (assetSize <= 0L) {
                    addResult(results, "FAIL", "GitHub asset $assetName", "Asset exists but size is ${asset["size"]}")
                    return@forEach
                }
                addResult(results, "PASS", "GitHub asset $assetName", "Present, $assetSize byte(s)")

                val probe = httpProbe(browserDownloadUrl)
                if (probe.ok) {
                    addResult(results, "PASS", "GitHub download URL $assetName", "HTTP ${probe.status} via ${probe.method}")
                } else {
                    val details = if (probe.details.isNullOrEmpty()) "" else ": ${probe.details}"
                    addResult(results, "FAIL", "GitHub download URL $assetName", "HTTP ${probe.status} via ${probe.method}$details")
                }
            }

            val extraAssets = releaseAssetsByName.keys - expectedAssets.toSet()
            if (extraAssets.isNotEmpty()) {
                addResult(results, "WARN", "GitHub extra assets", "Unexpected uploaded asset(s): ${extraAssets.sorted().joinToString(", ")}")
            }
        }

        val launcherKeyIds = readLauncherKeyIds()
        if (launcherKeyIds.isEmpty()) {
            addResult(results, "FAIL", "Launcher signer key ids", "Could not parse signer key ids from DesktopAppLauncher")
        } else if (launcherKeyIds == bisq2ReleaseKeyIds.sorted()) {
            addResult(results, "PASS", "Launcher signer key ids", "DesktopAppLauncher signer ids are ${launcherKeyIds.joinToString(", ")}")
        } else {
            addResult(
                results,
                "FAIL",
                "Launcher signer key ids",
                "DesktopAppLauncher signer ids are ${launcherKeyIds.joinToString(", ")}, expected ${bisq2ReleaseKeyIds.sorted().joinToString(", ")}"
            )
        }

        val signingKeyFile = file("apps/desktop/desktop-app-launcher/maintainer_public_keys/signingkey.asc")
        val activeSigningKeyId = if (!signingKeyFile.isFile) {
            addResult(results, "FAIL", "signingkey.asc local file", "Missing ${rootProject.relativePath(signingKeyFile)}")
            ""
        } else {
            val keyId = signingKeyFile.readText(StandardCharsets.UTF_8).trim().uppercase(Locale.ROOT)
            if (bisq2ReleaseKeyIds.contains(keyId)) {
                addResult(results, "PASS", "Active signing key id", "${rootProject.relativePath(signingKeyFile)} points to $keyId")
            } else {
                addResult(
                    results,
                    "FAIL",
                    "Active signing key id",
                    "${rootProject.relativePath(signingKeyFile)} points to ${keyId.ifEmpty { "<blank>" }}, expected one of ${bisq2ReleaseKeyIds.joinToString(", ")}"
                )
            }
            keyId
        }

        if (signingKeyFile.isFile && releaseAssetsLoaded) {
            val signingKeyAsset = releaseAssetsByName["signingkey.asc"]
            val browserDownloadUrl = signingKeyAsset?.get("browser_download_url")?.toString().orEmpty()
            if (browserDownloadUrl.isEmpty()) {
                addResult(results, "FAIL", "signingkey.asc GitHub asset", "Missing signingkey.asc from GitHub release")
            } else {
                try {
                    val githubSigningKeyId = String(httpGetBytes(browserDownloadUrl), StandardCharsets.UTF_8)
                        .trim()
                        .uppercase(Locale.ROOT)
                    if (githubSigningKeyId == activeSigningKeyId && bisq2ReleaseKeyIds.contains(githubSigningKeyId)) {
                        addResult(results, "PASS", "signingkey.asc marker", "GitHub release points to active key $githubSigningKeyId")
                    } else {
                        addResult(
                            results,
                            "FAIL",
                            "signingkey.asc marker",
                            "local=${activeSigningKeyId.ifEmpty { "<invalid>" }}, GitHub release=${githubSigningKeyId.ifEmpty { "<blank>" }}"
                        )
                    }
                } catch (exception: Exception) {
                    addResult(results, "FAIL", "signingkey.asc GitHub asset", exception.message)
                }
            }
        }

        val gpgExecutable = resolveGpgExecutable(gpgExecutableProperty.orNull)
        val publicKeySourceDir = layout.buildDirectory
            .dir("tmp/verifyReleaseReadiness/public-keys")
            .get()
            .asFile
        resetTemporaryDirectory(publicKeySourceDir, "release readiness public key source directory")

        fun safeFileName(value: String): String {
            return value.replace(Regex("[^A-Za-z0-9._-]"), "_")
        }

        fun writePublicKeySource(keyId: String, sourceName: String, bytes: ByteArray): File {
            val outputFile = File(publicKeySourceDir, "${keyId}_${safeFileName(sourceName)}.asc")
            outputFile.writeBytes(bytes)
            return outputFile
        }

        fun readPublicKeySourceFingerprint(keyId: String, sourceName: String, keyFile: File): String? {
            val gpgResult = runCommand(
                listOf(
                    gpgExecutable,
                    "--show-keys",
                    "--with-colons",
                    "--fingerprint",
                    keyFile.absolutePath,
                )
            )
            if (gpgResult.exitValue != 0) {
                addResult(
                    results,
                    "FAIL",
                    "Public key $keyId $sourceName metadata",
                    "$gpgExecutable --show-keys failed. Install GnuPG or set -PgpgExecutable=/path/to/gpg. ${gpgResult.stderr.trim()}"
                )
                return null
            }

            val keyInfo = parseGpgKeyInfo(gpgResult.stdout)
            if (keyInfo == null || keyInfo.fingerprint.isEmpty()) {
                addResult(results, "FAIL", "Public key $keyId $sourceName metadata", "Could not parse fingerprint from gpg --show-keys output")
                return null
            }
            val normalizedFingerprint = normalizeGpgFingerprintOrNull(keyInfo.fingerprint)
            if (normalizedFingerprint == null) {
                addResult(results, "FAIL", "Public key $keyId $sourceName metadata", "Could not parse full fingerprint ${keyInfo.fingerprint}")
                return null
            }
            if (!normalizedFingerprint.endsWith(keyId.uppercase(Locale.ROOT))) {
                addResult(results, "FAIL", "Public key $keyId $sourceName identity", "Fingerprint is $normalizedFingerprint")
                return null
            }
            return normalizedFingerprint
        }

        val resourceKeyFiles = mutableMapOf<String, File>()
        val publicKeySourceFilesByKeyId = mutableMapOf<String, MutableMap<String, File>>()
        val expectedSignerFingerprintsByKeyId = mutableMapOf<String, String>()
        bisq2ReleaseKeyIds.forEach { keyId ->
            val resourceFile = file("evolution/src/main/resources/keys/$keyId.asc")
            val packageFile = file("apps/desktop/desktop-app-launcher/maintainer_public_keys/$keyId.asc")
            resourceKeyFiles[keyId] = resourceFile

            if (resourceFile.isFile) {
                publicKeySourceFilesByKeyId.getOrPut(keyId) { mutableMapOf() }["app resources"] = resourceFile
            } else {
                addResult(results, "FAIL", "Public key $keyId app resource", "Missing ${rootProject.relativePath(resourceFile)}")
            }
            if (packageFile.isFile) {
                publicKeySourceFilesByKeyId.getOrPut(keyId) { mutableMapOf() }["release package"] = packageFile
            } else {
                addResult(results, "FAIL", "Public key $keyId package resource", "Missing ${rootProject.relativePath(packageFile)}")
            }

            try {
                publicKeySourceFilesByKeyId.getOrPut(keyId) { mutableMapOf() }["bisq.network/pubkey"] = writePublicKeySource(
                    keyId,
                    "bisq.network/pubkey",
                    httpGetBytes("https://bisq.network/pubkey/$keyId.asc")
                )
            } catch (exception: Exception) {
                addResult(results, "FAIL", "Public key $keyId webpage", exception.message)
            }

            val githubAsset = releaseAssetsByName["$keyId.asc"]
            val browserDownloadUrl = githubAsset?.get("browser_download_url")?.toString().orEmpty()
            if (browserDownloadUrl.isNotEmpty()) {
                try {
                    publicKeySourceFilesByKeyId.getOrPut(keyId) { mutableMapOf() }["GitHub release"] = writePublicKeySource(
                        keyId,
                        "GitHub release",
                        httpGetBytes(browserDownloadUrl)
                    )
                } catch (exception: Exception) {
                    addResult(results, "FAIL", "Public key $keyId GitHub asset", exception.message)
                }
            } else if (releaseAssetsLoaded) {
                addResult(results, "FAIL", "Public key $keyId GitHub asset", "Missing $keyId.asc from GitHub release")
            }

            val expectedKeySourceCount = if (releaseAssetsLoaded) 4 else 3
            val sourceFilesByName = publicKeySourceFilesByKeyId[keyId].orEmpty()
            if (sourceFilesByName.size == expectedKeySourceCount) {
                val fingerprints = sourceFilesByName.mapValues { (sourceName, keyFile) ->
                    readPublicKeySourceFingerprint(keyId, sourceName, keyFile)
                }
                if (fingerprints.values.all { it != null }) {
                    val uniqueFingerprints = fingerprints.values.filterNotNull().toSet()
                    if (uniqueFingerprints.size == 1) {
                        addResult(
                            results,
                            "PASS",
                            "Public key $keyId identity",
                            "All checked sources have fingerprint ${uniqueFingerprints.first()}; key metadata may differ"
                        )
                    } else {
                        addResult(
                            results,
                            "FAIL",
                            "Public key $keyId identity",
                            fingerprints.entries.joinToString(", ") { (name, fingerprint) -> "$name=${fingerprint ?: "<invalid>"}" }
                        )
                    }
                } else {
                    addResult(
                        results,
                        "FAIL",
                        "Public key $keyId identity",
                        fingerprints.entries.joinToString(", ") { (name, fingerprint) -> "$name=${fingerprint ?: "<invalid>"}" }
                    )
                }
            }
        }

        bisq2ReleaseKeyIds.forEach { keyId ->
            val keyFile = resourceKeyFiles[keyId]
            if (keyFile?.isFile != true) {
                return@forEach
            }

            val gpgResult = runCommand(
                listOf(
                    gpgExecutable,
                    "--show-keys",
                    "--with-colons",
                    "--fingerprint",
                    keyFile.absolutePath,
                )
            )
            if (gpgResult.exitValue != 0) {
                addResult(
                    results,
                    "FAIL",
                    "Public key $keyId metadata",
                    "$gpgExecutable --show-keys failed. Install GnuPG or set -PgpgExecutable=/path/to/gpg. ${gpgResult.stderr.trim()}"
                )
                return@forEach
            }

            val keyInfo = parseGpgKeyInfo(gpgResult.stdout)
            if (keyInfo == null || keyInfo.fingerprint.isEmpty()) {
                addResult(results, "FAIL", "Public key $keyId metadata", "Could not parse fingerprint from gpg --show-keys output")
                return@forEach
            }
            val normalizedFingerprint = normalizeGpgFingerprintOrNull(keyInfo.fingerprint)
            if (normalizedFingerprint == null) {
                addResult(results, "FAIL", "Public key $keyId metadata", "Could not parse full fingerprint ${keyInfo.fingerprint}")
                return@forEach
            }
            expectedSignerFingerprintsByKeyId[keyId] = normalizedFingerprint

            val isActiveSigningKey = activeSigningKeyId.isNotEmpty() &&
                (keyId.equals(activeSigningKeyId, ignoreCase = true) ||
                    keyInfo.keyId.uppercase(Locale.ROOT).endsWith(activeSigningKeyId))
            val now = Instant.now()
            if (keyInfo.expires.isEmpty()) {
                addResult(results, "PASS", "Public key $keyId expiry", "Fingerprint ${keyInfo.fingerprint} has no expiry date")
            } else {
                val expiryInstant = Instant.ofEpochSecond(keyInfo.expires.toLong())
                val expiryDate = expiryInstant.atZone(ZoneOffset.UTC).toLocalDate()
                val daysRemaining = Duration.between(now, expiryInstant).toDays()
                when {
                    !expiryInstant.isAfter(now) -> {
                        addResult(
                            results,
                            if (isActiveSigningKey) "FAIL" else "WARN",
                            "Public key $keyId expiry",
                            "${if (isActiveSigningKey) "Active signing key" else "Bundled non-active key"} expired on $expiryDate"
                        )
                    }
                    daysRemaining <= 90 -> {
                        addResult(results, "WARN", "Public key $keyId expiry", "Expires on $expiryDate ($daysRemaining day(s) remaining)")
                    }
                    else -> {
                        addResult(results, "PASS", "Public key $keyId expiry", "Expires on $expiryDate ($daysRemaining day(s) remaining)")
                    }
                }
            }
        }

        if (releaseAssetsLoaded) {
            val activeSignerFingerprint = expectedSignerFingerprintsByKeyId[activeSigningKeyId]
            val activeSigningKeySources = if (activeSigningKeyId.isEmpty()) {
                emptyMap()
            } else {
                publicKeySourceFilesByKeyId[activeSigningKeyId].orEmpty()
            }

            if (activeSigningKeyId.isEmpty()) {
                addResult(results, "FAIL", "Release artifact signatures", "No active signing key id was available")
            } else if (activeSignerFingerprint == null) {
                addResult(results, "FAIL", "Release artifact signatures", "No expected fingerprint was available for active signing key $activeSigningKeyId")
            } else if (activeSigningKeySources.isEmpty()) {
                addResult(results, "FAIL", "Release artifact signatures", "No public key sources were available for active signing key $activeSigningKeyId")
            } else {
                val signatureVerificationDir = layout.buildDirectory
                    .dir("tmp/verifyReleaseReadiness/signatures")
                    .get()
                    .asFile
                resetTemporaryDirectory(signatureVerificationDir, "release signature verification directory")
                val downloadsDir = File(signatureVerificationDir, "downloads")
                ensureDirectory(downloadsDir, "release signature download directory")

                fun prepareGpgHomeDir(gpgHomeDir: File) {
                    resetTemporaryDirectory(gpgHomeDir, "release signature GPG home directory")
                    gpgHomeDir.setReadable(false, false)
                    gpgHomeDir.setWritable(false, false)
                    gpgHomeDir.setExecutable(false, false)
                    gpgHomeDir.setReadable(true, true)
                    gpgHomeDir.setWritable(true, true)
                    gpgHomeDir.setExecutable(true, true)
                }

                val downloadedArtifacts = mutableMapOf<String, Pair<File, File>>()
                expectedBisq2SignableReleaseAssets(releaseVersion).forEach { artifactName ->
                    val signatureName = "$artifactName.asc"
                    val artifactUrl = releaseAssetsByName[artifactName]?.get("browser_download_url")?.toString().orEmpty()
                    val signatureUrl = releaseAssetsByName[signatureName]?.get("browser_download_url")?.toString().orEmpty()
                    if (artifactUrl.isEmpty() || signatureUrl.isEmpty()) {
                        addResult(
                            results,
                            "FAIL",
                            "GPG signature $signatureName",
                            "Missing browser_download_url for ${listOfNotNull(
                                artifactName.takeIf { artifactUrl.isEmpty() },
                                signatureName.takeIf { signatureUrl.isEmpty() }
                            ).joinToString(", ")}"
                        )
                        return@forEach
                    }

                    try {
                        val artifactFile = File(downloadsDir, artifactName)
                        val signatureFile = File(downloadsDir, signatureName)
                        httpDownloadToFile(artifactUrl, artifactFile)
                        httpDownloadToFile(signatureUrl, signatureFile)
                        downloadedArtifacts[artifactName] = Pair(artifactFile, signatureFile)
                    } catch (exception: Exception) {
                        addResult(results, "FAIL", "GPG signature $signatureName", exception.message)
                    }
                }

                activeSigningKeySources.forEach { (sourceName, keyFile) ->
                    val gpgHomeDir = File(signatureVerificationDir, "gnupg-${safeFileName(sourceName)}")
                    prepareGpgHomeDir(gpgHomeDir)

                    val importResult = runCommand(
                        listOf(
                            gpgExecutable,
                            "--homedir", gpgHomeDir.absolutePath,
                            "--batch",
                            "--import",
                            keyFile.absolutePath,
                        )
                    )
                    if (importResult.exitValue != 0) {
                        addResult(results, "FAIL", "Release signature keyring $sourceName", importResult.failureDetails())
                    } else {
                        downloadedArtifacts.forEach { (artifactName, files) ->
                            val signatureName = "$artifactName.asc"
                            try {
                                val (artifactFile, signatureFile) = files

                                val verifyResult = runCommand(
                                    gpgAssertSignerVerifyCommand(
                                        gpgExecutable,
                                        listOf(activeSignerFingerprint),
                                        signatureFile,
                                        artifactFile,
                                        listOf("--homedir", gpgHomeDir.absolutePath)
                                    )
                                )
                                if (verifyResult.exitValue != 0) {
                                    addResult(results, "FAIL", "GPG signature $signatureName with $sourceName key", verifyResult.failureDetails())
                                    return@forEach
                                }

                                val signerError = validateGpgVerifiedExpectedSigner(verifyResult, listOf(activeSignerFingerprint))
                                if (signerError == null) {
                                    val validSigFingerprints = parseGpgValidSigFingerprints(verifyResult.stdout)
                                    addResult(
                                        results,
                                        "PASS",
                                        "GPG signature $signatureName with $sourceName key",
                                        "Valid signature from active signer ${validSigFingerprints.intersect(setOf(activeSignerFingerprint)).joinToString()}"
                                    )
                                } else {
                                    addResult(results, "FAIL", "GPG signature $signatureName with $sourceName key", signerError)
                                }
                            } catch (exception: Exception) {
                                addResult(results, "FAIL", "GPG signature $signatureName with $sourceName key", exception.message)
                            }
                        }
                    }
                }
            }
        }

        val reportFile = releaseReadinessReportFile.get().asFile
        reportFile.parentFile.mkdirs()
        val markdown = StringBuilder()
        markdown.append("# Bisq 2 Release Readiness\n\n")
        markdown.append("- Release version: `$releaseVersion`\n")
        markdown.append("- GitHub tag: `$releaseTag`\n")
        markdown.append("- Generated at: `${Instant.now()}`\n\n")
        markdown.append("This report is generated by `./gradlew verifyReleaseReadiness -PreleaseVersion=$releaseVersion`. ")
        markdown.append("The task is read-only: it checks expected release assets, GitHub download URLs, Bisq 2 signer key consistency, and key expiry.\n\n")
        markdown.append("## Summary\n\n")
        markdown.append("| Status | Count |\n")
        markdown.append("| --- | ---: |\n")
        listOf("PASS", "WARN", "FAIL").forEach { status ->
            markdown.append("| $status | ${results.count { it.status == status }} |\n")
        }
        markdown.append("\n## Checks\n\n")
        markdown.append("| Status | Check | Details |\n")
        markdown.append("| --- | --- | --- |\n")
        results.forEach { result ->
            markdown.append("| ${result.status} | ${escapeMarkdownCell(result.name)} | ${escapeMarkdownCell(result.details)} |\n")
        }
        markdown.append('\n')
        reportFile.writeText(markdown.toString(), StandardCharsets.UTF_8)
        logger.lifecycle("Wrote ${rootProject.relativePath(reportFile)}")

        val failures = results.filter { it.status == "FAIL" }
        if (failures.isNotEmpty()) {
            throw GradleException("Release readiness failed with ${failures.size} failure(s). See ${rootProject.relativePath(reportFile)}")
        }

        logger.lifecycle("Release readiness passed with ${results.count { it.status == "WARN" }} warning(s).")
    }
}

tasks.register("verifyGithubReleaseReadiness") {
    group = "verification"
    description = "Compatibility alias for verifyReleaseReadiness."
    dependsOn("verifyReleaseReadiness")
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
    dependsOn("verifyDependencySignaturePolicy")
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
