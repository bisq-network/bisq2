package bisq.gradle.common

import java.util.*

enum class Architecture(val architectureName: String) {
    X86_64("x86_64"),
    ARM_64("arm64"),
}

fun getArchitecture(): Architecture {
    val architectureName = getArchitectureName()
    if (isX86_64(architectureName)) {
        return Architecture.X86_64
    } else if (isArm64(architectureName)) {
        return Architecture.ARM_64
    }

    throw IllegalStateException("Running on unsupported Architecture: $architectureName")
}

fun isX86_64(archName: String): Boolean {
    return is64Bit(archName) && (archName.contains("x86") || archName.contains("amd"))
}

fun isArm64(archName: String): Boolean {
    return is64Bit(archName) && (archName.contains("aarch") || archName.contains("arm"))
}

fun is64Bit(archName: String): Boolean {
    return archName.contains("64")
}

fun getArchitectureName(): String {
    return System.getProperty("os.arch").toLowerCase()
}

