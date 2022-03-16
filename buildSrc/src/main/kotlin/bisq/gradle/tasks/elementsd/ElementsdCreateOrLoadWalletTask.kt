package bisq.gradle.tasks.elementsd

import bisq.gradle.elementsd.ElementsdRpcClient
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class ElementsdCreateOrLoadWalletTask : DefaultTask() {
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
        ElementsdRpcClient.daemonRpcCall(
            listOf(
                "--named",
                "createwallet",

                "wallet_name=${walletFile.absolutePath}",
                "passphrase=bisq"
            )
        )
    }

    private fun loadWallet(walletFile: File) {
        ElementsdRpcClient.daemonRpcCall(
            listOf(
                "loadwallet",
                walletFile.absolutePath,
            )
        )
    }

    private fun setPassphrase(walletFile: File) {
        ElementsdRpcClient.walletRpcCall(
            walletPath = walletFile.absolutePath,
            listOf(
                "walletpassphrase",
                "bisq",
                "10000000"
            )
        )
    }
}