package bisq.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register
import org.gradle.process.CommandLineArgumentProvider
import java.util.function.BiFunction

object ApplicationRunTaskFactory {

    fun registerDesktopRegtestRunTask(
        project: Project,
        taskName: String,
        description: String,
        cmdLineArgs: List<String>,
        dataDir: Provider<Directory>,
        dependentTask: TaskProvider<out DefaultTask>?
    ) {
        val desktopProject: Project = project.project("desktopapp")
        registerRunTaskToProject(
            project = desktopProject,
            taskName = taskName,
            descriptionText = description,
            cmdLineArgs = cmdLineArgs,
            jvmArgs = listOf(
                "-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.0=127.0.0.1:8000",
                "-Dbisq.networkServiceConfig.seedAddressByTransportType.clear.1=127.0.0.1:8001"
            ),
            dataDir = dataDir,
            dependentTask = dependentTask
        )
    }

    private fun registerRunTaskToProject(
        project: Project,
        taskName: String,
        descriptionText: String,
        cmdLineArgs: List<String>,
        jvmArgs: List<String>,
        dataDir: Provider<Directory>,
        dependentTask: TaskProvider<out DefaultTask>?
    ) {
        project.tasks.register<JavaExec>(taskName) {
            if (dependentTask != null) {
                dependsOn(dependentTask)
            }

            doFirst {
                // Create Bisq wallets directory
                dataDir.get().asFile
                    .resolve("wallets").mkdirs()
            }

            group = "Regtest"
            description = descriptionText

            argumentProviders.add(CommandLineArgumentProvider {
                cmdLineArgs + "--data-dir=${dataDir.get().asFile.absolutePath}"
            })
            classpath = computeRuntimeClasspath(project, mainModule)

            val javaApplication: JavaApplication = project.extensions["application"] as JavaApplication
            mainModule.set(javaApplication.mainModule)
            mainClass.set(javaApplication.mainClass)

            conventionMapping.map("jvmArgs") { javaApplication.applicationDefaultJvmArgs + jvmArgs }

            val javaPluginExtension = project.extensions.getByType(JavaPluginExtension::class.java)
            modularity.inferModulePath.convention(javaPluginExtension.modularity.inferModulePath)
            javaLauncher.convention(getToolchainTool(project, JavaToolchainService::launcherFor))
        }
    }

    private fun computeRuntimeClasspath(project: Project, mainModule: Property<String>): FileCollection =
        project.files().from({
            if (mainModule.isPresent) {
                jarsOnlyRuntimeClasspath(project)
            } else {
                runtimeClasspath(project)
            }
        })

    private fun <T> getToolchainTool(
        project: Project, toolMapper: BiFunction<JavaToolchainService, JavaToolchainSpec, Provider<T>>
    ): Provider<T> {
        val extension: JavaPluginExtension = project.extensions.getByType(JavaPluginExtension::class.java)
        val service: JavaToolchainService = project.extensions.getByType(JavaToolchainService::class.java)
        return toolMapper.apply(service, extension.toolchain)
    }

    private fun jarsOnlyRuntimeClasspath(project: Project): FileCollection =
        project.tasks.getAt(JavaPlugin.JAR_TASK_NAME)
            .outputs.files
            .plus(
                project.configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
            )

    private fun runtimeClasspath(project: Project): FileCollection =
        project.extensions.findByType(JavaPluginExtension::class.java)!!
            .sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            .runtimeClasspath
}