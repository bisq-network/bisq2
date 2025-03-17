package bisq.gradle.tor_binary

import bisq.gradle.common.OS
import bisq.gradle.common.getOS
import bisq.gradle.tasks.download.SignedBinaryDownloader
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.register

class TorBinaryPackager(private val project: Project, private val torBinaryDownloader: SignedBinaryDownloader) {

    companion object {
        private const val ARCHIVE_EXTRACTION_DIR = "${BisqTorBinaryPlugin.DOWNLOADS_DIR}/extracted"
        private const val PROCESSED_DIR = "${BisqTorBinaryPlugin.DOWNLOADS_DIR}/processed"
    }

    fun registerTasks() {
        val unpackTarTask: TaskProvider<Copy> = project.tasks.register<Copy>("unpackTorBinaryTar") {
            dependsOn(torBinaryDownloader.verifySignatureTask)

            val tarFile: Provider<RegularFile> = torBinaryDownloader.verifySignatureTask.flatMap { it.fileToVerify }
            from(
                tarFile.map {
                    project.tarTree(it.asFile.absolutePath)
                }
            )

            into(project.layout.buildDirectory.dir(ARCHIVE_EXTRACTION_DIR))
        }

        val processUnpackedTorBinaryTar: TaskProvider<Copy> =
            project.tasks.register<Copy>("processUnpackedTorBinaryTar") {
                dependsOn(unpackTarTask)

                from(project.layout.buildDirectory.dir("${ARCHIVE_EXTRACTION_DIR}/data"))
                from(project.layout.buildDirectory.dir("${ARCHIVE_EXTRACTION_DIR}/tor"))
                into(project.layout.buildDirectory.dir(PROCESSED_DIR))
            }

        val packageTorBinary: TaskProvider<Zip> =
            project.tasks.register<Zip>("packageTorBinary") {
                dependsOn(processUnpackedTorBinaryTar)

                archiveFileName.set("tor.zip")
                destinationDirectory.set(project.layout.buildDirectory.dir("generated/src/main/resources"))
                from(project.layout.buildDirectory.dir(PROCESSED_DIR))
            }

        if (getOS() == OS.LINUX) {
            // The Bisq package depends on Tor and the OS's package manager installs Tor and its dependencies for us.
            return
        }

        val processResourcesTask = project.tasks.named("processResources")
        processResourcesTask.configure {
            dependsOn(packageTorBinary)
        }
    }
}