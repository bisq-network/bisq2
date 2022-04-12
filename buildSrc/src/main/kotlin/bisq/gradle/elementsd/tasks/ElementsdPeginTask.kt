package bisq.gradle.elementsd.tasks

import bisq.gradle.bitcoind.BitcoindRpcClient
import bisq.gradle.elementsd.ElementsdRpcClient
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class ElementsdPeginTask : DefaultTask() {

    @get:Input
    abstract val bitcoindWalletDirectory: DirectoryProperty

    @get:Input
    abstract val peginWalletDirectory: DirectoryProperty

    @TaskAction
    fun pegin() {
        val peginAddress = getPeginAddress()
        val bitcoinTxId = fundAddress(peginAddress)

        mineBitcoinBlocks(1)
        val rawBitcoinTransaction = getRawTransaction(bitcoinTxId)

        // main chain tx needs 102 confirmations for pegin
        mineBitcoinBlocks(101)

        val txOutProof = getTxOutProof(bitcoinTxId)
        mineElementsBlocks(1)

        claimPegin(rawBitcoinTransaction, txOutProof)
        mineElementsBlocks(1)
    }

    private fun getPeginAddress(): String {
        val getPeginAddressResponse = ElementsdRpcClient.walletRpcCall(
            walletPath = peginWalletDirectory.get().asFile.absolutePath,
            args = listOf("getpeginaddress")
        )

        /*
        Response:
        {
          "mainchain_address": "2N3i4C56DiqfpdcAJsAdZd2xYpCQMRAroye",
          "claim_script": "0014b515db1688fa148308eb723e00ff8f7913fdcfdb"
        }
         */

        return getPeginAddressResponse.lines()
            .filter { line -> line.contains("mainchain_address") }[0]
            .split("\": \"")[1]
            .replace("\",", "")
    }

    private fun fundAddress(address: String): String {
        return BitcoindRpcClient.walletRpcCall(
            walletPath = bitcoindWalletDirectory.get().asFile.absolutePath,
            args = listOf(
                "sendtoaddress",
                address,
                20.toString()
            )
        ).trim()
    }

    private fun mineBitcoinBlocks(numberOfBlocks: Int) {
        BitcoindRpcClient.walletRpcCall(
            walletPath = bitcoindWalletDirectory.get().asFile.absolutePath,
            args = listOf(
                "-generate",
                numberOfBlocks.toString()
            )
        )
    }

    private fun mineElementsBlocks(numberOfBlocks: Int) {
        ElementsdRpcClient.walletRpcCall(
            walletPath = peginWalletDirectory.get().asFile.absolutePath,
            args = listOf(
                "-generate",
                numberOfBlocks.toString()
            )
        )
    }

    private fun getRawTransaction(txId: String): String {
        return BitcoindRpcClient.daemonRpcCall(
            listOf("getrawtransaction", txId)
        ).trim()
    }

    private fun getTxOutProof(txId: String): String {
        return BitcoindRpcClient.daemonRpcCall(
            listOf("gettxoutproof", "[\"$txId\"]")
        ).trim()
    }

    private fun claimPegin(rawBitcoindTx: String, txOutProof: String): String {
        return ElementsdRpcClient.walletRpcCall(
            walletPath = peginWalletDirectory.get().asFile.absolutePath,
            args = listOf("claimpegin", rawBitcoindTx, txOutProof)
        ).trim()
    }
}