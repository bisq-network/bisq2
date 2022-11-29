package bisq.gradle.electrum

import bisq.gradle.electrum.tasks.DownloadTask
import bisq.gradle.electrum.tasks.FileVerificationTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register

class ElectrumBinaryDownloader(
    private val project: Project,
    private val pluginExtension: BisqElectrumPluginExtension
) {

    companion object {
        private const val ELECTRUM_WEBSITE_URL = "https://download.electrum.org"
        private const val DOWNLOADS_DIR = "${BisqElectrumPlugin.DATA_DIR}/downloads"
    }

    private val taskDownloadDirectory: Provider<Directory> = project.layout.buildDirectory.dir(DOWNLOADS_DIR)

    val binaryFile: RegularFile
        get() = taskDownloadDirectory.get().file(getBinaryFileName())

    lateinit var lastTask: TaskProvider<out Task>

    fun registerTasks() {
        val binaryDownloadTask: TaskProvider<DownloadTask> =
            project.tasks.register<DownloadTask>("downloadElectrumBinary") {
                downloadUrl.set(getBinaryDownloadUrl())
                sha256hash.set(getBinaryHash())
                downloadDirectory.set(taskDownloadDirectory)
            }

        val signatureDownloadTask: TaskProvider<DownloadTask> =
            project.tasks.register<DownloadTask>("downloadElectrumSignature") {
                downloadUrl.set(getSignatureDownloadUrl())
                downloadDirectory.set(taskDownloadDirectory)
            }

        lastTask = project.tasks.register<FileVerificationTask>("verifyElectrumBinary") {
            dependsOn(binaryDownloadTask, signatureDownloadTask)

            fileToVerify.set(binaryDownloadTask.flatMap { it.outputFile })
            detachedSignatureFile.set(signatureDownloadTask.flatMap { it.outputFile })
            publicKeyUrls.set(getPublicKeyUrls())
            publicKeyFingerprints.set(getPublicKeyFingerprints())
        }
    }

    private fun getBinaryFileName(): String {
        val electrumVersion: String = pluginExtension.version.get()
        return when (getOS()) {
            OS.LINUX -> "electrum-$electrumVersion-x86_64.AppImage"
            OS.MAC_OS -> "electrum-$electrumVersion.dmg"
            OS.WINDOWS -> "electrum-$electrumVersion.exe"
        }
    }

    private fun getBinaryDownloadUrl(): String = "$ELECTRUM_WEBSITE_URL/${pluginExtension.version.get()}/" + getBinaryFileName()

    private fun getSignatureDownloadUrl(): String = "${getBinaryDownloadUrl()}.asc"

    private fun getBinaryHash(): Property<String> {
        return when (getOS()) {
            OS.LINUX -> pluginExtension.appImageHash
            OS.MAC_OS -> pluginExtension.dmgHash
            OS.WINDOWS -> pluginExtension.exeHash
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