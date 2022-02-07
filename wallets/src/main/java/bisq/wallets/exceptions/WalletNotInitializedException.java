package bisq.wallets.exceptions;

public class WalletNotInitializedException extends RuntimeException {
    public WalletNotInitializedException() {
        super();
    }

    public WalletNotInitializedException(String message) {
        super(message);
    }

    public WalletNotInitializedException(String message, Throwable cause) {
        super(message, cause);
    }
}
