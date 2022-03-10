package bisq.gradle.elementsd

import bisq.gradle.bitcoind.BitcoindProcessTasks
import bisq.gradle.bitcoind.BitcoindWalletCreationTasks
import bisq.gradle.tasks.ApplicationRunTaskFactory
import org.gradle.api.Project
import java.io.File

class ElementsdRegtestTasks(private val project: Project, dataDir: File) {

    private val bitcoindDataDir: File = dataDir.resolve("bitcoind")
    private val elementsdDataDir: File = dataDir.resolve("elementsd")
    private val bisqDataDir: File = dataDir.resolve("bisq")

    private lateinit var bitcoindProcessTasks: BitcoindProcessTasks
    private lateinit var bitcoindWalletCreationTasks: BitcoindWalletCreationTasks

    private lateinit var elementsdWalletCreationTasks: ElementsdWalletCreationTasks

    fun registerTasks() {
        bisqDataDir.resolve("wallets").mkdirs()

        registerBitcoindTasks()
        registerElementsdTasks()

        ApplicationRunTaskFactory.registerDesktopRegtestRunTask(
            project = project,
            taskName = "runWithElementsdRegtestWallet",
            description = "Run Bisq with Elements Core Wallet (Regtest)",
            cmdLineArgs = listOf("--regtest-elementsd", "--data-dir=${bisqDataDir.absolutePath}"),
            dependentTask = elementsdWalletCreationTasks.peginTask
        )
    }

    private fun registerBitcoindTasks() {
        bitcoindProcessTasks = BitcoindProcessTasks(project, bitcoindDataDir, taskNameSuffix = "ForElementsRegtest")
        bitcoindProcessTasks.registerTasks()

        bitcoindWalletCreationTasks = BitcoindWalletCreationTasks(
            project = project,
            bisqDataDir = bisqDataDir,
            taskNameSuffix = "ForElementsRegtest"
        )
        bitcoindWalletCreationTasks.registerTasks(bitcoindProcessTasks.startBitcoinQtTask)
    }

    private fun registerElementsdTasks() {
        val elementsdProcessTasks =
            ElementsdProcessTasks(project, elementsdDataDir)
        elementsdProcessTasks.registerTasks(
            bitcoindProcessTasks,
            bitcoindWalletCreationTasks.mineInitialRegtestBlocksTask
        )

        elementsdWalletCreationTasks = ElementsdWalletCreationTasks(
            project = project,
            bisqDataDir = bisqDataDir,
        )
        elementsdWalletCreationTasks.registerTasks(
            elementsdProcessTasks.elementsdStartTask,
            bitcoindWalletCreationTasks.walletDirectory
        )
    }
}