package bisq.account.accounts.stable_coin;

import bisq.account.payment_method.stable_coin.StableCoinPaymentMethod;
import bisq.account.payment_method.stable_coin.StableCoinPaymentMethodSpec;
import bisq.account.payment_method.stable_coin.StableCoinPaymentRail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

    // --- T1: Account rail filtering tests ---

    @Nested
    @DisplayName("Account rail filtering")
    class AccountRailFiltering {

        @Test
        @DisplayName("T1a: Polygon payload matches USDC_POLYGON spec; Ethereum does not")
        void polygon_payload_matches_polygon_spec_only() {
            StableCoinAccountPayload polygonPayload = new StableCoinAccountPayload("p1", "USDC", VALID_ADDRESS, "POLYGON");
            StableCoinAccountPayload ethereumPayload = new StableCoinAccountPayload("p2", "USDC", VALID_ADDRESS, "ETHEREUM");
            StableCoinPaymentMethodSpec polygonSpec = new StableCoinPaymentMethodSpec(
                    StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDC_POLYGON));

            assertEquals(StableCoinPaymentRail.USDC_POLYGON, polygonPayload.getPaymentMethod().getPaymentRail());
            assertEquals(polygonSpec.getPaymentMethod().getPaymentRail(), polygonPayload.getPaymentMethod().getPaymentRail());
            assertNotEquals(polygonSpec.getPaymentMethod().getPaymentRail(), ethereumPayload.getPaymentMethod().getPaymentRail());
        }

        @Test
        @DisplayName("T1b: ERC20 payload matches USDC_ERC20 spec; Polygon does not")
        void erc20_payload_matches_erc20_spec_only() {
            StableCoinAccountPayload polygonPayload = new StableCoinAccountPayload("p1", "USDC", VALID_ADDRESS, "POLYGON");
            StableCoinAccountPayload ethereumPayload = new StableCoinAccountPayload("p2", "USDC", VALID_ADDRESS, "ETHEREUM");
            StableCoinPaymentMethodSpec erc20Spec = new StableCoinPaymentMethodSpec(
                    StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDC_ERC20));

            assertEquals(erc20Spec.getPaymentMethod().getPaymentRail(), ethereumPayload.getPaymentMethod().getPaymentRail());
            assertNotEquals(erc20Spec.getPaymentMethod().getPaymentRail(), polygonPayload.getPaymentMethod().getPaymentRail());
        }

        @Test
        @DisplayName("T1c: Stablecoin rail enum is never equal to null or other enum types")
        void stablecoin_rail_differs_across_networks() {
            StableCoinAccountPayload polygonPayload = new StableCoinAccountPayload("p1", "USDC", VALID_ADDRESS, "POLYGON");
            StableCoinAccountPayload ethereumPayload = new StableCoinAccountPayload("p2", "USDC", VALID_ADDRESS, "ETHEREUM");

            assertNotEquals(polygonPayload.getPaymentMethod().getPaymentRail(),
                    ethereumPayload.getPaymentMethod().getPaymentRail());
            assertNotNull(polygonPayload.getPaymentMethod().getPaymentRail());
        }
    }

    // --- T2: Display string with network ---

    @Nested
    @DisplayName("Display string with network label")
    class DisplayStringWithNetwork {

        @Test
        @DisplayName("T2a: display string contains address and network name")
        void display_string_contains_address_and_network() {
            StableCoinAccountPayload payload = new StableCoinAccountPayload("test-id", "USDC", VALID_ADDRESS, "POLYGON");
            String display = payload.getAccountDataDisplayString();

            assertTrue(display.contains(VALID_ADDRESS), "Should contain the address");
            assertTrue(display.contains("Polygon"), "Should contain the network display name");
        }

        @Test
        @DisplayName("T2b: different networks produce different display strings for same address")
        void different_networks_produce_different_display_strings() {
            StableCoinAccountPayload polygonPayload = new StableCoinAccountPayload("p1", "USDC", VALID_ADDRESS, "POLYGON");
            StableCoinAccountPayload ethereumPayload = new StableCoinAccountPayload("p2", "USDC", VALID_ADDRESS, "ETHEREUM");

            assertNotEquals(polygonPayload.getAccountDataDisplayString(), ethereumPayload.getAccountDataDisplayString());
            assertTrue(polygonPayload.getAccountDataDisplayString().contains("Polygon"));
            assertTrue(ethereumPayload.getAccountDataDisplayString().contains("Ethereum"));
        }
    }
}
