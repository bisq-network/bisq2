package bisq.account.accounts.stable_coin;

import bisq.account.accounts.Account;
import bisq.account.accounts.AccountOrigin;
import bisq.account.timestamp.KeyType;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

class StableCoinAccountTest {

    private static final String VALID_ADDRESS = "0x742d35Cc6634C0532925a3b844Bc9e7595f2bD08";

    private StableCoinAccount createTestAccount() throws GeneralSecurityException {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        StableCoinAccountPayload payload = new StableCoinAccountPayload("test-id", "USDC", VALID_ADDRESS, "POLYGON");
        return new StableCoinAccount(
                "test-id",
                System.currentTimeMillis(),
                "My USDC Polygon",
                payload,
                keyPair,
                KeyType.EC,
                AccountOrigin.BISQ2_NEW);
    }

    @Test
    @DisplayName("account proto roundtrip preserves all fields")
    void account_proto_roundtrip() throws GeneralSecurityException {
        StableCoinAccount account = createTestAccount();

        bisq.account.protobuf.Account proto = account.toProto(false);
        Account<?, ?> restored = Account.fromProto(proto);

        assertInstanceOf(StableCoinAccount.class, restored);
        StableCoinAccount restoredAccount = (StableCoinAccount) restored;

        assertEquals(account.getId(), restoredAccount.getId());
        assertEquals(account.getAccountName(), restoredAccount.getAccountName());
        assertEquals(account.getCreationDate(), restoredAccount.getCreationDate());

        StableCoinAccountPayload originalPayload = account.getAccountPayload();
        StableCoinAccountPayload restoredPayload = restoredAccount.getAccountPayload();
        assertEquals(originalPayload.getCurrencyCode(), restoredPayload.getCurrencyCode());
        assertEquals(originalPayload.getAddress(), restoredPayload.getAddress());
        assertEquals(originalPayload.getNetwork(), restoredPayload.getNetwork());
    }

    @Test
    @DisplayName("Account.fromProto dispatches to StableCoinAccount")
    void account_from_proto_dispatch() throws GeneralSecurityException {
        StableCoinAccount account = createTestAccount();

        bisq.account.protobuf.Account proto = account.toProto(false);
        Account<?, ?> restored = Account.fromProto(proto);

        assertInstanceOf(StableCoinAccount.class, restored);
    }
}
