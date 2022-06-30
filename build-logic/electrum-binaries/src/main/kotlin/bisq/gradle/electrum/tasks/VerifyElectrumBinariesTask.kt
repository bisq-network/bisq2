package bisq.gradle.electrum.tasks

import bisq.gradle.electrum.SignatureVerifier
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction


abstract class VerifyElectrumBinariesTask : DefaultTask() {

    @get:Input
    abstract val electrumVersion: Property<String>

    @get:InputDirectory
    abstract val inputDirectory: DirectoryProperty

    private val signatureVerifier = SignatureVerifier(
        allPublicKeyUrls = getPublicKeyUrls(),
        publicKeyFingerprints = getPublicKeyFingerprints()
    )

    @TaskAction
    fun run() {
        val binaryNames: Set<String> = DownloadElectrumBinariesTask.getBinaryNames(electrumVersion.get())
        val downloadsDirectory = inputDirectory.asFile.get()

        for (filename in binaryNames) {
            println("Verifying: $filename")
            val isSignatureValid = signatureVerifier.verifySignature(
                signatureFile = downloadsDirectory.resolve("$filename.asc"),
                fileToVerify = downloadsDirectory.resolve(filename)
            )

            if (!isSignatureValid) {
                throw GradleException("Signature verification failed for $filename.")
            }
        }
    }

    private fun getPublicKeyUrls() =
        setOf(
            this::class.java.getResource("/ThomasV.asc")!!,

            // Why failing signature check?
            // this::class.java.getResource("/Emzy.asc")!!

            this::class.java.getResource("/SomberNight.asc")!!
        )

    private fun getPublicKeyFingerprints(): Set<String> {
        val fingerprints = setOf(
            // ThomasV
            "6694 D8DE 7BE8 EE56 31BE  D950 2BD5 824B 7F94 70E6",
            // Emzy
            "9EDA FF80 E080 6596 04F4  A76B 2EBB 056F D847 F8A7",
            // SomberNight
            "0EED CFD5 CAFB 4590 6734  9B23 CA9E EEC4 3DF9 11DC"
        )

        return fingerprints.map { fingerprint ->
            fingerprint.filterNot { it.isWhitespace() }  // Remove all spaces
                .toLowerCase()
        }.toSet()
    }

}