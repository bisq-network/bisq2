package network.misq.wallets;

public enum NetworkType {
    MAINNET(8332),
    TESTNET(18332),
    SIGNET(38332),
    REGTEST(18443);

    private final int rpcPort;

    NetworkType(int rpcPort) {
        this.rpcPort = rpcPort;
    }

    public int getRpcPort() {
        return rpcPort;
    }
}
