package bisq.gradle.dev.setup

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JvmImplementation
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.register
import javax.inject.Inject

class BisqDevSetupPlugin @Inject constructor(private val javaToolchainService: JavaToolchainService) : Plugin<Project> {
    override fun apply(project: Project) {
        val launcherProvider = javaToolchainService.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
            vendor.set(JvmVendorSpec.AZUL)
            implementation.set(JvmImplementation.VENDOR_SPECIFIC)
        }
        val launcherExecutable = launcherProvider.map { it.executablePath }

        val javaPluginExtension = project.extensions.findByType<JavaPluginExtension>()
        checkNotNull(javaPluginExtension) { "Can't find JavaPluginExtension extension." }

        val mainSourceSet = javaPluginExtension.sourceSets.named("main", SourceSet::class.java)
        val runtimeClassPath: Provider<FileCollection> = mainSourceSet.map { it.runtimeClasspath }

        project.tasks.register<BisqSeedNodeTask>("seed1") {
            javaExecutable.set(launcherExecutable)
            classPath.setFrom(runtimeClassPath)
            nameSuffix.set("1")
            port.set(8000)
        }

        project.tasks.register<BisqSeedNodeTask>("seed2") {
            javaExecutable.set(launcherExecutable)
            classPath.setFrom(runtimeClassPath)
            nameSuffix.set("2")
            port.set(8001)
        }
    }
}