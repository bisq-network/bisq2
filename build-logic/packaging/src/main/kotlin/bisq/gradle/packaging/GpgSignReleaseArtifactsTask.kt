package bisq.gradle.packaging

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Locale

abstract class GpgSignReleaseArtifactsTask : DefaultTask() {

    @get:Input
    abstract val releaseDirPath: Property<String>

    @get:Input
    abstract val gpgUser: Property<String>

    @get:Input
    abstract val expectedFingerprint: Property<String>

    @get:Input
    abstract val activeSigningKeyIdPath: Property<String>

    @get:Input
    abstract val maintainerPublicKeysDirPath: Property<String>

    @get:Input
    abstract val gpgExecutable: Property<String>

    @get:Input
    abstract val artifactExtensions: ListProperty<String>

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun run() {
        val releaseDir = getReleaseDir()
        val gpgUser = getRequiredValue(gpgUser, "Missing required -PgpgUser=<key-id-email-or-fingerprint>. You can also use -PbisqGpgUser=<key-id-email-or-fingerprint> or BISQ_GPG_USER.")
        val gpgExecutable = gpgExecutable.get()
        val expectedFingerprint = resolveExpectedFingerprint(gpgExecutable)

        val releaseDirFiles = releaseDir.listFiles()
                ?: throw GradleException("Cannot list release directory: $releaseDir. Check that it is readable.")

        val artifacts = releaseDirFiles
                .filter(::isSignableArtifact)
                .sortedBy(File::getName)

        if (artifacts.isEmpty()) {
            throw GradleException("No release artifacts found to sign in $releaseDir")
        }

        logger.lifecycle("Processing ${artifacts.size} release artifact(s) in ${releaseDir.absolutePath}")
        artifacts.forEach { artifact ->
            val signatureFile = File(artifact.parentFile, "${artifact.name}.asc")
            signArtifact(gpgExecutable, gpgUser, artifact, signatureFile)
            verifySignature(gpgExecutable, expectedFingerprint, artifact, signatureFile)
            logger.lifecycle("Signed and verified ${artifact.name} -> ${signatureFile.name}")
        }
    }

    private fun getReleaseDir(): File {
        val rawReleaseDir = getRequiredValue(releaseDirPath, "Missing required -Pbisq.release.dir=<directory> or -PreleaseDir=<directory>.")
        val releaseDir = project.file(rawReleaseDir.trim())
        if (!releaseDir.isDirectory) {
            throw GradleException("Release directory is not a directory: $releaseDir")
        }
        return releaseDir
    }

    private fun getRequiredValue(property: Property<String>, message: String): String {
        val value = property.orNull
        if (value == null || value.trim().isEmpty()) {
            throw GradleException(message)
        }
        return value.trim()
    }

    private fun isSignableArtifact(candidate: File): Boolean {
        if (!candidate.isFile || candidate.name.startsWith(".") || candidate.name.endsWith(".asc")) {
            return false
        }

        val lowerName = candidate.name.lowercase(Locale.ROOT)
        return artifactExtensions.get().any { extension ->
            lowerName.endsWith(".${extension.lowercase(Locale.ROOT)}")
        }
    }

    private fun resolveExpectedFingerprint(gpgExecutable: String): String {
        return expectedFingerprint.orNull
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.let(::normalizeFingerprint)
                ?: deriveExpectedFingerprint(gpgExecutable)
    }

    private fun deriveExpectedFingerprint(gpgExecutable: String): String {
        val activeSigningKeyIdFile = project.file(getRequiredValue(
                activeSigningKeyIdPath,
                "Missing active signing key id marker path."
        ))
        if (!activeSigningKeyIdFile.isFile) {
            throw GradleException(
                    "Active signing key id marker does not exist: $activeSigningKeyIdFile. " +
                            "Set -PgpgFingerprint=<full-fingerprint> to override."
            )
        }

        val activeSigningKeyId = activeSigningKeyIdFile.readText(StandardCharsets.UTF_8)
                .trim()
                .uppercase(Locale.ROOT)
        if (!activeSigningKeyId.matches(Regex("([0-9A-F]{8}|[0-9A-F]{16}|[0-9A-F]{40})"))) {
            throw GradleException(
                    "Active signing key id marker $activeSigningKeyIdFile must contain an 8-, 16-, " +
                            "or 40-character hex key id/fingerprint. Got: ${activeSigningKeyId.ifEmpty { "<blank>" }}"
            )
        }

        val maintainerPublicKeysDir = project.file(getRequiredValue(
                maintainerPublicKeysDirPath,
                "Missing maintainer public keys directory path."
        ))
        val publicKeyFile = File(maintainerPublicKeysDir, "$activeSigningKeyId.asc")
        if (!publicKeyFile.isFile) {
            throw GradleException(
                    "Active signing key id marker $activeSigningKeyIdFile points to $activeSigningKeyId, " +
                            "but the corresponding public key file does not exist: $publicKeyFile. " +
                            "Set -PgpgFingerprint=<full-fingerprint> to override."
            )
        }

        val showKeysResult = execResult(listOf(
                gpgExecutable,
                "--show-keys",
                "--with-colons",
                "--fingerprint",
                publicKeyFile.absolutePath
        ))
        if (showKeysResult.exitValue != 0) {
            throw GradleException(
                    "Failed to read public key metadata from $publicKeyFile: ${showKeysResult.failureDetails()}. " +
                            "Set -PgpgFingerprint=<full-fingerprint> to override."
            )
        }

        val primaryFingerprint = parsePrimaryFingerprint(showKeysResult.stdout)
                ?: throw GradleException("Could not parse primary fingerprint from $publicKeyFile")
        if (!primaryFingerprint.endsWith(activeSigningKeyId)) {
            throw GradleException(
                    "Active signing key id marker $activeSigningKeyIdFile points to $activeSigningKeyId, " +
                            "but $publicKeyFile has primary fingerprint $primaryFingerprint"
            )
        }

        logger.lifecycle(
                "Using expected GPG fingerprint $primaryFingerprint from ${publicKeyFile.name} " +
                        "selected by ${activeSigningKeyIdFile.name}"
        )
        return primaryFingerprint
    }

    private fun signArtifact(gpgExecutable: String,
                             gpgUser: String,
                             artifact: File,
                             signatureFile: File) {
        val signResult = execResult(listOf(
                gpgExecutable,
                "--yes",
                "--digest-algo", "SHA256",
                "--local-user", gpgUser,
                "--output", signatureFile.absolutePath,
                "--detach-sig",
                "--armor",
                artifact.absolutePath
        ))
        if (signResult.exitValue != 0) {
            throw GradleException("Failed to sign ${artifact.name}: ${signResult.failureDetails()}")
        }
        if (!signatureFile.isFile || signatureFile.length() == 0L) {
            throw GradleException("GPG did not create a non-empty signature file for ${artifact.name}: $signatureFile")
        }
    }

    private fun verifySignature(gpgExecutable: String,
                                expectedFingerprint: String,
                                artifact: File,
                                signatureFile: File) {
        val verifyResult = execResult(listOf(
                gpgExecutable,
                "--status-fd", "1",
                "--verify",
                signatureFile.absolutePath,
                artifact.absolutePath
        ))
        if (verifyResult.exitValue != 0) {
            throw GradleException("Failed to verify ${signatureFile.name}: ${verifyResult.failureDetails()}")
        }

        val validSigFingerprints = parseValidSigFingerprints(verifyResult.stdout)
        if (!validSigFingerprints.contains(expectedFingerprint)) {
            throw GradleException(
                    "Signature ${signatureFile.name} was valid, but not from expected fingerprint $expectedFingerprint. " +
                            "VALIDSIG fingerprints: ${validSigFingerprints.joinToString()}"
            )
        }
    }

    private fun parseValidSigFingerprints(statusOutput: String): Set<String> {
        return statusOutput.lineSequence()
                .filter { it.startsWith("[GNUPG:] VALIDSIG ") }
                .flatMap { line ->
                    val tokens = line.split(Regex("\\s+"))
                    // GnuPG emits the signing-key fingerprint first and, for subkey signatures, the primary-key
                    // fingerprint as the last field. Accept either so release operators can pin the primary key.
                    listOfNotNull(tokens.getOrNull(2), tokens.lastOrNull())
                }
                .mapNotNull(::normalizeFingerprintOrNull)
                .toSet()
    }

    private fun parsePrimaryFingerprint(showKeysOutput: String): String? {
        var waitingForPrimaryFingerprint = false
        for (line in showKeysOutput.lineSequence()) {
            val tokens = line.split(":")
            when (tokens.getOrNull(0)) {
                "pub" -> waitingForPrimaryFingerprint = true
                "sub" -> waitingForPrimaryFingerprint = false
                "fpr" -> {
                    if (waitingForPrimaryFingerprint) {
                        return tokens.getOrNull(9)?.let(::normalizeFingerprintOrNull)
                    }
                }
            }
        }
        return null
    }

    private fun normalizeFingerprint(fingerprint: String): String {
        val normalizedFingerprint = fingerprint.replace(Regex("\\s+"), "")
                .uppercase(Locale.ROOT)
        if (!normalizedFingerprint.matches(Regex("[0-9A-F]{40}"))) {
            throw GradleException("Expected a full 40-character GPG fingerprint, got: $fingerprint")
        }
        return normalizedFingerprint
    }

    private fun normalizeFingerprintOrNull(fingerprint: String): String? {
        val normalizedFingerprint = fingerprint.replace(Regex("\\s+"), "")
                .uppercase(Locale.ROOT)
        return if (normalizedFingerprint.matches(Regex("[0-9A-F]{40}"))) {
            normalizedFingerprint
        } else {
            null
        }
    }

    private fun execResult(commandLine: List<String>): ExecResult {
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()
        val result = project.exec {
            commandLine(commandLine)
            standardOutput = stdout
            errorOutput = stderr
            isIgnoreExitValue = true
        }
        return ExecResult(
                result.exitValue,
                stdout.toString(Charsets.UTF_8.name()),
                stderr.toString(Charsets.UTF_8.name())
        )
    }

    private data class ExecResult(val exitValue: Int,
                                  val stdout: String,
                                  val stderr: String) {
        fun failureDetails(): String =
                listOf(stderr, stdout)
                        .filter { it.trim().isNotEmpty() }
                        .joinToString("\n")
                        .trim()
    }
}
