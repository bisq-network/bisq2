package bisq.gradle.packaging.jpackage

import bisq.gradle.packaging.OS
import bisq.gradle.packaging.getOS
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import java.time.Year
import java.util.concurrent.TimeUnit


class PackageFactory(private val jPackagePath: Path, private val jPackageConfig: JPackageConfig) {

    companion object {
        private val FILE_PERMISSIONS: Set<PosixFilePermission> = PosixFilePermissions.fromString("rw-r--r--")
        private val EXECUTABLE_PERMISSIONS: Set<PosixFilePermission> = PosixFilePermissions.fromString("rwxr-xr-x")
        private val EXECUTE_PERMISSIONS: Set<PosixFilePermission> = setOf(
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_EXECUTE
        )
    }

    fun createPackages() {
        // jpackage takes the file modes of --input and --runtime-image over into the app image as they
        // are and creates the directories of the app image with the umask of the build session. The RPM
        // spec it generates copies that tree as it is, so a release built with a umask of 027 ships
        // /opt/bisq2 with 750 directories and 640 files and is unusable for every user except the one
        // who ran the installation (#4394, #3895).
        // On Linux we therefore build the app image once, normalize its file modes and create both
        // packages from it. That also saves the second app image build the loop below did before.
        val appImagePath: Path? = if (getOS() == OS.LINUX) createNormalizedAppImage() else null

        val packageFormatConfigs = jPackageConfig.packageFormatConfigs
        val perPackageCommand = packageFormatConfigs.packageFormats
                .map { Pair(it.fileExtension, packageFormatConfigs.createArgumentsForJPackage(it) + listOf("--type", it.fileExtension)) }

        perPackageCommand.forEach { filetypeAndCustomCommands ->
            val commonArgs: Map<String, String> =
                    if (appImagePath == null) {
                        createAppImageArguments(jPackageConfig.appConfig) +
                                createPackageArguments(jPackageConfig.appConfig) +
                                getOsSpecificOverrideArgs(filetypeAndCustomCommands.first)
                    } else {
                        createPackageArguments(jPackageConfig.appConfig) +
                                mapOf("--app-image" to appImagePath.toAbsolutePath().toString())
                    }

            val arguments = mutableListOf<String>()
            commonArgs.forEach { (key, value) ->
                arguments.add(key)
                arguments.add(value)
            }

            val jPackageTempPath = jPackageConfig.outputDirPath.parent.resolve("temp_${filetypeAndCustomCommands.first}")
            deleteFileOrDirectory(jPackageTempPath.toFile())
            arguments.add("--temp")
            arguments.add(jPackageTempPath.toAbsolutePath().toString())

            arguments.addAll(filetypeAndCustomCommands.second)

            runJPackage(arguments)
        }
    }

    private fun createNormalizedAppImage(): Path {
        val appImageDirPath = jPackageConfig.outputDirPath.parent.resolve("app_image")
        val tempPath = jPackageConfig.outputDirPath.parent.resolve("temp_app_image")
        deleteFileOrDirectory(appImageDirPath.toFile())
        deleteFileOrDirectory(tempPath.toFile())

        val arguments = mutableListOf("--type", "app-image")
        createAppImageArguments(jPackageConfig.appConfig).forEach { (key, value) ->
            arguments.add(key)
            arguments.add(value)
        }
        arguments.add("--dest")
        arguments.add(appImageDirPath.toAbsolutePath().toString())
        arguments.add("--temp")
        arguments.add(tempPath.toAbsolutePath().toString())
        arguments.addAll(jPackageConfig.packageFormatConfigs.appImageArguments)

        // The packages are created from this image, so unlike for the packages themselves we have to
        // be sure that jpackage is done with it and did not fail.
        val process = runJPackage(arguments)
        if (!process.waitFor(15, TimeUnit.MINUTES) || process.exitValue() != 0) {
            throw IllegalStateException("JPackage failed to create the app image.")
        }

        val appImagePath = appImageDirPath.resolve(jPackageConfig.appConfig.name)
        normalizePermissions(appImagePath)
        return appImagePath
    }

    private fun normalizePermissions(path: Path) {
        Files.walk(path).use { paths ->
            paths.forEach {
                if (Files.isSymbolicLink(it)) {
                    return@forEach
                }
                val isExecutable = Files.isDirectory(it) ||
                        Files.getPosixFilePermissions(it).any(EXECUTE_PERMISSIONS::contains)
                Files.setPosixFilePermissions(it, if (isExecutable) EXECUTABLE_PERMISSIONS else FILE_PERMISSIONS)
            }
        }
    }

    private fun runJPackage(arguments: List<String>): Process {
        val jPackageBinaryPath = jPackagePath.toAbsolutePath().toString()
        // Depending on the JDK, jpackage either takes the file modes of the app image over into the
        // package tree or re-creates the files there with the umask of the build session, so the umask
        // has to be pinned on top of normalizing the app image.
        val commandLine =
                if (getOS() == OS.LINUX) {
                    listOf("sh", "-c", "umask 022; exec \"\$@\"", "sh", jPackageBinaryPath) + arguments
                } else {
                    listOf(jPackageBinaryPath) + arguments
                }

        val processBuilder = ProcessBuilder(commandLine)
                .inheritIO()

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
        return process
    }

    private fun createAppImageArguments(appConfig: JPackageAppConfig): Map<String, String> =
            mapOf(
                    "--name" to appConfig.name,
                    "--description" to "A decentralized bitcoin exchange network.",
                    "--copyright" to "Copyright © 2013-${Year.now()} - The Bisq developers",
                    "--vendor" to "Bisq",
                    "--app-version" to appConfig.appVersion,

                    "--input" to jPackageConfig.inputDirPath.toAbsolutePath().toString(),
                    "--main-jar" to appConfig.mainJarFileName,

                    "--main-class" to appConfig.mainClassName,
                    "--java-options" to appConfig.jvmArgs.joinToString(separator = " "),

                    "--runtime-image" to jPackageConfig.runtimeImageDirPath.toAbsolutePath().toString()
            )

    private fun createPackageArguments(appConfig: JPackageAppConfig): Map<String, String> =
            mapOf(
                    "--dest" to jPackageConfig.outputDirPath.toAbsolutePath().toString(),

                    "--name" to appConfig.name,
                    "--description" to "A decentralized bitcoin exchange network.",
                    "--copyright" to "Copyright © 2013-${Year.now()} - The Bisq developers",
                    "--vendor" to "Bisq",
                    "--license-file" to appConfig.licenceFilePath,
                    "--app-version" to appConfig.appVersion
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
