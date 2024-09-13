package bisq.gradle.bitcoin_core

import bisq.gradle.tasks.PerPlatformUrlProvider

class BitcoinCoreBinaryUrlProvider(private val version: String) : PerPlatformUrlProvider {
    override val urlPrefix: String
        get() = "https://bitcoincore.org/bin/bitcoin-core-$version/bitcoin-$version-"

    override val LINUX_X86_64_URL: String
        get() = "x86_64-linux-gnu.tar.gz"

    override val MACOS_X86_64_URL: String
        get() = "x86_64-apple-darwin.tar.gz"

    override val MACOS_ARM_64_URL: String
        get() = "arm64-apple-darwin.tar.gz"

    override val WIN_X86_64_URL: String
        get() = "win64.zip"
}