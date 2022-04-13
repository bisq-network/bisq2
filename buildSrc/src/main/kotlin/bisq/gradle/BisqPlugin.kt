package bisq.gradle

import bisq.gradle.bitcoind.BitcoindRegtestConfig
import bisq.gradle.bitcoind.BitcoindRegtestTasks
import bisq.gradle.elementsd.ElementsRegtestConfig
import bisq.gradle.elementsd.ElementsdRegtestTasks
import org.gradle.api.Plugin
import org.gradle.api.Project


class BisqPlugin : Plugin<Project> {
    companion object {
        const val REGTEST_ROOT_DIR = "regtest"
    }

    override fun apply(project: Project) {
        registerBitcoindRegtestTasks(project)
        registerElementsdRegtestTasks(project)
    }

    private fun registerBitcoindRegtestTasks(project: Project) {
        val regtestConfig = BitcoindRegtestConfig(
            project = project,
            regtestDir = "$REGTEST_ROOT_DIR/bitcoind_regtest"
        )
        val bitcoindRegtestTasks = BitcoindRegtestTasks(project, regtestConfig)
        bitcoindRegtestTasks.registerTasks()
    }

    private fun registerElementsdRegtestTasks(project: Project) {
        val regtestConfig = ElementsRegtestConfig(
            project = project,
            regtestDir = "$REGTEST_ROOT_DIR/elementsd_regtest"
        )
        val elementsdRegtestTasks = ElementsdRegtestTasks(project, regtestConfig)
        elementsdRegtestTasks.registerTasks()
    }
}