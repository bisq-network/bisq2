package bisq.gradle.packaging

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

abstract class PackagingPluginExtension {
    abstract val name: Property<String>
    abstract val version: Property<String>
    abstract val runtimeImageDirectory: DirectoryProperty
    abstract val runtimeImageModules: SetProperty<String>
    abstract val runtimeImageJlinkOptions: ListProperty<String>
    abstract val runtimeImageExcludedNativeCommands: SetProperty<String>
    abstract val requireJavaLauncher: Property<Boolean>
}
