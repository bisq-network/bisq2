package bisq.gradle.bitcoind.tasks

import bisq.gradle.bitcoind.BitcoindRpcClient
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class BitcoindMineToWallet : DefaultTask() {
    @get:Input
    abstract val walletDirectory: DirectoryProperty

    @get:Input
    abstract val numberOfBlocks: Property<Int>

    init {
        numberOfBlocks.convention(1)
    }

    @TaskAction
    fun mine() {
        BitcoindRpcClient.walletRpcCall(
            walletPath = walletDirectory.get().asFile.absolutePath,
            args = listOf(
                "-generate", numberOfBlocks.get().toString()
            )
        )
    }
}