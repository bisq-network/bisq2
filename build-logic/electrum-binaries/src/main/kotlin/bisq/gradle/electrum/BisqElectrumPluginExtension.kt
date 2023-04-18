package bisq.gradle.electrum

import org.gradle.api.provider.Property

abstract class BisqElectrumPluginExtension {
    abstract val version: Property<String>
}