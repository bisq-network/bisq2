package bisq.gradle.electrum

import bisq.gradle.tasks.PerOsUrlProvider

class ElectrumBinaryUrlProvider(private val version: String) : PerOsUrlProvider {
    override val urlPrefix: String
        get() = "https://download.electrum.org/$version/"

    override val linuxUrl: String
        get() = "electrum-$version-x86_64.AppImage"

    override val macOsUrl: String
        get() = "electrum-$version.dmg"

    override val windowsUrl: String
        get() = "electrum-$version.exe"
}