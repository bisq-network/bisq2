package bisq.gradle.elementsd

import bisq.gradle.ApplicationRunTaskFactory
import bisq.gradle.bitcoind.BitcoindProcessTasks
import bisq.gradle.bitcoind.BitcoindWalletCreationTasks
import org.gradle.api.Project

class ElementsdRegtestTasks(private val project: Project, private val regtestConfig: ElementsRegtestConfig) {

    private lateinit var bitcoindProcessTasks: BitcoindProcessTasks
    private lateinit var bitcoindWalletCreationTasks: BitcoindWalletCreationTasks

    private lateinit var elementsdWalletCreationTasks: ElementsdWalletCreationTasks

    fun registerTasks() {
        registerBitcoindTasks()
        registerElementsdTasks()

        ApplicationRunTaskFactory.registerDesktopRegtestRunTask(
            project = project,
            taskName = "runWithElementsdRegtestWallet",
            description = "Run Bisq with Elements Core Wallet (Regtest)",
            cmdLineArgs = listOf("--regtest-elementsd"),
            dataDir = regtestConfig.bisqDataDir,
            dependentTask = elementsdWalletCreationTasks.peginTask
        )
    }

    private fun registerBitcoindTasks() {
        bitcoindProcessTasks = BitcoindProcessTasks(
            project,
            regtestConfig.bitcoindDataDir,
            taskNameSuffix = "ForElementsRegtest"
        )
        bitcoindProcessTasks.registerTasks()

        bitcoindWalletCreationTasks = BitcoindWalletCreationTasks(
            project = project,
            regtestConfig = regtestConfig,
            taskNameSuffix = "ForElementsRegtest"
        )
        bitcoindWalletCreationTasks.registerTasks(bitcoindProcessTasks.startBitcoinQtTask)
    }

    private fun registerElementsdTasks() {
        val elementsdProcessTasks =
            ElementsdProcessTasks(project, regtestConfig.elementsdDataDir)
        elementsdProcessTasks.registerTasks(
            bitcoindProcessTasks,
            bitcoindWalletCreationTasks.mineInitialRegtestBlocksTask
        )

        elementsdWalletCreationTasks = ElementsdWalletCreationTasks(
            project = project,
            walletDirectory = regtestConfig.elementsdWalletDir
        )
        elementsdWalletCreationTasks.registerTasks(
            elementsdProcessTasks.elementsdStartTask,
            bitcoindWalletCreationTasks.walletDirProvider
        )
    }
}