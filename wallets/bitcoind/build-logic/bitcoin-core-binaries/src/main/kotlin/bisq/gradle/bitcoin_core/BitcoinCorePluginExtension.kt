package bisq.gradle.bitcoin_core

import org.gradle.api.provider.Property

abstract class BitcoinCorePluginExtension {
    abstract val version: Property<String>
}