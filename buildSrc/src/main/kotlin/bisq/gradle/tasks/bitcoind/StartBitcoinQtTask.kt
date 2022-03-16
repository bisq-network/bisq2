package bisq.gradle.tasks.bitcoind

import bisq.gradle.Network
import bisq.gradle.bitcoind.BitcoindRpcClient
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class StartBitcoinQtTask : DefaultTask() {
    companion object {
        const val DEFAULT_BITCOIND_RPC_PORT = 18443
    }

    @get:Input
    abstract val port: Property<Int>

    @get:Input
    abstract val dataDir: DirectoryProperty

    init {
        port.convention(DEFAULT_BITCOIND_RPC_PORT)
    }

    @TaskAction
    open fun start() {
        if (!Network.isPortFree(port.get())) {
            throw StopExecutionException("bitcoind is already running.")
        }

        val bitcoindDataDir = dataDir.get().asFile
        bitcoindDataDir.mkdirs()
        spawnBitcoinQtProcess(bitcoindDataDir)

        waitUntilReady()
    }

    private fun spawnBitcoinQtProcess(bitcoindDataDir: File) {
        ProcessBuilder(
            listOf(
                "bitcoin-qt",
                "-regtest",
                "-daemon",
                "-datadir=${bitcoindDataDir.absolutePath}",

                "-server",
                "-rpcbind=127.0.0.1",
                "-rpcallowip=127.0.0.1",
                "-rpcuser=bisq",
                "-rpcpassword=bisq",

                "-fallbackfee=0.00000001",
                "-txindex=1"
            )
        ).start()
    }

    private fun waitUntilReady() {
        // BitcoindRpcClient waits until daemon is ready before sending request.
        BitcoindRpcClient.daemonRpcCall(listOf("help"))
    }
}