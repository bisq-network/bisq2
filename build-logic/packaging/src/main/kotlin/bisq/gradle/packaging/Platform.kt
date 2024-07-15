package bisq.gradle.packaging

import bisq.gradle.packaging.Architecture.ARM_64
import bisq.gradle.packaging.Architecture.X86_64
import bisq.gradle.packaging.OS.*
import bisq.gradle.packaging.Platform.*

enum class Platform(val platformName: String) {
    LINUX_X86_64("linux_x86_64"),
    LINUX_ARM_64("linux_arm64"),

    MACOS_X86_64("macos_x86_64"),
    MACOS_ARM_64("macos_arm64"),

    WIN_X86_64("win_x86_64"),
    WIN_ARM_64("win_arm64")
}

fun getPlatform(): Platform {
    val os = getOS()
    val architecture = getArchitecture()
    when (os) {
        LINUX -> {
            return when (architecture) {
                X86_64 -> LINUX_X86_64
                ARM_64 -> LINUX_ARM_64
            }
        }

        MAC_OS -> {
            return when (architecture) {
                X86_64 -> MACOS_X86_64
                ARM_64 -> MACOS_ARM_64
            }
        }

        WINDOWS -> {
            return when (architecture) {
                X86_64 -> WIN_X86_64
                ARM_64 -> WIN_ARM_64
            }
        }
    }
    throw IllegalStateException("Running on unsupported Platform: ${os.osName} / ${architecture.architectureName}")
}