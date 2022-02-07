package bisq.wallets.bitcoind.rpc;

public class WalletInitializationFailedException extends RuntimeException {
    public WalletInitializationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
