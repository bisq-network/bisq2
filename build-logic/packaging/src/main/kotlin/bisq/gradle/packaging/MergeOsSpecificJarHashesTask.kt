package bisq.gradle.packaging

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class MergeOsSpecificJarHashesTask : DefaultTask() {

    @get:InputFiles
    abstract val hashFiles: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun run() {
        val outputFile: File = outputFile.get().asFile
        val bufferedWriter = outputFile.bufferedWriter()

        for (file: File in hashFiles.files) {
            val linePrefix = getLinePrefix(file.name)
            for (line in file.readLines()) {
                bufferedWriter.write("$linePrefix:$line\n")
            }
        }

        bufferedWriter.close()
    }

    private fun getLinePrefix(fileName: String): String =
        when {
            fileName.contains("mac") -> "macOS"
            fileName.contains("linux") -> "linux"
            fileName.contains("windows") -> "windows"
            else -> throw IllegalStateException("Unknown hash file: $fileName")
        }
}