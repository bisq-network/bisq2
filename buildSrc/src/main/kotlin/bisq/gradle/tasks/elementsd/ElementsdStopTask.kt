package bisq.gradle.tasks.elementsd

import bisq.gradle.Network
import bisq.gradle.elementsd.ElementsdRpcClient
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction

abstract class ElementsdStopTask : DefaultTask() {
    @get:Input
    abstract val port: Property<Int>

    init {
        port.convention(StartElementsQtTask.DEFAULT_ELEMENTSD_RPC_PORT)
    }

    @TaskAction
    fun doRpcCall() {
        if (Network.isPortFree(port.get())) {
            throw StopExecutionException("bitcoind is not running.")
        }

        ElementsdRpcClient.daemonRpcCall(
            listOf("stop")
        )
    }
}