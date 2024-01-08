package bisq.gradle.packaging.jpackage

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Year
import java.util.concurrent.TimeUnit


class PackageFactory(private val jPackagePath: Path, private val jPackageConfig: JPackageConfig) {

    fun createPackages() {
        val jPackageCommonArgs: List<String> = createCommonArguments(jPackageConfig.appConfig)

        val packageFormatConfigs = jPackageConfig.packageFormatConfigs
        val perPackageCommand = packageFormatConfigs.packageFormats
                .map { Pair(it.fileExtension, packageFormatConfigs.createArgumentsForJPackage(it) + listOf("--type", it.fileExtension)) }

        val absoluteBinaryPath = jPackagePath.toAbsolutePath().toString()
        perPackageCommand.forEach { filetypeAndCustomCommands ->
            val processBuilder = ProcessBuilder(absoluteBinaryPath)
                    .inheritIO()

            val allCommands = processBuilder.command()
            allCommands.addAll(jPackageCommonArgs)

            val jPackageTempPath = jPackageConfig.outputDirPath.parent.resolve("temp_${filetypeAndCustomCommands.first}")
            deleteFileOrDirectory(jPackageTempPath.toFile())
            allCommands.add("--temp")
            allCommands.add(jPackageTempPath.toAbsolutePath().toString(),)

            allCommands.addAll(filetypeAndCustomCommands.second)

            val process: Process = processBuilder.start()
            process.waitFor(15, TimeUnit.MINUTES)
        }
    }

    private fun createCommonArguments(appConfig: JPackageAppConfig): List<String> =
            mutableListOf(
                    "--dest", jPackageConfig.outputDirPath.toAbsolutePath().toString(),

                    "--name", "Bisq 2",
                    "--copyright", "Copyright © 2013-${Year.now()} - The Bisq developers",
                    "--vendor", "Bisq",
                    "--license-file", appConfig.licenceFilePath,
                    "--app-version", appConfig.appVersion,

                    "--input", jPackageConfig.inputDirPath.toAbsolutePath().toString(),
                    "--main-jar", appConfig.mainJarFileName,

                    "--main-class", appConfig.mainClassName,
                    "--java-options", appConfig.jvmArgs.joinToString(separator = " "),

                    "--runtime-image", jPackageConfig.runtimeImageDirPath.toAbsolutePath().toString()
            )

    private fun deleteFileOrDirectory(dir: File) {
        val files = dir.listFiles()
        if (files != null) {
            for (file in files) {
                deleteFileOrDirectory(file)
            }
        }
        if (dir.exists()) {
            Files.delete(dir.toPath())
        }
    }
}
