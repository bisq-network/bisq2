package bisq.gradle.electrum

import java.io.Closeable
import java.io.File
import java.util.concurrent.TimeUnit

class DmgImageMounter(
    private val dmgFile: File,
    private val mountDirectory: File
) : Closeable {

    companion object {
        private const val CMD_TIMEOUT: Long = 25
    }

    fun mount() {
        if (isImageMounted()) {
            return
        }

        val attachDmgFileProcess: Process = ProcessBuilder("hdiutil", "attach", dmgFile.absolutePath)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()

        var isSuccess: Boolean = attachDmgFileProcess.waitFor(CMD_TIMEOUT, TimeUnit.SECONDS)
        val exitCode = attachDmgFileProcess.exitValue()

        isSuccess = isSuccess && exitCode == 0
        if (!isSuccess) {
            throw IllegalStateException("Could not attach DMG file. hdiutil attach exit code: $exitCode")
        }
    }

    private fun unmount() {
        if (!isImageMounted()) {
            return
        }

        val detachDmgFileProcess: Process = ProcessBuilder("hdiutil", "detach", mountDirectory.absolutePath)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()

        var isSuccess: Boolean = detachDmgFileProcess.waitFor(CMD_TIMEOUT, TimeUnit.SECONDS)
        val exitCode = detachDmgFileProcess.exitValue()

        isSuccess = isSuccess && exitCode == 0
        if (!isSuccess) {
            throw IllegalStateException("Could not detach DMG file. hdiutil detach exit code: $exitCode")
        }
    }

    override fun close() {
        unmount()
    }

    private fun isImageMounted(): Boolean {
        return mountDirectory.exists()
    }
}