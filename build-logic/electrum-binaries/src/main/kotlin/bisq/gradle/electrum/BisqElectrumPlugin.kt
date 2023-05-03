package bisq.gradle.electrum

import bisq.gradle.tasks.PgpFingerprint
import bisq.gradle.tasks.download.SignedBinaryDownloader
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create


class BisqElectrumPlugin : Plugin<Project> {

    companion object {
        const val DATA_DIR = "electrum_binaries"
        private const val DOWNLOADS_DIR = "$DATA_DIR/downloads"
    }

    override fun apply(project: Project) {
        val extension = project.extensions.create<BisqElectrumPluginExtension>("electrum")

        val electrumBinaryDownloader = SignedBinaryDownloader(
            project = project,
            binaryName = "Electrum",
            version = extension.version,

            perOsUrlProvider = { version -> ElectrumBinaryUrlProvider(version) },
            downloadDirectory = DOWNLOADS_DIR,

            pgpFingerprintToKeyUrlMap = mapOf(
                Pair(
                    PgpFingerprint.normalize("6694 D8DE 7BE8 EE56 31BE  D950 2BD5 824B 7F94 70E6"),
                    this::class.java.getResource("/ThomasV.asc")!!
                ),

                Pair(
                    PgpFingerprint.normalize("9EDA FF80 E080 6596 04F4  A76B 2EBB 056F D847 F8A7"),
                    this::class.java.getResource("/Emzy.asc")!!
                ),

                Pair(
                    PgpFingerprint.normalize("0EED CFD5 CAFB 4590 6734  9B23 CA9E EEC4 3DF9 11DC"),
                    this::class.java.getResource("/SomberNight.asc")!!
                ),
            )
        )
        electrumBinaryDownloader.registerTasks()

        val electrumBinaryPackager = ElectrumBinaryPackager(project, electrumBinaryDownloader)
        electrumBinaryPackager.registerTasks()
    }
}