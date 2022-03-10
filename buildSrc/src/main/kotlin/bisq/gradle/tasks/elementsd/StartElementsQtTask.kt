package bisq.gradle.tasks.elementsd

import bisq.gradle.Network
import bisq.gradle.elementsd.ElementsdRpcClient
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class StartElementsQtTask : DefaultTask() {
    companion object {
        const val DEFAULT_ELEMENTSD_RPC_PORT = 7040
    }

    @get:Input
    abstract val port: Property<Int>

    @get:Input
    abstract val dataDir: DirectoryProperty

    init {
        port.convention(DEFAULT_ELEMENTSD_RPC_PORT)
    }

    @TaskAction
    open fun start() {
        if (!Network.isPortFree(port.get())) {
            throw StopExecutionException("elementsd is already running.")
        }

        val elementsdDataDir = dataDir.get().asFile
        elementsdDataDir.mkdirs()
        spawnElementsQtProcess(elementsdDataDir)

        waitUntilReady()
    }

    private fun spawnElementsQtProcess(elementsdDataDir: File) {
        ProcessBuilder(
            listOf(
                "elements-qt",
                "-daemon",
                "-port=19444",

                "-chain=elementsregtest",
                "-datadir=" + elementsdDataDir.absolutePath,

                "-server",
                "-rpcbind=127.0.0.1",
                "-rpcallowip=127.0.0.1",
                "-rpcuser=bisq",
                "-rpcpassword=bisq",

                "-mainchainrpchost=127.0.0.1",
                "-mainchainrpcport=18443",
                "-mainchainrpcuser=bisq",
                "-mainchainrpcpassword=bisq",

                "-fallbackfee=0.00000001",
                "-txindex=1"
            )
        ).start()
    }

    private fun waitUntilReady() {
        // ElementsdRpcClient waits until daemon is ready before sending request.
        ElementsdRpcClient.daemonRpcCall(listOf("help"))
    }
}