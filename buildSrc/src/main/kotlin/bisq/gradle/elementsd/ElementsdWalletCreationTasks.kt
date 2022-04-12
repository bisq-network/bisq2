package bisq.gradle.elementsd

import bisq.gradle.elementsd.tasks.ElementsdCreateOrLoadWalletTask
import bisq.gradle.elementsd.tasks.ElementsdPeginTask
import bisq.gradle.elementsd.tasks.StartElementsQtTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import java.io.File

class ElementsdWalletCreationTasks(
    project: Project,
    bisqDataDir: File
) {

    private val tasks: TaskContainer = project.tasks
    private val walletDirectory: File = bisqDataDir.resolve("wallets/elementsd")

    private lateinit var createWalletTask: TaskProvider<ElementsdCreateOrLoadWalletTask>
    lateinit var peginTask: TaskProvider<ElementsdPeginTask>

    fun registerTasks(
        startElementsQtTask: TaskProvider<StartElementsQtTask>,
        bitcoinWalletDirectory: File
    ) {
        createWalletTask = registerCreatePeginWalletTask(startElementsQtTask)
        peginTask = registerPeginTask(bitcoinWalletDirectory)
    }

    private fun registerCreatePeginWalletTask(startElementsQtTask: TaskProvider<StartElementsQtTask>):
            TaskProvider<ElementsdCreateOrLoadWalletTask> =
        tasks.register<ElementsdCreateOrLoadWalletTask>("elementsdCreatePeginWallet") {
            dependsOn(startElementsQtTask)
            walletDirectory.set(this@ElementsdWalletCreationTasks.walletDirectory)
        }

    private fun registerPeginTask(bitcoinWalletDirectory: File): TaskProvider<ElementsdPeginTask> =
        tasks.register<ElementsdPeginTask>("elementsdRegtestPegin") {
            dependsOn(createWalletTask)
            onlyIf { getBlockCount() == 0 }

            bitcoindWalletDirectory.set(bitcoinWalletDirectory)
            peginWalletDirectory.set(this@ElementsdWalletCreationTasks.walletDirectory)
        }

    private fun getBlockCount(): Int {
        val blockCount = ElementsdRpcClient.daemonRpcCall(listOf("getblockcount")).trim()
        return blockCount.toInt()
    }
}