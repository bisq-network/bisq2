package bisq.gradle.bitcoin_core

import bisq.gradle.tasks.PgpFingerprint
import bisq.gradle.tasks.download.DownloadTaskFactory
import bisq.gradle.tasks.download.SignedBinaryDownloader
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register


class BitcoinCorePlugin : Plugin<Project> {

    companion object {
        const val DATA_DIR = "bitcoin_core"
        private const val DOWNLOADS_DIR = "$DATA_DIR/downloads"
    }

    override fun apply(project: Project) {
        val extension = project.extensions.create<BitcoinCorePluginExtension>("bitcoin_core")

        val hashFileDownloader = SignedBinaryDownloader(
            project = project,
            binaryName = "BitcoinCoreSha256Sums",
            version = extension.version,

            perPlatformUrlProvider = { version -> BitcoinCoreHashFileUrlProvider(version) },
            downloadDirectory = DOWNLOADS_DIR,

            pgpFingerprintToKeyUrlMap = mapOf(
                filenameAndFingerprint("0xb10c.gpg", "982A 193E 3CE0 EED5 35E0  9023 188C BB26 4841 6AD5"),
                filenameAndFingerprint("achow101.gpg", "1528 1230 0785 C964 44D3  334D 1756 5732 E08E 5E41"),
                filenameAndFingerprint("benthecarman.gpg", "0AD8 3877 C1F0 CD1E E9BD  660A D7CC 770B 81FD 22A8"),
                filenameAndFingerprint("cfields.gpg", "C060 A663 5913 D98A 3587  D7DB 1C24 91FF EB0E F770"),
                filenameAndFingerprint("CoinForensics.gpg", "1015 98DC 823C 1B5F 9A66  24AB A5E0 907A 0380 E6C3"),
                filenameAndFingerprint("darosior.gpg", "590B 7292 695A FFA5 B672  CBB2 E13F C145 CD3F 4304"),
                filenameAndFingerprint("davidgumberg.gpg", "41E4 42A1 4C34 2C87 7AE4  DC8F 3B63 05FA 06DE 51D5"),
                filenameAndFingerprint("dunxen.gpg", "9484 44FC E03B 05BA 5AB0  591E C37B 1C1D 44C7 86EE"),
                filenameAndFingerprint("Emzy.gpg", "9EDA FF80 E080 6596 04F4  A76B 2EBB 056F D847 F8A7"),
                filenameAndFingerprint("fanquake.gpg", "E777 299F C265 DD04 7930  70EB 944D 35F9 AC3D B76A"),
                filenameAndFingerprint("glozow.gpg", "6B00 2C6E A3F9 1B1B 0DF0  C9BC 8F61 7F12 00A6 D25C"),
                filenameAndFingerprint("guggero.gpg", "F4FC 70F0 7310 0284 24EF  C20A 8E42 5659 3F17 7720"),
                filenameAndFingerprint("hebasto.gpg", "D1DB F2C4 B96F 2DEB F4C1  6654 4101 0811 2E7E A81F"),
                filenameAndFingerprint("jackielove4u.gpg", "287A E4CA 1187 C68C 08B4  9CB2 D11B D4F3 3F1D B499"),
                filenameAndFingerprint("josibake.gpg", "6165 16B8 EB6E D028 82FC  4A7A 8ADC B558 C4F3 3D65"),
                filenameAndFingerprint("kvaciral.gpg", "C388 F696 1FB9 72A9 5678  E327 F627 11DB DCA8 AE56"),
                filenameAndFingerprint("laanwj.gpg", "71A3 B167 3540 5025 D447  E8F2 7481 0B01 2346 C9A6"),
                filenameAndFingerprint("luke-jr.gpg", "1A3E 761F 19D2 CC77 85C5  502E A291 A2C4 5D0C 504A"),
                filenameAndFingerprint("m3dwards.gpg", "E86A E734 3962 5BBE E306  AAE6 B66D 427F 873C B1A3"),
                filenameAndFingerprint("pinheadmz.gpg", "E617 73CD 6E01 040E 2F1B  D78C E7E2 984B 6289 C93A"),
                filenameAndFingerprint("satsie.gpg", "2F78 ACF6 7702 9767 C873  6F13 747A 7AE2 FB0F D25B"),
                filenameAndFingerprint("sipa.gpg", "133E AC17 9436 F14A 5CF1  B794 860F EB80 4E66 9320"),
                filenameAndFingerprint("Sjors.gpg", "ED9B DF7A D6A5 5E23 2E84  5242 57FF 9BDB CC30 1009"),
                filenameAndFingerprint("svanstaa.gpg", "9ED9 9C7A 355A E460 9810  3E74 476E 74C8 529A 9006"),
                filenameAndFingerprint("TheCharlatan.gpg", "A8FC 55F3 B04B A314 6F34  92E7 9303 B33A 3052 24CB"),
                filenameAndFingerprint("theStack.gpg", "6A8F 9C26 6528 E25A EB1D  7731 C237 1D91 CB71 6EA7"),
                filenameAndFingerprint("vertiond.gpg", "28E7 2909 F171 7FE9 6077  54F8 A7BE B262 1678 D37D"),
                filenameAndFingerprint("willcl-ark.gpg", "67AA 5B46 E7AF 7805 3167  FE34 3B8F 814A 7842 18F8"),
                filenameAndFingerprint("willyko.gpg", "79D0 0BAC 68B5 6D42 2F94  5A8F 8E3A 8F32 47DB CBBF"),
            )
        )
        hashFileDownloader.registerTasks()

        val binaryDownloadUrl: Provider<String> = extension.version.map { BitcoinCoreBinaryUrlProvider(it).url }
        val downloadTaskFactory = DownloadTaskFactory(project, DOWNLOADS_DIR)
        val binaryDownloadTask = downloadTaskFactory
            .registerDownloadTask("downloadBitcoinCore", binaryDownloadUrl)

        val bitcoinCoreHashVerificationTask: TaskProvider<HashVerificationTask> =
            project.tasks.register<HashVerificationTask>("verifyBitcoinCore") {
                inputFile.set(binaryDownloadTask.flatMap { it.outputFile })
                sha256File.set(hashFileDownloader.verifySignatureTask.flatMap { it.fileToVerify })
                resultFile.set(project.layout.buildDirectory.file("${DOWNLOADS_DIR}/bitcoin_core.sha256.result"))
            }

        val unpackBitcoinCoreArchiveTask: TaskProvider<Copy> =
            project.tasks.register<Copy>("unpackBitcoinCoreArchive") {
                dependsOn(bitcoinCoreHashVerificationTask)

                val inputFile = bitcoinCoreHashVerificationTask.flatMap { it.inputFile }
                val sourceFile = inputFile.map {
                    if (it.asFile.name.endsWith(".tar.gz")) {
                        project.tarTree(it.asFile.absolutePath)
                    } else {
                        project.zipTree(it.asFile.absolutePath)
                    }
                }
                from(sourceFile)

                into(project.layout.buildDirectory.dir("${DOWNLOADS_DIR}/extracted"))
            }

        val copyBitcoinCoreToResourcesTask: TaskProvider<Copy> =
            project.tasks.register<Copy>("copyBitcoinCoreToResources") {
                dependsOn(unpackBitcoinCoreArchiveTask)
                val sourceDirectory = extension.version.map { version ->
                    project.layout.buildDirectory.dir("${DOWNLOADS_DIR}/extracted/bitcoin-$version/bin/")
                }
                from(sourceDirectory)
                include("bitcoind*")
                into(project.layout.buildDirectory.dir("generated/src/main/resources"))
            }

        val processResourcesTask = project.tasks.named("processResources")
        processResourcesTask.configure {
            dependsOn(copyBitcoinCoreToResourcesTask)
        }
    }

    private fun filenameAndFingerprint(filename: String, fingerprint: String) =
        Pair(
            PgpFingerprint.normalize(fingerprint),
            this::class.java.getResource("/$filename")!!
        )
}