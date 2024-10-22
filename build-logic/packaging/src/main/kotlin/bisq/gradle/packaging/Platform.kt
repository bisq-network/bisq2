package bisq.gradle.packaging

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
        OS.LINUX -> {
            return when (architecture) {
                Architecture.X86_64 -> Platform.LINUX_X86_64
                Architecture.ARM_64 -> Platform.LINUX_ARM_64
            }
        }

        OS.MAC_OS -> {
            return when (architecture) {
                Architecture.X86_64 -> Platform.MACOS_X86_64
                Architecture.ARM_64 -> Platform.MACOS_ARM_64
            }
        }

        OS.WINDOWS -> {
            return when (architecture) {
                Architecture.X86_64 -> Platform.WIN_X86_64
                Architecture.ARM_64 -> Platform.WIN_ARM_64
            }
        }
    }
    throw IllegalStateException("Running on unsupported Platform: ${os.osName} / ${architecture.architectureName}")
}