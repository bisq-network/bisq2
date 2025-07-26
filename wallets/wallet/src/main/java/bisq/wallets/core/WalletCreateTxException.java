package bisq.wallets.core;

import lombok.Getter;

@Getter
public class WalletCreateTxException extends WalletException {
    public WalletCreateTxException() {
        super("Failed to create transaction");
    }

    public WalletCreateTxException(String message) {
        super(message);
    }
}
