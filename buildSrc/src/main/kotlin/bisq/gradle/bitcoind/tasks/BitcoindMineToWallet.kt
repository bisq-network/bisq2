package bisq.gradle.bitcoind.tasks

import bisq.gradle.bitcoind.BitcoindRpcClient
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class BitcoindMineToWallet : DefaultTask() {
    @get:Input
    abstract val walletDirectory: Property<File>

    @get:Input
    abstract val numberOfBlocks: Property<Int>

    init {
        numberOfBlocks.convention(1)
    }

    @TaskAction
    fun mine() {
        BitcoindRpcClient.walletRpcCall(
            walletPath = walletDirectory.get().absolutePath,
            args = listOf(
                "-generate", numberOfBlocks.get().toString()
            )
        )
    }
}