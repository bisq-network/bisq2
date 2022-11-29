package bisq.gradle.electrum.tasks

import bisq.gradle.electrum.Sha256
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels

abstract class DownloadTask : DefaultTask() {

    @get:Input
    abstract val downloadUrl: Property<String>

    @get:Input
    @get:Optional
    abstract val sha256hash: Property<String>

    @get:OutputDirectory
    abstract val downloadDirectory: DirectoryProperty

    @get:OutputFile
    val outputFile: Provider<RegularFile>
        get() = downloadDirectory.file(fileName)

    private val fileName: String
        get() = downloadUrl.get().split("/").last()

    @TaskAction
    fun download() {
        if (isFileAlreadyPresent()) {
            return
        }
        downloadFile()
    }

    private fun isFileAlreadyPresent(): Boolean {
        if (outputFile.get().asFile.exists()) {
            if (sha256hash.isPresent) {
                return verifySha256Hash()
            }
            return true
        }
        return false
    }

    private fun downloadFile() {
        val url = downloadUrl.get()
        URL(url).openStream().use { inputStream ->
            Channels.newChannel(inputStream).use { readableByteChannel ->
                println("Downloading: $url")

                FileOutputStream(outputFile.get().asFile).use { fileOutputStream ->
                    fileOutputStream.channel
                        .transferFrom(readableByteChannel, 0, Long.MAX_VALUE)
                }
            }
        }
    }

    private fun verifySha256Hash(): Boolean {
        val hash: String = Sha256.hashFile(outputFile.get().asFile)
        val expectedHash = sha256hash.get()
        return hash == expectedHash
    }
}