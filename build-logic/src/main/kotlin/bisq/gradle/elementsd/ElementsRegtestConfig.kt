package bisq.gradle.elementsd

import bisq.gradle.bitcoind.BitcoindRegtestConfig
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

class ElementsRegtestConfig(project: Project, regtestDir: String) : BitcoindRegtestConfig(project, regtestDir) {
    val elementsdDataDir: Provider<Directory>
        get() = createDirProperty("$regtestDir/elementsd")

    val elementsdWalletDir: Provider<Directory>
        get() = createDirProperty("$bisqDataDirAsString/wallets/elementsd")
}