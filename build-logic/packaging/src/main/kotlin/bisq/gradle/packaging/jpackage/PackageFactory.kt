package bisq.gradle.packaging.jpackage

import org.gradle.api.GradleException
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Year
import java.util.concurrent.TimeUnit


class PackageFactory(private val jPackagePath: Path, private val jPackageConfig: JPackageConfig) {

    companion object {
        private const val JPACKAGE_TIMEOUT_MINUTES = 15L
    }

    fun createPackages() {
        val jPackageCommonArgs: Map<String, String> = createCommonArguments(jPackageConfig.appConfig)

        val packageFormatConfigs = jPackageConfig.packageFormatConfigs
        val perPackageCommand = packageFormatConfigs.packageFormats
                .map { Pair(it.fileExtension, packageFormatConfigs.createArgumentsForJPackage(it) + listOf("--type", it.fileExtension)) }

        // A package of an earlier run must not pass for the result of this run
        perPackageCommand.forEach { deletePackageFiles(it.first) }

        val absoluteBinaryPath = jPackagePath.toAbsolutePath().toString()
        perPackageCommand.forEach { filetypeAndCustomCommands ->
            // The standard output of the Gradle daemon goes nowhere, so jpackage writes into a file of the build
            val logFile = jPackageConfig.outputDirPath.parent.resolve("jpackage_${filetypeAndCustomCommands.first}.log")
                    .toFile()
            val processBuilder = ProcessBuilder(absoluteBinaryPath)
                    .redirectErrorStream(true)
                    .redirectOutput(logFile)

            var commonArgs: Map<String, String> = jPackageCommonArgs

            val osSpecificOverrideArgs = getOsSpecificOverrideArgs(filetypeAndCustomCommands.first)
            if (osSpecificOverrideArgs.isNotEmpty()) {
                val mutableMap: MutableMap<String, String> = jPackageCommonArgs.toMutableMap()
                mutableMap.putAll(osSpecificOverrideArgs)
                commonArgs = mutableMap
            }

            val allCommands = processBuilder.command()
            commonArgs.forEach { (key, value) ->
                allCommands.add(key)
                allCommands.add(value)
            }

            val jPackageTempPath = jPackageConfig.outputDirPath.parent.resolve("temp_${filetypeAndCustomCommands.first}")
            deleteFileOrDirectory(jPackageTempPath.toFile())
            allCommands.add("--temp")
            allCommands.add(jPackageTempPath.toAbsolutePath().toString())

            allCommands.addAll(filetypeAndCustomCommands.second)

            val process: Process = processBuilder.start()
            waitForSuccess(process, filetypeAndCustomCommands.first, allCommands, logFile)
            checkPackageFileCreated(filetypeAndCustomCommands.first, logFile)
        }
    }

    private fun waitForSuccess(process: Process, fileExtension: String, command: List<String>, logFile: File) {
        val hasFinished = process.waitFor(JPACKAGE_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        if (!hasFinished) {
            process.destroyForcibly()
            throw GradleException("jpackage did not create the $fileExtension package within " +
                    "$JPACKAGE_TIMEOUT_MINUTES minutes. Its output is in $logFile. Command: ${command.joinToString(" ")}")
        }

        val exitCode = process.exitValue()
        if (exitCode != 0) {
            throw GradleException("jpackage failed with exit code $exitCode for the $fileExtension package. Its " +
                    "output is in $logFile. Command: ${command.joinToString(" ")}")
        }
    }

    private fun checkPackageFileCreated(fileExtension: String, logFile: File) {
        val packageFiles = findPackageFiles(fileExtension)
        if (packageFiles.size != 1) {
            throw GradleException("jpackage exited with 0, but ${jPackageConfig.outputDirPath} contains " +
                    "${packageFiles.size} $fileExtension packages instead of one: ${packageFiles.joinToString(", ")}. " +
                    "Its output is in $logFile.")
        }
    }

    private fun deletePackageFiles(fileExtension: String) {
        findPackageFiles(fileExtension).forEach { packageFile ->
            if (!packageFile.delete()) {
                throw GradleException("Couldn't delete the package of an earlier run: ${packageFile.absolutePath}")
            }
        }
    }

    private fun findPackageFiles(fileExtension: String): List<File> =
            jPackageConfig.outputDirPath.toFile()
                    .listFiles { file -> file.isFile && file.name.endsWith(".$fileExtension") }
                    ?.toList()
                    ?: emptyList()

    private fun createCommonArguments(appConfig: JPackageAppConfig): Map<String, String> =
            mutableMapOf(
                    "--dest" to jPackageConfig.outputDirPath.toAbsolutePath().toString(),

                    "--name" to appConfig.name,
                    "--description" to "A decentralized bitcoin exchange network.",
                    "--copyright" to "Copyright © 2013-${Year.now()} - The Bisq developers",
                    "--vendor" to "Bisq",
                    "--license-file" to appConfig.licenceFilePath,
                    "--app-version" to appConfig.appVersion,

                    "--input" to jPackageConfig.inputDirPath.toAbsolutePath().toString(),
                    "--main-jar" to appConfig.mainJarFileName,

                    "--main-class" to appConfig.mainClassName,
                    "--java-options" to appConfig.jvmArgs.joinToString(separator = " "),

                    "--runtime-image" to jPackageConfig.runtimeImageDirPath.toAbsolutePath().toString()
            )

    private fun getOsSpecificOverrideArgs(fileType: String): Map<String, String> =
            if (jPackageConfig.appConfig.name == "Bisq" && fileType == "exe") {
                // Needed for Windows OS notification support
                mutableMapOf("--description" to "Bisq2")
            } else {
                emptyMap()
            }

    private fun deleteFileOrDirectory(dir: File) {
        if (dir.exists()) {
            Files.walk(dir.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete)
        }
    }
}
