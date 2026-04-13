package bisq.gradle.packaging

import bisq.gradle.packaging.jpackage.JPackageAppConfig
import bisq.gradle.packaging.jpackage.JPackageConfig
import bisq.gradle.packaging.jpackage.PackageFactory
import bisq.gradle.packaging.jpackage.package_formats.JPackagePackageFormatConfigs
import bisq.gradle.packaging.jpackage.package_formats.LinuxPackages
import bisq.gradle.packaging.jpackage.package_formats.MacPackage
import bisq.gradle.packaging.jpackage.package_formats.WindowsPackage
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.notExists

abstract class JPackageTask : DefaultTask() {

    @get:InputDirectory
    abstract val jdkDirectory: DirectoryProperty

    @get:InputDirectory
    abstract val distDirFile: Property<File>

    @get:InputFile
    abstract val mainJarFile: RegularFileProperty

    @get:Input
    abstract val mainClassName: Property<String>

    @get:Input
    abstract val jvmArgs: SetProperty<String>

    @get:InputFile
    abstract val licenseFile: Property<File>

    @get:Input
    abstract val appName: Property<String>

    @get:Input
    abstract val appVersion: Property<String>

    @get:InputDirectory
    abstract val packageResourcesDir: DirectoryProperty

    @get:Optional
    @get:InputDirectory
    abstract val onionGraterDir: DirectoryProperty

    @get:InputDirectory
    abstract val runtimeImageDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun run() {
        val jPackagePath = jdkDirectory.asFile.get().toPath().resolve("bin").resolve("jpackage")

        val jPackageConfig = JPackageConfig(
            inputDirPath = distDirFile.get().toPath().resolve("lib"),
            outputDirPath = outputDirectory.asFile.get().toPath(),
            runtimeImageDirPath = runtimeImageDirectory.asFile.get().toPath(),

            appConfig = JPackageAppConfig(
                name = appName.get(),
                appVersion = appVersion.get(),
                mainJarFileName = mainJarFile.asFile.get().name,
                mainClassName = mainClassName.get(),
                jvmArgs = jvmArgs.get(),
                licenceFilePath = licenseFile.get().absolutePath
            ),

            packageFormatConfigs = getPackageFormatConfigs()
        )

        val packageFactory = PackageFactory(jPackagePath, jPackageConfig)
        packageFactory.createPackages()
    }

    private fun getPackageFormatConfigs(): JPackagePackageFormatConfigs {
        val packagePath = packageResourcesDir.asFile.get().toPath()
        return when (getOS()) {
            OS.WINDOWS -> {
                val resourcesPath = packagePath.resolve("windows")
                WindowsPackage(resourcesPath)
            }

            OS.MAC_OS -> {
                val resourcesPath = packagePath.resolve("macosx")
                MacPackage(resourcesPath, appName.get())
            }

            OS.LINUX -> {
                val resourcesPath = packagePath.resolve("linux")
                val appContentPaths = stageLinuxAppContent(resourcesPath)
                LinuxPackages(resourcesPath, appName.get(), appContentPaths)
            }
        }
    }

    private fun stageLinuxAppContent(linuxResourcesPath: Path): List<Path> {
        val stagingDir = project.layout.buildDirectory.get().asFile.toPath()
            .resolve("packaging").resolve("app-content")
        if (stagingDir.notExists()) {
            Files.createDirectories(stagingDir)
        }

        val contentPaths = mutableListOf<Path>()

        // Stage prepare_tails.sh as a standalone file in lib/prepare_tails.sh
        val prepareTailsSrc = linuxResourcesPath.resolve("prepare_tails.sh")
        if (Files.exists(prepareTailsSrc)) {
            val dest = stagingDir.resolve("prepare_tails.sh")
            Files.copy(prepareTailsSrc, dest, StandardCopyOption.REPLACE_EXISTING)
            dest.toFile().setExecutable(true)
            contentPaths.add(dest)
        }

        // Stage onion-grater YAMLs into lib/onion-grater/
        if (onionGraterDir.isPresent) {
            val onionGraterStageDir = stagingDir.resolve("onion-grater")
            Files.createDirectories(onionGraterStageDir)
            val srcDir = onionGraterDir.asFile.get().toPath()
            Files.list(srcDir).use { paths ->
                paths
                    .filter { it.toString().endsWith(".yml") }
                    .forEach { src ->
                        Files.copy(src, onionGraterStageDir.resolve(src.fileName), StandardCopyOption.REPLACE_EXISTING)
                    }
            }
            contentPaths.add(stagingDir.resolve("onion-grater"))
        }

        return contentPaths
    }
}
