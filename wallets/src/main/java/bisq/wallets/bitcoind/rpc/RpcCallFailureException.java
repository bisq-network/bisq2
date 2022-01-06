package bisq.wallets.bitcoind.rpc;

public class RpcCallFailureException extends Exception {
    public RpcCallFailureException(String message) {
        super(message);
    }

    public RpcCallFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
