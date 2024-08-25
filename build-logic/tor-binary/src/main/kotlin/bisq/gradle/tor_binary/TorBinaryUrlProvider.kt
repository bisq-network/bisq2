package bisq.gradle.tor_binary

import bisq.gradle.tasks.PerOsUrlProvider

class TorBinaryUrlProvider(private val version: String) : PerOsUrlProvider {
    override val urlPrefix: String
        get() = "https://archive.torproject.org/tor-package-archive/torbrowser/$version/"

    override val LINUX_X86_64_URL: String
        get() = "tor-expert-bundle-linux-x86_64-$version.tar.gz"

    override val MACOS_X86_64_URL: String
        get() = "tor-expert-bundle-macos-x86_64-$version.tar.gz"

    override val MACOS_ARM_64_URL: String
        // Currently the Tor version for aarch64 does not work, thus we use the x64 version.
        // See: https://github.com/bisq-network/bisq2/issues/2679
        // Once resolved we can use the line below.
        // get() = "tor-expert-bundle-macos-aarch64-$version.tar.gz"
        get() = "tor-expert-bundle-macos-x86_64-$version.tar.gz"

    override val WIN_X86_64_URL: String
        get() = "tor-expert-bundle-windows-x86_64-$version.tar.gz"
}