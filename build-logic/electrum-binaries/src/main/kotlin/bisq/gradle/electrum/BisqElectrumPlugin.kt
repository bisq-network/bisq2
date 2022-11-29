package bisq.gradle.electrum

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create


class BisqElectrumPlugin : Plugin<Project> {

    companion object {
        const val DATA_DIR = "electrum_binaries"
    }

    override fun apply(project: Project) {
        val extension = project.extensions.create<BisqElectrumPluginExtension>("electrum")

        val electrumBinaryDownloader = ElectrumBinaryDownloader(project, extension)
        electrumBinaryDownloader.registerTasks()

        val electrumBinaryPackager = ElectrumBinaryPackager(project, electrumBinaryDownloader)
        electrumBinaryPackager.registerTasks()
    }
}