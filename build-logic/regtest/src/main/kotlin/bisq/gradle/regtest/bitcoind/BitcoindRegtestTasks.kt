package bisq.gradle.regtest.bitcoind

import bisq.gradle.regtest.ApplicationRunTaskFactory
import bisq.gradle.regtest.bitcoind.tasks.StartBitcoinQtTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

class BitcoindRegtestTasks(
    private val project: Project,
    private val regtestConfig: BitcoindRegtestConfig
) {

    fun registerTasks() {
        val bitcoindProcessTasks = BitcoindProcessTasks(project, regtestConfig.bitcoindDataDir)
        bitcoindProcessTasks.registerTasks()

        val bitcoindWalletCreationTasks: BitcoindWalletCreationTasks =
            registerWalletTasks(bitcoindProcessTasks.startBitcoinQtTask)

        ApplicationRunTaskFactory.registerDesktopRegtestRunTask(
            project = project,
            taskName = "runWithBitcoindRegtestWallet",
            description = "Run Bisq with Bitcoin Core Wallet (Regtest)",
            cmdLineArgs = listOf("--regtest-bitcoind"),
            dataDir = regtestConfig.bisqDataDir,
            dependentTask = bitcoindWalletCreationTasks.mineInitialRegtestBlocksTask
        )
    }

    private fun registerWalletTasks(
        startBitcoinQtTask: TaskProvider<StartBitcoinQtTask>
    ): BitcoindWalletCreationTasks {
        val bitcoindWalletCreationTasks = BitcoindWalletCreationTasks(
            project = project,
            regtestConfig = regtestConfig
        )
        bitcoindWalletCreationTasks.registerTasks(startBitcoinQtTask)
        return bitcoindWalletCreationTasks
    }
}