package bisq.gradle.electrum.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit


abstract class ExtractElectrumAppFromDmgFile : DefaultTask() {

    companion object {
        private const val MOUNT_DIR = "/Volumes/Electrum"
        private const val ELECTRUM_APP = "Electrum.app"
        private const val MOUNTED_ELECTRUM_APP_PATH = "$MOUNT_DIR/$ELECTRUM_APP"
    }


    @get:Input
    abstract val electrumVersion: Property<String>

    @get:InputDirectory
    abstract val inputDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    private val electrumAppDestinationFile: File
        get() = outputDirectory.get().asFile.resolve(ELECTRUM_APP)

    @TaskAction
    fun extract() {
        if (electrumAppDestinationFile.exists()) {
            return
        }

        attachDmgFile()
        copyElectrumAppToOutputDirectory()
        detachDmgFile()
    }

    private fun attachDmgFile() {
        val dmgFile = inputDirectory.get().asFile.resolve("electrum-${electrumVersion.get()}.dmg")
        val attachDmgFile: Process = ProcessBuilder("hdiutil", "attach", dmgFile.absolutePath).start()
        val isSuccess: Boolean = attachDmgFile.waitFor(25, TimeUnit.SECONDS)
        if (!isSuccess) {
            throw IllegalStateException("Could not attach DMG file.")
        }
    }

    private fun copyElectrumAppToOutputDirectory() {
        val electrumAppFile = Paths.get(MOUNTED_ELECTRUM_APP_PATH)
        val destinationDir = electrumAppDestinationFile.absolutePath

        Files.walk(electrumAppFile)
            .forEach { source ->
                val destination: Path = Paths.get(
                    destinationDir, source.toString().substring(MOUNTED_ELECTRUM_APP_PATH.length)
                )
                Files.copy(source, destination)
            }
    }

    private fun detachDmgFile() {
        val attachDmgFile: Process = ProcessBuilder("hdiutil", "detach", MOUNT_DIR).start()
        val isSuccess: Boolean = attachDmgFile.waitFor(25, TimeUnit.SECONDS)
        if (!isSuccess) {
            throw IllegalStateException("Could not detach DMG file.")
        }
    }
}