package bisq.gradle

import bisq.gradle.bitcoind.BitcoindRegtestTasks
import bisq.gradle.elementsd.ElementsdRegtestTasks
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File


class BisqPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val regtestRootDataDir = project.buildDir.resolve("regtest")
        regtestRootDataDir.mkdirs()

        registerBitcoindRegtestTasks(project, regtestRootDataDir)
        registerElementsdRegtestTasks(project, regtestRootDataDir)
    }

    private fun registerBitcoindRegtestTasks(project: Project, regtestRootDataDir: File) {
        val bitcoindDataDir: File = regtestRootDataDir.resolve("bitcoind_regtest")
        val bitcoindRegtestTasks = BitcoindRegtestTasks(project, bitcoindDataDir)
        bitcoindRegtestTasks.registerTasks()
    }

    private fun registerElementsdRegtestTasks(project: Project, regtestRootDataDir: File) {
        val elementsdDataDir: File = regtestRootDataDir.resolve("elementsd_regtest")
        val elementsdRegtestTasks = ElementsdRegtestTasks(project, elementsdDataDir)
        elementsdRegtestTasks.registerTasks()
    }


}