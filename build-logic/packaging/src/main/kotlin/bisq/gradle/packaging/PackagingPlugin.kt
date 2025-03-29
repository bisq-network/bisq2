package bisq.gradle.packaging

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JvmImplementation
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.register
import java.io.File
import javax.inject.Inject

class PackagingPlugin @Inject constructor(private val javaToolchainService: JavaToolchainService) : Plugin<Project> {

    companion object {
        const val OUTPUT_DIR_PATH = "packaging/jpackage/packages"
    }

    override fun apply(project: Project) {
        val extension = project.extensions.create<PackagingPluginExtension>("packaging")

        val installDistTask: TaskProvider<Sync> = project.tasks.named("installDist", Sync::class.java)

        val generateHashesTask = project.tasks.register<Sha256HashTask>("generateHashes") {
            inputDirFile.set(installDistTask.map { File(it.destinationDir, "lib") })
            outputFile.set(getHashFileForOs(project, extension))
        }

        val jarTask: TaskProvider<Jar> = project.tasks.named("jar", Jar::class.java)

        val javaApplicationExtension = project.extensions.findByType<JavaApplication>()
        checkNotNull(javaApplicationExtension) { "Can't find JavaApplication extension." }

        project.tasks.register<JPackageTask>("generateInstallers") {
            group = "distribution"
            description = "Generate the installer or the platform the project is running"

            val webcamProject = project.parent?.childProjects?.filter { e -> e.key == "webcam-app" }?.map { e -> e.value.project }?.first()
            webcamProject?.let { webcam ->
                val desktopProject = project.parent?.childProjects?.filter { e -> e.key == "desktop" }?.map { e -> e.value.project }?.first()
                desktopProject?.let { desktop ->
                    val processResourcesInDesktop = desktop.tasks.named("processResources")
                    val processWebcamForDesktopProvider = webcam.tasks.named("processWebcamForDesktop")
                    processResourcesInDesktop.get().dependsOn(processWebcamForDesktopProvider)
                    dependsOn(processWebcamForDesktopProvider)
                }
            }

            dependsOn(generateHashesTask)

            jdkDirectory.set(getJPackageJdkDirectory(extension))

            distDirFile.set(installDistTask.map { it.destinationDir })
            mainJarFile.set(jarTask.flatMap { it.archiveFile })

            mainClassName.set(javaApplicationExtension.mainClass)
            jvmArgs.set(javaApplicationExtension.applicationDefaultJvmArgs)

            val licenseFileProvider: Provider<File> = extension.name.map { name ->
                val licenseDir = if (name == "Bisq") project.projectDir.parentFile
                else project.projectDir.parentFile.parentFile.parentFile
                return@map File(licenseDir, "LICENSE")
            }
            licenseFile.set(licenseFileProvider)

            appName.set(extension.name)
            appVersion.set(extension.version)

            val packageResourcesDirFile = File(project.projectDir, "package")
            packageResourcesDir.set(packageResourcesDirFile)

            runtimeImageDirectory.set(
                    getJPackageJdkDirectory(extension)
            )

            outputDirectory.set(project.layout.buildDirectory.dir("packaging/jpackage/packages"))
        }

        val releaseBinariesTaskFactory = ReleaseBinariesTaskFactory(project)
        releaseBinariesTaskFactory.registerCopyReleaseBinariesTask()
        releaseBinariesTaskFactory.registerCopyMaintainerPublicKeysTask()
        releaseBinariesTaskFactory.registerCopySigningPublicKeyTask()
        releaseBinariesTaskFactory.registerMergeOsSpecificJarHashesTask(extension.version)
    }

    private fun getHashFileForOs(project: Project, extension: PackagingPluginExtension): Provider<RegularFile> {
        val platformName = getPlatform().platformName
        return extension.version.flatMap { version ->
            project.layout.buildDirectory.file("$OUTPUT_DIR_PATH/Bisq-$version-$platformName-all-jars.sha256")
        }
    }

    private fun getJPackageJdkDirectory(extension: PackagingPluginExtension): Provider<Directory> {
        val launcherProvider = javaToolchainService.launcherFor {
            languageVersion.set(getJavaLanguageVersion(extension))
            vendor.set(JvmVendorSpec.AZUL)
            implementation.set(JvmImplementation.VENDOR_SPECIFIC)
        }
        return launcherProvider.map { it.metadata.installationPath }
    }

    private fun getJavaLanguageVersion(extension: PackagingPluginExtension): Provider<JavaLanguageVersion> {
        val javaVersion = extension.name.map { appName ->
            if (appName == "Bisq") {
                // Bisq1
                if (getOS() == OS.MAC_OS) {
                    15
                } else {
                    17
                }
            } else {
                // Bisq2
                21
            }
        }
        return javaVersion.map { JavaLanguageVersion.of(it) }
    }
}
