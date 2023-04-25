package bisq.gradle.electrum

import bisq.gradle.tasks.DownloadTask
import bisq.gradle.tasks.signature.SignatureVerificationTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import java.net.URL

class ElectrumBinaryDownloader(
    private val project: Project,
    private val pluginExtension: BisqElectrumPluginExtension
) {

    companion object {
        private const val DOWNLOADS_DIR = "${BisqElectrumPlugin.DATA_DIR}/downloads"
    }

    lateinit var verifyDownloadTask: TaskProvider<SignatureVerificationTask>

    fun registerTasks() {
        val binaryDownloadTask: TaskProvider<DownloadTask> = registerBinaryDownloadTask()
        val signatureDownloadTask: TaskProvider<DownloadTask> = registerSignatureDownloadTask()

        verifyDownloadTask = project.tasks.register<SignatureVerificationTask>("verifyElectrumBinary") {
            dependsOn(binaryDownloadTask, signatureDownloadTask)

            fileToVerify.set(binaryDownloadTask.flatMap { it.outputFile })
            detachedSignatureFile.set(signatureDownloadTask.flatMap { it.outputFile })
            publicKeyUrls.set(getPublicKeyUrls())
            publicKeyFingerprints.set(getPublicKeyFingerprints())

            resultFile.set(project.layout.buildDirectory.file("$DOWNLOADS_DIR/sha256.result"))
        }
    }

    private fun registerBinaryDownloadTask(): TaskProvider<DownloadTask> {
        val binaryUrl: Provider<URL> = getBinaryDownloadUrl()
        val binaryFileName: Provider<String> = binaryUrl.map { it.file }
        val binaryOutputFile: Provider<RegularFile> =
            project.layout.buildDirectory.file("$DOWNLOADS_DIR/$binaryFileName")
        return project.tasks.register<DownloadTask>("downloadElectrumBinary") {
            downloadUrl.set(binaryUrl)
            outputFile.set(binaryOutputFile)
        }
    }

    private fun registerSignatureDownloadTask(): TaskProvider<DownloadTask> {
        val signatureUrl: Provider<URL> = getSignatureDownloadUrl()
        val signatureFileName: Provider<String> = signatureUrl.map { it.file }
        val signatureOutputFile: Provider<RegularFile> =
            project.layout.buildDirectory.file("$DOWNLOADS_DIR/$signatureFileName")
        return project.tasks.register<DownloadTask>("downloadElectrumSignature") {
            downloadUrl.set(signatureUrl)
            outputFile.set(signatureOutputFile)
        }
    }

    private fun getBinaryDownloadUrl(): Provider<URL> =
        pluginExtension.version.map { ElectrumBinaryUrlProvider(it).url }

    private fun getSignatureDownloadUrl(): Provider<URL> = getBinaryDownloadUrl().map { URL("$it.asc") }

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