package bisq.gradle.desktop.regtest.elementsd

object ElementsdRpcClient {
    private val cmdArgs = listOf(
        "elements-cli",
        "-rpcwait",
        "-chain=elementsregtest",

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