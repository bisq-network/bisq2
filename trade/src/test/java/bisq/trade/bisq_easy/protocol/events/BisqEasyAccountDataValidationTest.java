package bisq.trade.bisq_easy.protocol.events;

import bisq.account.accounts.stable_coin.StableCoinAccountPayload;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.payment_method.stable_coin.StableCoinPaymentMethod;
import bisq.account.payment_method.stable_coin.StableCoinPaymentMethodSpec;
import bisq.account.payment_method.stable_coin.StableCoinPaymentRail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the sender-side rail validation logic used by
 * {@link BisqEasyAccountDataEventHandler#validateRailMatch()}.
 * Uses {@link StableCoinAccountPayload#containsNetworkTag} -- the same method
 * the handler calls -- so the test stays in sync with production code.
 */
class BisqEasyAccountDataValidationTest {

    private static final String POLYGON_ADDRESS = "0x742d35Cc6634C0532925a3b844Bc9e7595f2bD08";

    /**
     * Delegates to the same shared utility that the handler uses, ensuring
     * the test validates the real logic rather than a hand-written copy.
     */
    private static boolean isValidForSpec(Object quoteSideSpec, String paymentAccountData) {
        if (quoteSideSpec instanceof StableCoinPaymentMethodSpec stableCoinSpec) {
            String expectedNetwork = stableCoinSpec.getPaymentMethod().getNetwork().getDisplayName();
            return StableCoinAccountPayload.containsNetworkTag(paymentAccountData, expectedNetwork);
        }
        return true;
    }

    @Nested
    @DisplayName("T5: Sender-side validation")
    class SenderSideValidation {

        @Test
        @DisplayName("T5a: USDC_ERC20 data rejected for USDC_POLYGON contract")
        void wrong_rail_rejected() {
            StableCoinPaymentMethodSpec polygonSpec = new StableCoinPaymentMethodSpec(
                    StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDC_POLYGON));

            String ethereumData = POLYGON_ADDRESS + " (Ethereum)";

            assertFalse(isValidForSpec(polygonSpec, ethereumData),
                    "Ethereum account data must not pass validation for Polygon contract");
        }

        @Test
        @DisplayName("T5b: USDC_POLYGON data accepted for USDC_POLYGON contract")
        void correct_rail_accepted() {
            StableCoinPaymentMethodSpec polygonSpec = new StableCoinPaymentMethodSpec(
                    StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDC_POLYGON));

            String polygonData = POLYGON_ADDRESS + " (Polygon)";

            assertTrue(isValidForSpec(polygonSpec, polygonData),
                    "Polygon account data should pass validation for Polygon contract");
        }

        @Test
        @DisplayName("T5c: fiat contract skips stablecoin validation entirely")
        void fiat_contract_skips_stablecoin_check() {
            FiatPaymentMethodSpec fiatSpec = new FiatPaymentMethodSpec(
                    FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA));

            assertTrue(isValidForSpec(fiatSpec, "Some SEPA bank data"),
                    "Fiat spec should always pass (no stablecoin validation)");
        }

        @Test
        @DisplayName("T5a-extra: null payment data rejected for stablecoin contract")
        void null_data_rejected_for_stablecoin() {
            StableCoinPaymentMethodSpec polygonSpec = new StableCoinPaymentMethodSpec(
                    StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDC_POLYGON));

            assertFalse(isValidForSpec(polygonSpec, null),
                    "Null data must not pass validation");
        }
    }
}
