package bisq.gradle.bitcoind

import bisq.gradle.ApplicationRunTaskFactory
import bisq.gradle.bitcoind.tasks.StartBitcoinQtTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import java.io.File

class BitcoindRegtestTasks(private val project: Project, dataDir: File) {

    private val bitcoindDataDir: File = dataDir.resolve("bitcoind")
    private val bisqDataDir: File = dataDir.resolve("bisq")

    fun registerTasks() {
        bisqDataDir.resolve("wallets").mkdirs()

        val bitcoindProcessTasks = BitcoindProcessTasks(project, bitcoindDataDir)
        bitcoindProcessTasks.registerTasks()

        val bitcoindWalletCreationTasks: BitcoindWalletCreationTasks =
            registerWalletTasks(bitcoindProcessTasks.startBitcoinQtTask)

        ApplicationRunTaskFactory.registerDesktopRegtestRunTask(
            project = project,
            taskName = "runWithBitcoindRegtestWallet",
            description = "Run Bisq with Bitcoin Core Wallet (Regtest)",
            cmdLineArgs = listOf("--regtest-bitcoind", "--data-dir=${bisqDataDir.absolutePath}"),
            dependentTask = bitcoindWalletCreationTasks.mineInitialRegtestBlocksTask
        )
    }

    private fun registerWalletTasks(
        startBitcoinQtTask: TaskProvider<StartBitcoinQtTask>
    ): BitcoindWalletCreationTasks {
        val bitcoindWalletCreationTasks = BitcoindWalletCreationTasks(
            project = project,
            bisqDataDir = bisqDataDir
        )
        bitcoindWalletCreationTasks.registerTasks(startBitcoinQtTask)
        return bitcoindWalletCreationTasks
    }
}