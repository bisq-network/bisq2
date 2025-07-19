package bisq.wallets.core;

import lombok.Getter;

@Getter
public class WalletSeedException extends WalletException {
    public WalletSeedException() {
        super("Wallet seed Exception");
    }

    public WalletSeedException(String message) {
        super(message);
    }
}
