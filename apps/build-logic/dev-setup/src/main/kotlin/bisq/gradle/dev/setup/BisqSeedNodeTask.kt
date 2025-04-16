package bisq.gradle.dev.setup

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

abstract class BisqSeedNodeTask : DefaultTask() {
    @get:InputFile
    abstract val javaExecutable: RegularFileProperty

    @get:InputFiles
    abstract val classPath: ConfigurableFileCollection

    @get:Input
    abstract val nameSuffix: Property<String>

    @get:Input
    abstract val port: Property<Int>

    @TaskAction
    fun start() {
        val javaExecPath = javaExecutable.get().asFile.absolutePath
        val fullClassPath = classPath.files.joinToString(":") { it.absolutePath }

        val processBuilder = ProcessBuilder(javaExecPath,
            "--class-path", fullClassPath,
            "-Dapplication.appName=bisq2_seed${nameSuffix.get()}",
            "-Dapplication.network.configByTransportType.clear.defaultNodePort=${port.get()}",
            "-Dapplication.network.supportedTransportTypes.0=CLEAR",
            "-Dapplication.network.seedAddressByTransportType.clear.0=127.0.0.1:8000",
            "-Dapplication.network.seedAddressByTransportType.clear.1=127.0.0.1:8001",
            "bisq.seed_node.SeedNodeApp")

        processBuilder.redirectErrorStream(true)
        processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD)

        val process = processBuilder.start()
        process.waitFor()
    }
}