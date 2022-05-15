package bisq.gradle.regtest.elementsd

import bisq.gradle.regtest.elementsd.tasks.ElementsdCreateOrLoadWalletTask
import bisq.gradle.regtest.elementsd.tasks.ElementsdPeginTask
import bisq.gradle.regtest.elementsd.tasks.StartElementsQtTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register

class ElementsdWalletCreationTasks(
    project: Project,
    private val walletDirectory: Provider<Directory>
) {

    private val tasks: TaskContainer = project.tasks

    private lateinit var createOrLoadWalletTask: TaskProvider<ElementsdCreateOrLoadWalletTask>
    lateinit var peginTask: TaskProvider<ElementsdPeginTask>

    fun registerTasks(
        startElementsQtTask: TaskProvider<StartElementsQtTask>,
        bitcoinWalletDirectory: Provider<Directory>
    ) {
        createOrLoadWalletTask = registerCreateOrLoadWalletTask(startElementsQtTask)
        peginTask = registerPeginTask(bitcoinWalletDirectory)
    }

    private fun registerCreateOrLoadWalletTask(startElementsQtTask: TaskProvider<StartElementsQtTask>):
            TaskProvider<ElementsdCreateOrLoadWalletTask> =
        tasks.register<ElementsdCreateOrLoadWalletTask>("elementsdCreatePeginWallet") {
            dependsOn(startElementsQtTask)
            walletDirectory.set(this@ElementsdWalletCreationTasks.walletDirectory)
        }

    private fun registerPeginTask(bitcoinWalletDirectory: Provider<Directory>): TaskProvider<ElementsdPeginTask> =
        tasks.register<ElementsdPeginTask>("elementsdRegtestPegin") {
            dependsOn(createOrLoadWalletTask)
            onlyIf { getBlockCount() == 0 }

            bitcoindWalletDirectory.set(bitcoinWalletDirectory)
            peginWalletDirectory.set(this@ElementsdWalletCreationTasks.walletDirectory)
        }

    private fun getBlockCount(): Int {
        val blockCount = ElementsdRpcClient.daemonRpcCall(listOf("getblockcount")).trim()
        return blockCount.toInt()
    }
}