package bisq.gradle.tasks.download

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register

class DownloadTaskFactory(
    private val project: Project, private val downloadDirectoryPath: String
) {
    fun registerDownloadTask(taskName: String, url: Provider<String>): TaskProvider<DownloadTask> {
        val outputFileProvider: Provider<RegularFile> = url.flatMap {
            // url.file:
            // https://example.org/1.2.3/binary.exe -> 1.2.3/binary.exe
            val fileName = it.split("/").last()
            project.layout.buildDirectory.file("$downloadDirectoryPath/$fileName")
        }
        return project.tasks.register<DownloadTask>(taskName) {
            downloadUrl.set(url)
            outputFile.set(outputFileProvider)
        }
    }
}