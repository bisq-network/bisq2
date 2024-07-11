package bisq.gradle.tasks

import java.util.*

enum class OS {
    LINUX, MAC_OS, WINDOWS
}

fun getOsArch(): String = System.getProperty("os.arch").toLowerCase(Locale.US).replace(" ", "").trim()
fun getOsName(): String = System.getProperty("os.name").toLowerCase(Locale.US).replace(" ", "").trim()

fun getOS(): OS {
    val osName = getOsName()
    val osArch = getOsArch()
    if (isLinux(osName)) {
        return OS.LINUX
    } else if (isMacOs(osName)) {
        OS.MAC_OS
    } else if (isWindows(osName)) {
        return OS.WINDOWS
    }

    throw IllegalStateException("Running on unsupported OS: $osName")
}

private fun isLinux(osName: String): Boolean {
    return osName.contains("linux")
}

private fun isMacOs(osName: String): Boolean {
    return osName.contains("mac") || osName.contains("darwin")
}

private fun isWindows(osName: String): Boolean {
    return osName.contains("win")
}