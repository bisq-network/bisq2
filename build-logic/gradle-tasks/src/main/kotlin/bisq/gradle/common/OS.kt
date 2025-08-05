package bisq.gradle.common

import java.util.*

enum class OS(val osName: String) {
    LINUX("linux"),
    MAC_OS("macos"),
    WINDOWS("win")
}

fun getOS(): OS {
    val osName = getOSName()
    if (isLinux(osName)) {
        return OS.LINUX
    } else if (isMacOs(osName)) {
        return OS.MAC_OS
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

fun getOSName(): String {
    return System.getProperty("os.name").toLowerCase()
}
