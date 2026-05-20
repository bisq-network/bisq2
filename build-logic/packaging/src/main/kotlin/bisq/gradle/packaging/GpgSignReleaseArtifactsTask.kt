package bisq.gradle.packaging

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale

abstract class GpgSignReleaseArtifactsTask : DefaultTask() {

    @get:Input
    abstract val releaseDirPath: Property<String>

    @get:Input
    abstract val gpgUser: Property<String>

    @get:Input
    abstract val expectedFingerprint: Property<String>

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
        val expectedFingerprint = expectedFingerprint.orNull
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.let(::normalizeFingerprint)

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
            signArtifact(gpgExecutable.get(), gpgUser, artifact, signatureFile)
            verifySignature(gpgExecutable.get(), expectedFingerprint, artifact, signatureFile)
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
                                expectedFingerprint: String?,
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

        if (expectedFingerprint == null) {
            return
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
