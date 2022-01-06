package bisq.wallets.bitcoind;

public enum BitcoindRpcEndpoint {
    ADD_MULTISIG_ADDRESS("addmultisigaddress"),
    CREATE_WALLET("createwallet"),
    FINALIZE_PSBT("finalizepsbt"),
    GENERATE_TO_ADDRESS("generatetoaddress"),
    GET_ADDRESS_INFO("getaddressinfo"),
    GET_BALANCE("getbalance"),
    GET_NEW_ADDRESS("getnewaddress"),
    IMPORT_ADDRESS("importaddress"),
    LIST_TRANSACTIONS("listtransactions"),
    LIST_UNSPENT("listunspent"),
    LOAD_WALLET("loadwallet"),
    SEND_RAW_TRANSACTION("sendrawtransaction"),
    SEND_TO_ADDRESS("sendtoaddress"),
    STOP("stop"),
    SIGN_MESSAGE("signmessage"),
    UNLOAD_WALLET("unloadwallet"),
    VERIFY_MESSAGE("verifymessage"),
    WALLET_CREATE_FUNDED_PSBT("walletcreatefundedpsbt"),
    WALLET_PASSPHRASE("walletpassphrase"),
    WALLET_PROCESS_PSBT("walletprocesspsbt");

    private final String methodName;

    BitcoindRpcEndpoint(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }
}
