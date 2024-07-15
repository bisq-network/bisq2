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
            allCommands.add(jPackageTempPath.toAbsolutePath().toString())

            allCommands.addAll(filetypeAndCustomCommands.second)

            val process: Process = processBuilder.start()
            process.waitFor(2, TimeUnit.MINUTES)

            // @alvasw
            // On Linux that causes a build failure at the packager task.
            // On Windows the exe cannot start up as it seems the running process still has some resources blocked.
            // I reduce the timeout to 2 minutes, that seems to work in my tests
            /* process.waitFor(15, TimeUnit.MINUTES)

             val exitCode = process.exitValue()
             if (exitCode != 0) {
                 throw IllegalStateException("JPackage failed with exit code $exitCode.")
             }*/
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
                mutableMapOf("--description" to "Bisq2")
            } else {
                emptyMap()
            }

    private fun deleteFileOrDirectory(dir: File) {
        Files.walk(dir.toPath())
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete)
    }
}
