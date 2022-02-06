package bisq.wallets.bitcoind.rpc;

public class RpcCallFailureException extends RuntimeException {
    public RpcCallFailureException(String message) {
        super(message);
    }

    public RpcCallFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
