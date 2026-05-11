package bisq.account.accounts.stable_coin;

import bisq.account.payment_method.stable_coin.StableCoinPaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StableCoinAccountPayloadTest {

    private static final String VALID_ADDRESS = "0x742d35Cc6634C0532925a3b844Bc9e7595f2bD08";

    @Test
    @DisplayName("valid EVM address passes validation")
    void valid_evm_address_passes_validation() {
        assertTrue(StableCoinAccountPayload.isValidEvmAddress(VALID_ADDRESS));
    }

    @Test
    @DisplayName("invalid EVM addresses are rejected")
    void invalid_evm_address_rejected() {
        assertFalse(StableCoinAccountPayload.isValidEvmAddress(null));
        assertFalse(StableCoinAccountPayload.isValidEvmAddress(""));
        assertFalse(StableCoinAccountPayload.isValidEvmAddress("742d35Cc6634C0532925a3b844Bc9e7595f2bD08"));
        assertFalse(StableCoinAccountPayload.isValidEvmAddress("0x742d35Cc6634C0532925a3b844Bc9e7595f2bD"));
        assertFalse(StableCoinAccountPayload.isValidEvmAddress("0x742d35Cc6634C0532925a3b844Bc9e7595f2bD0G"));
        assertFalse(StableCoinAccountPayload.isValidEvmAddress("0x742d35Cc6634C0532925a3b844Bc9e7595f2bD0800"));
    }

    @Test
    @DisplayName("payload creation with valid data exposes correct getters")
    void payload_creation_with_valid_data() {
        StableCoinAccountPayload payload = new StableCoinAccountPayload("test-id", "USDC", VALID_ADDRESS, "POLYGON");

        assertEquals("USDC", payload.getCurrencyCode());
        assertEquals(VALID_ADDRESS, payload.getAddress());
        assertEquals("POLYGON", payload.getNetwork());
        assertEquals("test-id", payload.getId());
    }

    @Test
    @DisplayName("getAccountDataDisplayString returns the address")
    void getAccountDataDisplayString_returns_address() {
        StableCoinAccountPayload payload = new StableCoinAccountPayload("test-id", "USDC", VALID_ADDRESS, "POLYGON");
        assertEquals(VALID_ADDRESS, payload.getAccountDataDisplayString());
    }

    @Test
    @DisplayName("getPaymentMethod returns matching rail for USDC on POLYGON")
    void getPaymentMethod_returns_matching_rail() {
        StableCoinAccountPayload payload = new StableCoinAccountPayload("test-id", "USDC", VALID_ADDRESS, "POLYGON");
        StableCoinPaymentMethod method = payload.getPaymentMethod();

        assertNotNull(method);
        assertEquals("USDC_POLYGON", method.getPaymentRailName());
    }

    @Test
    @DisplayName("proto roundtrip preserves all fields")
    void proto_roundtrip() {
        StableCoinAccountPayload payload = new StableCoinAccountPayload("test-id", "USDC", VALID_ADDRESS, "POLYGON");

        bisq.account.protobuf.AccountPayload proto = payload.toProto(false);
        StableCoinAccountPayload restored = StableCoinAccountPayload.fromProto(proto);

        assertEquals(payload.getId(), restored.getId());
        assertEquals(payload.getCurrencyCode(), restored.getCurrencyCode());
        assertEquals(payload.getAddress(), restored.getAddress());
        assertEquals(payload.getNetwork(), restored.getNetwork());
        assertArrayEquals(payload.getSalt(), restored.getSalt());
    }

    @Test
    @DisplayName("constructor rejects empty address")
    void constructor_rejects_empty_address() {
        assertThrows(IllegalArgumentException.class,
                () -> new StableCoinAccountPayload("test-id", "USDC", "", "POLYGON"));
    }

    @Test
    @DisplayName("constructor rejects empty currency code")
    void constructor_rejects_empty_currency_code() {
        assertThrows(RuntimeException.class,
                () -> new StableCoinAccountPayload("test-id", "", VALID_ADDRESS, "POLYGON"));
    }
}
