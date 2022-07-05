package bisq.gradle.desktop.regtest.bitcoind

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

open class BitcoindRegtestConfig(
    private val project: Project,
    protected val regtestDir: String
) {
    val bitcoindDataDir: Provider<Directory>
        get() = createDirProperty("$regtestDir/bitcoind")

    val bisqDataDir: Provider<Directory>
        get() = createDirProperty(bisqDataDirAsString)

    protected val bisqDataDirAsString: String
        get() = "$regtestDir/bisq"

    val bitcoindWalletDir: Provider<Directory>
        get() = createDirProperty("$bisqDataDirAsString/wallets/bitcoind")

    protected fun createDirProperty(path: String): Provider<Directory> =
        project.layout.buildDirectory.dir(path)
}