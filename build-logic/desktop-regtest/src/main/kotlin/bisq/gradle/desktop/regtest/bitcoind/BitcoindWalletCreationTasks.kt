package bisq.gradle.desktop.regtest.bitcoind

import bisq.gradle.desktop.regtest.bitcoind.tasks.BitcoindCreateOrLoadWalletTask
import bisq.gradle.desktop.regtest.bitcoind.tasks.BitcoindMineToWallet
import bisq.gradle.desktop.regtest.bitcoind.tasks.StartBitcoinQtTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register

class BitcoindWalletCreationTasks(
    project: Project,
    regtestConfig: BitcoindRegtestConfig,
    private val taskNameSuffix: String = ""
) {

    private val tasks: TaskContainer = project.tasks
    val walletDirProvider: Provider<Directory> = regtestConfig.bitcoindWalletDir

    private lateinit var createWalletTask: TaskProvider<BitcoindCreateOrLoadWalletTask>
    lateinit var mineInitialRegtestBlocksTask: TaskProvider<BitcoindMineToWallet>

    fun registerTasks(startBitcoinQtTask: TaskProvider<StartBitcoinQtTask>) {
        createWalletTask = registerCreateMinerWalletTask(startBitcoinQtTask)
        mineInitialRegtestBlocksTask = registerMineInitialRegtestBlocksTask()
    }

    private fun registerCreateMinerWalletTask(startBitcoinQtTask: TaskProvider<StartBitcoinQtTask>):
            TaskProvider<BitcoindCreateOrLoadWalletTask> =
        tasks.register<BitcoindCreateOrLoadWalletTask>("bitcoindCreateMinerWallet$taskNameSuffix") {
            dependsOn(startBitcoinQtTask)
            walletDirectory.set(walletDirProvider)
        }

    private fun registerMineInitialRegtestBlocksTask(): TaskProvider<BitcoindMineToWallet> =
        tasks.register<BitcoindMineToWallet>("bitcoindMineInitialRegtestBlocks$taskNameSuffix") {
            dependsOn(createWalletTask)
            onlyIf { getBlockCount() == 0 }

            walletDirectory.set(walletDirProvider)
            numberOfBlocks.set(101)
        }

    private fun getBlockCount(): Int {
        val blockCount = BitcoindRpcClient.daemonRpcCall(listOf("getblockcount")).trim()
        return blockCount.toInt()
    }
}