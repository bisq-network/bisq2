package bisq.gradle.packaging

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.concurrent.TimeUnit

abstract class JLinkTask : DefaultTask() {

    @get:InputDirectory
    abstract val jdkDirectory: DirectoryProperty

    @get:Input
    abstract val modules: SetProperty<String>

    @get:Input
    abstract val options: ListProperty<String>

    @get:Input
    abstract val excludedNativeCommands: SetProperty<String>

    @get:Input
    abstract val requireJavaLauncher: Property<Boolean>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun run() {
        val moduleNames = modules.get()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSortedSet()
        check(moduleNames.isNotEmpty()) { "At least one module must be configured for jlink." }

        val outputDirectoryFile = outputDirectory.asFile.get()
        // jlink expects a non-existent output directory.
        outputDirectoryFile.deleteRecursively()

        val jLinkPath = jdkDirectory.asFile.get().toPath().resolve("bin").resolve("jlink")
        val command = mutableListOf(
                jLinkPath.toAbsolutePath().toString(),
                "--add-modules", moduleNames.joinToString(","),
        )
        command.addAll(options.get())
        command.addAll(listOf("--output", outputDirectoryFile.absolutePath))

        val processBuilder = ProcessBuilder(command)
        processBuilder.inheritIO()

        val process = processBuilder.start()
        val finished = process.waitFor(2, TimeUnit.MINUTES)
        if (!finished) {
            process.destroyForcibly()
            throw IllegalStateException("jlink did not finish within 2 minutes.")
        }

        val isSuccess = process.exitValue() == 0
        if (!isSuccess) {
            throw IllegalStateException("jlink couldn't create custom runtime.")
        }

        deleteExcludedNativeCommands(outputDirectoryFile)

        if (requireJavaLauncher.get()) {
            val javaExecutableName = if (getOS() == OS.WINDOWS) "java.exe" else "java"
            val javaExecutable = outputDirectoryFile.toPath().resolve("bin").resolve(javaExecutableName).toFile()
            check(javaExecutable.isFile) {
                "The generated runtime image does not contain bin/$javaExecutableName. " +
                        "Do not use --strip-native-commands while Bisq starts child JVM processes."
            }
        }
    }

    private fun deleteExcludedNativeCommands(outputDirectoryFile: File) {
        val excludedCommands = excludedNativeCommands.get()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        if (excludedCommands.isEmpty()) {
            return
        }

        val binDirectory = outputDirectoryFile.toPath().resolve("bin").toFile()
        excludedCommands
                .map { binDirectory.resolve(it) }
                .filter { it.isFile }
                .forEach {
                    check(it.delete()) { "Could not remove excluded runtime command: ${it.absolutePath}" }
                }
    }
}
