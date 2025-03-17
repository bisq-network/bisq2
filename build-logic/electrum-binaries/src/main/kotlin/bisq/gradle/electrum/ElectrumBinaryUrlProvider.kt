package bisq.gradle.electrum

import bisq.gradle.tasks.PerPlatformUrlProvider

class ElectrumBinaryUrlProvider(private val version: String) : PerPlatformUrlProvider {
    override val urlPrefix: String
        get() = "https://download.electrum.org/$version/"

    override val LINUX_X86_64_URL: String
        get() = "electrum-$version-x86_64.AppImage"

    override val MACOS_X86_64_URL: String
        get() = "electrum-$version.dmg"

    override val MACOS_ARM_64_URL: String
        get() = "electrum-$version.dmg" // No ARM_64 version provided

    override val WIN_X86_64_URL: String
        get() = "electrum-$version.exe"
}