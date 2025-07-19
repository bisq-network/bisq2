package bisq.wallets.core;

import lombok.Getter;

@Getter
public class WalletLoadException extends WalletException {
    public WalletLoadException() {
        super("Failed to load wallet");
    }

    public WalletLoadException(String message) {
        super(message);
    }
}

