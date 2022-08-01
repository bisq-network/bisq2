package bisq.gradle.electrum.tasks

import bisq.gradle.electrum.ElectrumBinaryHashes
import bisq.gradle.electrum.Sha256
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels

abstract class DownloadElectrumBinariesTask : DefaultTask() {

    companion object {
        private const val ELECTRUM_WEBSITE_URL = "https://download.electrum.org"

        fun getBinaryNames(electrumVersion: String): Set<String> {
            return setOf(
                "electrum-$electrumVersion.dmg",
                "electrum-$electrumVersion-portable.exe",
                "electrum-$electrumVersion-x86_64.AppImage"
            )
        }
    }

    @get:Input
    abstract val electrumVersion: Property<String>

    @get:Nested
    abstract val binaryHashes: ElectrumBinaryHashes

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun run() {
        val binaryUrls: Set<String> = getBinaryDownloadUrls(electrumVersion.get())
        for (url in binaryUrls) {
            if (isFileAlreadyPresent(url)) {
                continue
            }

            downloadFile(url)
            downloadFile("$url.asc")
        }
    }

    private fun getBinaryDownloadUrls(electrumVersion: String): Set<String> {
        val urlPrefix = "$ELECTRUM_WEBSITE_URL/${electrumVersion}"
        return getBinaryNames(electrumVersion)
            .map { filename -> "$urlPrefix/$filename" }
            .toSet()
    }

    private fun isFileAlreadyPresent(url: String): Boolean {
        val outputFile: File = getOutputFileForUrl(url)
        if (outputFile.exists()) {
            val hash: String = Sha256.hashFile(outputFile)
            val expectedHash = binaryHashes.getHashPropertyForBinary(outputFile).get()
            return hash == expectedHash
        }
        return false
    }

    private fun downloadFile(url: String) {
        URL(url).openStream().use { inputStream ->
            Channels.newChannel(inputStream).use { readableByteChannel ->
                println("Downloading: $url")

                val outputFilePath: File = getOutputFileForUrl(url)
                FileOutputStream(outputFilePath).use { fileOutputStream ->
                    fileOutputStream.channel
                        .transferFrom(readableByteChannel, 0, Long.MAX_VALUE)
                }
            }
        }
    }

    private fun getOutputFileForUrl(url: String): File {
        val outputDir: File = outputDir.get().asFile
        val outputFileName = url.split("/").last()
        return outputDir.resolve(outputFileName)
    }
}