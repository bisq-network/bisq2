package bisq.gradle.elementsd

import bisq.gradle.bitcoind.BitcoindProcessTasks
import bisq.gradle.bitcoind.tasks.BitcoindMineToWallet
import bisq.gradle.elementsd.tasks.ElementsdStopTask
import bisq.gradle.elementsd.tasks.StartElementsQtTask
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import java.io.File

class ElementsdProcessTasks(
    project: Project,
    private val elementsdDataDir: File
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
            delete = setOf(elementsdDataDir.absolutePath)
        }
}