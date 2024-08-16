package bisq.gradle.bitcoin_core

import bisq.gradle.tasks.PerOsUrlProvider

class BitcoinCoreHashFileUrlProvider(private val version: String) : PerOsUrlProvider {
    override val urlPrefix: String
        get() = "https://bitcoincore.org/bin/bitcoin-core-$version/SHA256SUMS"

    override val linuxUrl: String
        get() = ""

    override val macOsUrl: String
        get() = ""

    override val windowsUrl: String
        get() = ""
}