package bisq.gradle.electrum

import org.gradle.api.provider.Property

abstract class BisqElectrumPluginExtension {
    abstract val version: Property<String>

    abstract val appImageHash: Property<String>
    abstract val dmgHash: Property<String>
    abstract val exeHash: Property<String>
}