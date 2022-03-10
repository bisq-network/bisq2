package bisq.gradle.tasks.bitcoind

import bisq.gradle.bitcoind.BitcoindRpcClient
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class BitcoindCreateOrLoadWalletTask : DefaultTask() {
    @get:Input
    abstract val walletDirectory: DirectoryProperty

    @TaskAction
    fun create() {
        val walletFile = walletDirectory.get().asFile
        if (!walletFile.exists()) {
            createWallet(walletFile)
        } else {
            loadWallet(walletFile)
        }

        setPassphrase(walletFile)
    }

    private fun createWallet(walletFile: File) {
        BitcoindRpcClient.daemonRpcCall(
            listOf(
                "--named",
                "createwallet",

                "wallet_name=${walletFile.absolutePath}",
                "passphrase=bisq"
            )
        )
    }

    private fun loadWallet(walletFile: File) {
        BitcoindRpcClient.daemonRpcCall(
            listOf(
                "loadwallet",
                walletFile.absolutePath,
            )
        )
    }

    private fun setPassphrase(walletFile: File) {
        BitcoindRpcClient.walletRpcCall(
            walletPath = walletFile.absolutePath,
            listOf(
                "walletpassphrase",
                "bisq",
                "10000000"
            )
        )
    }
}