package bisq.gradle.tasks

import bisq.gradle.common.Platform.*
import bisq.gradle.common.getPlatform

interface PerPlatformUrlProvider {
    val urlPrefix: String

    val LINUX_X86_64_URL: String
    val MACOS_X86_64_URL: String
    val MACOS_ARM_64_URL: String
    val WIN_X86_64_URL: String

    val url: String
        get() = urlPrefix + getUrlSuffix()

    private fun getUrlSuffix() =
        when (getPlatform()) {
            LINUX_X86_64 -> LINUX_X86_64_URL
            LINUX_ARM_64 -> LINUX_X86_64_URL // No ARM_64 provided
            MACOS_X86_64 -> MACOS_X86_64_URL
            MACOS_ARM_64 -> MACOS_ARM_64_URL
            WIN_X86_64 -> WIN_X86_64_URL
            WIN_ARM_64 -> WIN_X86_64_URL // No ARM_64 provided
        }

}