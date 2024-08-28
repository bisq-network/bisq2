package bisq.gradle.bitcoin_core

import bisq.gradle.tasks.PerPlatformUrlProvider

class BitcoinCoreHashFileUrlProvider(private val version: String) : PerPlatformUrlProvider {
    override val urlPrefix: String
        get() = "https://bitcoincore.org/bin/bitcoin-core-$version/SHA256SUMS"

    override val LINUX_X86_64_URL: String
        get() = ""

    override val MACOS_X86_64_URL: String
        get() = ""

    override val MACOS_ARM_64_URL: String
        get() = ""

    override val WIN_X86_64_URL: String
        get() = ""
}