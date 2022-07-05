package bisq.gradle.plugin

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

abstract class BisqPluginExtension {
    abstract val runIntegrationTests: Property<Boolean>
    abstract val neededBinariesToRunIntegrationTests: SetProperty<String>

    init {
        runIntegrationTests.convention(false)
        neededBinariesToRunIntegrationTests.convention(emptySet())
    }
}