package bisq.gradle.bitcoind

import bisq.gradle.tasks.bitcoind.BitcoindCreateOrLoadWalletTask
import bisq.gradle.tasks.bitcoind.BitcoindMineToWallet
import bisq.gradle.tasks.bitcoind.StartBitcoinQtTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import java.io.File


class BitcoindWalletCreationTasks(
    project: Project,
    bisqDataDir: File,
    private val taskNameSuffix: String = ""
) {

    private val tasks: TaskContainer = project.tasks
    val walletDirectory: File = bisqDataDir.resolve("wallets/bitcoind")

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
            walletDirectory.set(this@BitcoindWalletCreationTasks.walletDirectory)
        }

    private fun registerMineInitialRegtestBlocksTask(): TaskProvider<BitcoindMineToWallet> =
        tasks.register<BitcoindMineToWallet>("bitcoindMineInitialRegtestBlocks$taskNameSuffix") {
            dependsOn(createWalletTask)
            onlyIf { getBlockCount() == 0 }

            walletDirectory.set(this@BitcoindWalletCreationTasks.walletDirectory)
            numberOfBlocks.set(101)
        }

    private fun getBlockCount(): Int {
        val blockCount = BitcoindRpcClient.daemonRpcCall(listOf("getblockcount")).trim()
        return blockCount.toInt()
    }
}