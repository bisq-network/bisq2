package bisq.gradle.tasks.download

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.FileOutputStream
import java.net.URI
import java.nio.channels.Channels

@CacheableTask
abstract class DownloadTask : DefaultTask() {

    @get:Input
    abstract val downloadUrl: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun download() {
        downloadFile()
    }

    private fun downloadFile() {
        val url = URI.create(downloadUrl.get()).toURL();
        url.openStream().use { inputStream ->
            Channels.newChannel(inputStream).use { readableByteChannel ->
                println("Downloading: $url")

                FileOutputStream(outputFile.get().asFile).use { fileOutputStream ->
                    fileOutputStream.channel
                        .transferFrom(readableByteChannel, 0, Long.MAX_VALUE)
                }
            }
        }
    }
}