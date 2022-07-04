package bisq.gradle.desktop.regtest.bitcoind

object BitcoindRpcClient {
    private val cmdArgs = listOf(
        "bitcoin-cli",
        "-regtest",
        "-rpcwait",
        "-rpcuser=bisq",
        "-rpcpassword=bisq"
    )

    fun daemonRpcCall(args: List<String>): String {
        val process = ProcessBuilder(cmdArgs + args)
            .start()
        process.waitFor()

        return process.inputStream
            .bufferedReader()
            .readText()
    }

    fun walletRpcCall(walletPath: String, args: List<String>): String {
        return daemonRpcCall(listOf("-rpcwallet=$walletPath") + args)
    }
}