package bisq.gradle.desktop.regtest.elementsd

import bisq.gradle.desktop.regtest.bitcoind.BitcoindProcessTasks
import bisq.gradle.desktop.regtest.bitcoind.tasks.BitcoindMineToWallet
import bisq.gradle.desktop.regtest.elementsd.tasks.ElementsdStopTask
import bisq.gradle.desktop.regtest.elementsd.tasks.StartElementsQtTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register

class ElementsdProcessTasks(
    project: Project,
    private val elementsdDataDir: Provider<Directory>
) {

    private val tasks: TaskContainer = project.tasks

    lateinit var elementsdStartTask: TaskProvider<StartElementsQtTask>
    private lateinit var elementsdStopTask: TaskProvider<ElementsdStopTask>

    fun registerTasks(
        bitcoindProcessTasks: BitcoindProcessTasks,
        mineInitialRegtestBlocksTask: TaskProvider<BitcoindMineToWallet>
    ) {
        elementsdStartTask = registerElementsdStartTask(mineInitialRegtestBlocksTask)

        elementsdStopTask = registerElementsdStopTask()
        bitcoindProcessTasks.stopBitcoindTask.configure { dependsOn(elementsdStopTask) }

        registerCleanTask()
    }

    private fun registerElementsdStartTask(mineInitialRegtestBlocksTask: TaskProvider<BitcoindMineToWallet>):
            TaskProvider<StartElementsQtTask> =
        tasks.register<StartElementsQtTask>("elementsdStart") {
            dependsOn(mineInitialRegtestBlocksTask)
            dataDir.set(elementsdDataDir)
        }

    private fun registerElementsdStopTask(): TaskProvider<ElementsdStopTask> =
        tasks.register<ElementsdStopTask>("elementsdStop")

    private fun registerCleanTask(): TaskProvider<Delete> =
        tasks.register<Delete>("cleanElementsdRegtest") {
            dependsOn(elementsdStopTask)
            delete = setOf(elementsdDataDir)
        }
}