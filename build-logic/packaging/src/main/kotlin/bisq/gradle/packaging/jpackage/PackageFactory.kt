package bisq.gradle.packaging.jpackage

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Year
import java.util.concurrent.TimeUnit


class PackageFactory(private val jPackagePath: Path, private val jPackageConfig: JPackageConfig) {

    fun createPackages() {
        val jPackageCommonArgs: Map<String, String> = createCommonArguments(jPackageConfig.appConfig)

        val packageFormatConfigs = jPackageConfig.packageFormatConfigs
        val perPackageCommand = packageFormatConfigs.packageFormats
                .map { Pair(it.fileExtension, packageFormatConfigs.createArgumentsForJPackage(it) + listOf("--type", it.fileExtension)) }

        val absoluteBinaryPath = jPackagePath.toAbsolutePath().toString()
        perPackageCommand.forEach { filetypeAndCustomCommands ->
            val processBuilder = ProcessBuilder(absoluteBinaryPath)
                    .inheritIO()

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
            allCommands.add(jPackageTempPath.toAbsolutePath().toString(),)

            allCommands.addAll(filetypeAndCustomCommands.second)

            val process: Process = processBuilder.start()
            process.waitFor(15, TimeUnit.MINUTES)
        }
    }

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
            mutableMapOf("--description" to "Bisq 2")
        } else {
            emptyMap()
        }

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
