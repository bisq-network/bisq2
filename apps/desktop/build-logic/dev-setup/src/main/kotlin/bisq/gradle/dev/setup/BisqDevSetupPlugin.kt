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

        project.tasks.register<BisqDesktopNodeTask>("alice") {
            javaExecutable.set(launcherExecutable)
            classPath.setFrom(runtimeClassPath)
            nameSuffix.set("Alice")
        }

        project.tasks.register<BisqDesktopNodeTask>("bob") {
            javaExecutable.set(launcherExecutable)
            classPath.setFrom(runtimeClassPath)
            nameSuffix.set("Bob")
        }
    }
}