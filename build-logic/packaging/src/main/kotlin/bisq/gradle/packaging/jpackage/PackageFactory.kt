package bisq.gradle.packaging.jpackage

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Year
import java.util.concurrent.TimeUnit


class PackageFactory(private val jPackagePath: Path, private val jPackageConfig: JPackageConfig) {

    fun createPackages() {
        val jPackageTempPath = jPackageConfig.outputDirPath.parent.resolve("temp")
        deleteFileOrDirectory(jPackageTempPath.toFile())

        val jPackageCommonArgs: List<String> = createCommonArguments(jPackageConfig.appConfig, jPackageTempPath)

        val packageFormatConfigs = jPackageConfig.packageFormatConfigs
        val perPackageCommand = packageFormatConfigs.packageFormats
                .map { packageFormatConfigs.createArgumentsForJPackage(it) + listOf("--type", it.fileExtension) }

        val absoluteBinaryPath = jPackagePath.toAbsolutePath().toString()
        perPackageCommand.forEach { customCommands ->
            val processBuilder = ProcessBuilder(absoluteBinaryPath)
                    .inheritIO()

            val allCommands = processBuilder.command()
            allCommands.addAll(jPackageCommonArgs)
            allCommands.addAll(customCommands)

            val process: Process = processBuilder.start()
            process.waitFor(15, TimeUnit.MINUTES)
        }
    }

    private fun createCommonArguments(appConfig: JPackageAppConfig, tempDir: Path): List<String> =
            mutableListOf(
                    "--dest", jPackageConfig.outputDirPath.toAbsolutePath().toString(),

                    "--name", "Bisq 2",
                    "--description", "A decentralized bitcoin exchange network.",
                    "--copyright", "Copyright © 2013-${Year.now()} - The Bisq developers",
                    "--vendor", "Bisq",
                    "--license-file", appConfig.licenceFilePath,
                    "--app-version", appConfig.appVersion,

                    "--temp", tempDir.toAbsolutePath().toString(),
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
