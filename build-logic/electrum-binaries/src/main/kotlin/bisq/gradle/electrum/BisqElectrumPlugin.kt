package bisq.gradle.electrum

import bisq.gradle.electrum.tasks.DownloadElectrumBinariesTask
import bisq.gradle.electrum.tasks.VerifyElectrumBinariesTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register


class BisqElectrumPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create<BisqElectrumPluginExtension>("electrum")

        val downloadTask: TaskProvider<DownloadElectrumBinariesTask> =
            project.tasks.register<DownloadElectrumBinariesTask>("downloadElectrumBinaries") {
                electrumVersion.set(extension.electrumVersion)
                outputDir.set(project.layout.buildDirectory.dir("electrum_binaries/downloads"))
            }

        val verifyElectrumBinariesTask: TaskProvider<VerifyElectrumBinariesTask> =
            project.tasks.register<VerifyElectrumBinariesTask>("verifyElectrumBinaries") {
                electrumVersion.set(extension.electrumVersion)

                val downloadsDirectory: Provider<Directory> = downloadTask.flatMap { it.outputDir }
                inputDirectory.set(downloadsDirectory)

                outputDirectory.set(project.layout.buildDirectory.dir("electrum_binaries/binaries"))
            }

        val packageElectrumBinariesTask: TaskProvider<Zip> =
            project.tasks.register<Zip>("packageElectrumBinaries") {
                archiveFileName.set("electrum-binaries.zip")
                destinationDirectory.set(project.layout.buildDirectory.dir("generated/src/main/resources"))

                val binariesDir: Provider<Directory> = verifyElectrumBinariesTask.flatMap { it.outputDirectory }
                from(binariesDir)
            }

        val processResourcesTask = project.tasks.named("processResources")
        processResourcesTask.configure {
            dependsOn(packageElectrumBinariesTask)
        }
    }
}