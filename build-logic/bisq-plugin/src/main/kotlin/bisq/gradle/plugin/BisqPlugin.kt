package bisq.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.create
import java.io.IOException
import java.util.concurrent.TimeUnit


class BisqPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("bisq.java-library")
        val extension = project.extensions.create<BisqPluginExtension>("bisq")

        project.afterEvaluate {
            if (!canRunIntegrationTests(extension)) {
                excludeIntegrationTests(tasks)
            }
        }
    }

    private fun canRunIntegrationTests(extension: BisqPluginExtension) =
        extension.runIntegrationTests.get() && areAllBinariesPresent(extension)

    private fun excludeIntegrationTests(tasks: TaskContainer) {
        @Suppress("UNCHECKED_CAST")
        val testTask = tasks.named("test") as TaskProvider<Test>
        testTask.configure {
            exclude("**/**Integration*")
        }
    }

    private fun areAllBinariesPresent(extension: BisqPluginExtension) =
        extension.neededBinariesToRunIntegrationTests.get()
            .all { binaryName -> isBinaryPresent(binaryName) }

    private fun isBinaryPresent(programName: String): Boolean {
        try {
            val processBuilder = ProcessBuilder(programName, "--help")
            val process = processBuilder.start()
            process.waitFor(15, TimeUnit.SECONDS)
        } catch (e: IOException) {
            if (e.message!!.startsWith("Cannot run program")) {
                return false
            }
            throw e
        }
        return true
    }
}