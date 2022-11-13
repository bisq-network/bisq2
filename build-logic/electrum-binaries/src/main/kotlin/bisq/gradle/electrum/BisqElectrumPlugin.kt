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

    companion object {
        private const val DATA_DIR = "electrum_binaries"
        private const val DOWNLOADS_DIR = "$DATA_DIR/downloads"
        private const val BINARIES_DIR = "$DATA_DIR/binaries"
    }

    override fun apply(project: Project) {
        val extension = project.extensions.create<BisqElectrumPluginExtension>("electrum")

        val downloadTask: TaskProvider<DownloadElectrumBinariesTask> =
            project.tasks.register<DownloadElectrumBinariesTask>("downloadElectrumBinaries") {
                electrumVersion.set(extension.version)

                binaryHashes.appImageHash.set(extension.appImageHash)
                binaryHashes.dmgHash.set(extension.dmgHash)
                binaryHashes.exeHash.set(extension.exeHash)

                outputDir.set(project.layout.buildDirectory.dir(DOWNLOADS_DIR))
            }

        val verifyElectrumBinariesTask: TaskProvider<VerifyElectrumBinariesTask> =
            project.tasks.register<VerifyElectrumBinariesTask>("verifyElectrumBinaries") {
                electrumVersion.set(extension.version)

                val downloadsDirectory: Provider<Directory> = downloadTask.flatMap { it.outputDir }
                inputDirectory.set(downloadsDirectory)

                outputDirectory.set(project.layout.buildDirectory.dir(BINARIES_DIR))
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