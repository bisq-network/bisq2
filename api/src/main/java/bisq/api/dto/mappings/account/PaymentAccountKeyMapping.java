package bisq.api.dto.mappings.account;

import bisq.account.timestamp.KeyType;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public final class PaymentAccountKeyMapping {
    private PaymentAccountKeyMapping() {
    }

    public static KeyData createDefault() {
        return new KeyData(KeyGeneration.generateDefaultEcKeyPair(), KeyType.EC);
    }

    public record KeyData(KeyPair keyPair, KeyType keyType) {
    }
}
