package bisq.gradle.packaging

import org.gradle.api.provider.Property

abstract class PackagingPluginExtension {
    abstract val name: Property<String>
    abstract val version: Property<String>
}