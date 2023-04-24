package bisq.gradle.electrum

import bisq.gradle.electrum.tasks.ExtractElectrumAppFromDmgFile
import bisq.gradle.tasks.OS
import bisq.gradle.tasks.getOS
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.register

class ElectrumBinaryPackager(
    private val project: Project,
    private val binaryDownloader: ElectrumBinaryDownloader,
) {
    companion object {
        private const val BINARIES_DIR = "${BisqElectrumPlugin.DATA_DIR}/binaries"
    }

    private val binariesDir: Provider<Directory> = project.layout.buildDirectory.dir(BINARIES_DIR)

    fun registerTasks() {
        val extractDmgOrCopyTask: TaskProvider<out Task> =
            if (isMacOS()) {
                registerDmgExtractionTask()
            } else {
                registerVerifiedElectrumBinary()
            }

        extractDmgOrCopyTask.configure {
            dependsOn(binaryDownloader.verifyDownloadTask)
        }

        val packageElectrumBinariesTask: TaskProvider<Zip> =
            project.tasks.register<Zip>("packageElectrumBinaries") {
                dependsOn(extractDmgOrCopyTask)

                archiveFileName.set("electrum-binaries.zip")
                destinationDirectory.set(project.layout.buildDirectory.dir("generated/src/main/resources"))

                from(binariesDir)
            }

        val processResourcesTask = project.tasks.named("processResources")
        processResourcesTask.configure {
            dependsOn(packageElectrumBinariesTask)
        }
    }

    private fun isMacOS() = getOS() == OS.MAC_OS

    private fun registerDmgExtractionTask(): TaskProvider<out Task> =
        project.tasks.register<ExtractElectrumAppFromDmgFile>("extractElectrumAppFromDmgFile") {
            dmgFile.set(binaryDownloader.verifyDownloadTask.flatMap { it.fileToVerify })
            outputDirectory.set(binariesDir)
        }

    private fun registerVerifiedElectrumBinary(): TaskProvider<out Task> =
        project.tasks.register<Copy>("copyVerifiedElectrumBinary") {
            from(binaryDownloader.verifyDownloadTask.flatMap { it.fileToVerify })
            into(binariesDir.get().asFile.absolutePath)
        }
}