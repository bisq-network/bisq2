package bisq.gradle.electrum.tasks

import bisq.gradle.electrum.DmgImageMounter
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit


abstract class ExtractElectrumAppFromDmgFile : DefaultTask() {

    companion object {
        private const val VOLUMES_DIR = "/Volumes"
        private const val MOUNT_DIR = "$VOLUMES_DIR/Electrum"

        private const val ELECTRUM_APP = "Electrum.app"
        private const val MOUNTED_ELECTRUM_APP_PATH = "$MOUNT_DIR/$ELECTRUM_APP"

        private const val CMD_TIMEOUT: Long = 25
        private const val MOUNT_TIMEOUT_IN_MILLISECONDS = 30 * 1000
    }

    @get:InputFile
    abstract val dmgFile: Property<Provider<RegularFile>>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:OutputDirectory
    val electrumAppDestinationFile: Provider<Directory>
        get() = outputDirectory.dir(ELECTRUM_APP)

    @TaskAction
    fun extract() {
        val electrumAppFile = electrumAppDestinationFile.get().asFile
        if (electrumAppFile.exists()) {
            deleteElectrumAppFile()
        }

        electrumAppFile.mkdirs()

        val dmgImageMounter = DmgImageMounter(dmgFile.get().get().asFile, File(MOUNT_DIR))
        dmgImageMounter.use {
            dmgImageMounter.mount()
            waitUntilDmgImageMounted()
            copyElectrumAppToOutputDirectory()
        }
    }

    private fun waitUntilDmgImageMounted() {
        val startTime: Long = System.currentTimeMillis()
        val mountDir = File(MOUNT_DIR)
        while (!mountDir.exists()) {
            Thread.sleep(200)

            val currentTime: Long = System.currentTimeMillis()
            if (currentTime - startTime >= MOUNT_TIMEOUT_IN_MILLISECONDS) {
                throw IllegalStateException("$MOUNT_DIR is still missing after 30 seconds of mounting the DMG image.")
            }
        }
    }

    private fun deleteElectrumAppFile() {
        val electrumAppFilePath = electrumAppDestinationFile.get().asFile.toPath()
        Files.walk(electrumAppFilePath)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete)
    }

    private fun copyElectrumAppToOutputDirectory() {
        val destinationDir = outputDirectory.get().asFile.absolutePath
        val copyProcess: Process = ProcessBuilder(
            "cp", "-r", MOUNTED_ELECTRUM_APP_PATH, destinationDir
        )
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()

        val isSuccess: Boolean = copyProcess.waitFor(2, TimeUnit.MINUTES)
        if (!isSuccess) {
            throw IllegalStateException("Could not copy Electrum.app to output directory.")
        }
    }
}