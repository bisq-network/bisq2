/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.account.payment_method;

import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.account.payment_method.crypto.CryptoPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.payment_method.stable_coin.StableCoinPaymentMethod;
import bisq.account.payment_method.stable_coin.StableCoinPaymentMethodSpec;
import bisq.account.payment_method.stable_coin.StableCoinPaymentRail;
import bisq.common.market.Market;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaymentMethodSpecUtilTest {

    private static final Market BTC_USD = new Market("BTC", "USD", "Bitcoin", "US Dollar");
    private static final Market XMR_BTC = new Market("XMR", "BTC", "Monero", "Bitcoin");
    private static final Market BTC_USDT = new Market("BTC", "USDT", "Bitcoin", "Tether USD");

    @Test
    @DisplayName("create payment method specs for fiat returns fiat specs")
    void create_payment_method_specs_for_fiat_returns_fiat_specs() {
        FiatPaymentMethod sepa = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA);
        List<PaymentMethod<?>> methods = List.of(sepa);
        List<PaymentMethodSpec<?>> specs = PaymentMethodSpecUtil.createPaymentMethodSpecs(methods, "USD");
        assertEquals(1, specs.size());
        assertInstanceOf(FiatPaymentMethodSpec.class, specs.get(0));
    }

    @Test
    @DisplayName("create payment method specs for altcoin returns crypto specs")
    void create_payment_method_specs_for_altcoin_returns_crypto_specs() {
        CryptoPaymentMethod xmr = new CryptoPaymentMethod("XMR");
        List<PaymentMethod<?>> methods = List.of(xmr);
        List<PaymentMethodSpec<?>> specs = PaymentMethodSpecUtil.createPaymentMethodSpecs(methods, "XMR");
        assertEquals(1, specs.size());
        assertInstanceOf(CryptoPaymentMethodSpec.class, specs.get(0));
    }

    @Test
    @DisplayName("create payment method specs for stablecoin code filters out non crypto")
    void create_payment_method_specs_for_stablecoin_code_filters_out_non_crypto() {
        // USDT is classified as altcoin by Asset.isAltcoin, so altcoin branch is entered.
        // But a FiatPaymentMethod is not CryptoPaymentMethod, so it gets filtered out.
        FiatPaymentMethod sepa = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA);
        List<PaymentMethod<?>> methods = List.of(sepa);
        List<PaymentMethodSpec<?>> specs = PaymentMethodSpecUtil.createPaymentMethodSpecs(methods, "USDT");
        assertTrue(specs.isEmpty());
    }

    @Test
    @DisplayName("create bitcoin payment method specs creates correctly")
    void create_bitcoin_payment_method_specs_creates_correctly() {
        BitcoinPaymentMethod mainChain = BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN);
        List<BitcoinPaymentMethodSpec> specs = PaymentMethodSpecUtil.createBitcoinPaymentMethodSpecs(List.of(mainChain));
        assertEquals(1, specs.size());
        assertEquals(mainChain, specs.get(0).getPaymentMethod());
    }

    @Test
    @DisplayName("create bitcoin main chain payment method spec returns main chain")
    void create_bitcoin_main_chain_payment_method_spec_returns_main_chain() {
        List<BitcoinPaymentMethodSpec> specs = PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec();
        assertEquals(1, specs.size());
        assertEquals("MAIN_CHAIN", specs.get(0).getPaymentMethod().getPaymentRail().name());
    }

    @Test
    @DisplayName("create fiat payment method specs creates correctly")
    void create_fiat_payment_method_specs_creates_correctly() {
        FiatPaymentMethod sepa = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA);
        FiatPaymentMethod zelle = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ZELLE);
        List<FiatPaymentMethodSpec> specs = PaymentMethodSpecUtil.createFiatPaymentMethodSpecs(List.of(sepa, zelle));
        assertEquals(2, specs.size());
    }

    @Test
    @DisplayName("get payment method spec class for base side btc fiat")
    void get_payment_method_spec_class_for_base_side_btc_fiat() {
        assertEquals(BitcoinPaymentMethodSpec.class,
                PaymentMethodSpecUtil.getPaymentMethodSpecClassForBaseSide(BTC_USD));
    }

    @Test
    @DisplayName("get payment method spec class for base side crypto")
    void get_payment_method_spec_class_for_base_side_crypto() {
        assertEquals(CryptoPaymentMethodSpec.class,
                PaymentMethodSpecUtil.getPaymentMethodSpecClassForBaseSide(XMR_BTC));
    }

    @Test
    @DisplayName("get payment method spec class for base side throws for stablecoin market")
    void get_payment_method_spec_class_for_base_side_throws_for_stablecoin_market() {
        assertThrows(IllegalArgumentException.class,
                () -> PaymentMethodSpecUtil.getPaymentMethodSpecClassForBaseSide(BTC_USDT));
    }

    @Test
    @DisplayName("get payment method spec class for quote side btc fiat")
    void get_payment_method_spec_class_for_quote_side_btc_fiat() {
        assertEquals(FiatPaymentMethodSpec.class,
                PaymentMethodSpecUtil.getPaymentMethodSpecClassForQuoteSide(BTC_USD));
    }

    @Test
    @DisplayName("get payment method spec class for quote side crypto")
    void get_payment_method_spec_class_for_quote_side_crypto() {
        assertEquals(BitcoinPaymentMethodSpec.class,
                PaymentMethodSpecUtil.getPaymentMethodSpecClassForQuoteSide(XMR_BTC));
    }

    @Test
    @DisplayName("get payment method spec class for quote side throws for stablecoin market")
    void get_payment_method_spec_class_for_quote_side_throws_for_stablecoin_market() {
        assertThrows(IllegalArgumentException.class,
                () -> PaymentMethodSpecUtil.getPaymentMethodSpecClassForQuoteSide(BTC_USDT));
    }

    @Test
    @DisplayName("get payment methods extracts from specs")
    void get_payment_methods_extracts_from_specs() {
        FiatPaymentMethod sepa = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA);
        FiatPaymentMethodSpec spec = new FiatPaymentMethodSpec(sepa);
        List<FiatPaymentMethod> methods = PaymentMethodSpecUtil.getPaymentMethods(List.of(spec));
        assertEquals(1, methods.size());
        assertEquals(sepa, methods.get(0));
    }

    @Test
    @DisplayName("get payment method names extracts names")
    void get_payment_method_names_extracts_names() {
        FiatPaymentMethod sepa = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA);
        FiatPaymentMethodSpec spec = new FiatPaymentMethodSpec(sepa);
        List<String> names = PaymentMethodSpecUtil.getPaymentMethodNames(List.of(spec));
        assertEquals(1, names.size());
        assertEquals("SEPA", names.get(0));
    }

    @Test
    @DisplayName("create quote side payment method specs for fiat returns fiat specs")
    void create_quote_side_specs_for_fiat_returns_fiat_specs() {
        FiatPaymentMethod sepa = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA);
        List<PaymentMethodSpec<?>> specs = PaymentMethodSpecUtil.createQuoteSidePaymentMethodSpecs(List.of(sepa));
        assertEquals(1, specs.size());
        assertInstanceOf(FiatPaymentMethodSpec.class, specs.get(0));
        assertEquals(sepa, specs.get(0).getPaymentMethod());
    }

    @Test
    @DisplayName("create quote side payment method specs for stablecoin returns stablecoin specs")
    void create_quote_side_specs_for_stablecoin_returns_stablecoin_specs() {
        StableCoinPaymentMethod usdcPolygon = StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDC_POLYGON);
        List<PaymentMethodSpec<?>> specs = PaymentMethodSpecUtil.createQuoteSidePaymentMethodSpecs(List.of(usdcPolygon));
        assertEquals(1, specs.size());
        assertInstanceOf(StableCoinPaymentMethodSpec.class, specs.get(0));
        assertEquals(usdcPolygon, specs.get(0).getPaymentMethod());
    }

    @Test
    @DisplayName("create quote side payment method specs for mixed list")
    void create_quote_side_specs_for_mixed_list() {
        FiatPaymentMethod sepa = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA);
        StableCoinPaymentMethod usdcPolygon = StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDC_POLYGON);
        List<PaymentMethodSpec<?>> specs = PaymentMethodSpecUtil.createQuoteSidePaymentMethodSpecs(List.of(sepa, usdcPolygon));
        assertEquals(2, specs.size());
        assertInstanceOf(FiatPaymentMethodSpec.class, specs.get(0));
        assertInstanceOf(StableCoinPaymentMethodSpec.class, specs.get(1));
    }

    @Test
    @DisplayName("create quote side payment method specs rejects crypto payment method")
    void create_quote_side_specs_rejects_crypto_payment_method() {
        CryptoPaymentMethod xmr = new CryptoPaymentMethod("XMR");
        assertThrows(IllegalArgumentException.class,
                () -> PaymentMethodSpecUtil.createQuoteSidePaymentMethodSpecs(List.of(xmr)));
    }

    @Test
    @DisplayName("stablecoin payment rail name roundtrip via cookie string")
    void stablecoin_payment_rail_name_roundtrip() {
        StableCoinPaymentMethod usdcPolygon = StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDC_POLYGON);
        String railName = usdcPolygon.getPaymentRailName();
        assertEquals("USDC_POLYGON", railName);

        StableCoinPaymentRail restored = StableCoinPaymentRail.valueOf(railName);
        StableCoinPaymentMethod restoredMethod = StableCoinPaymentMethod.fromPaymentRail(restored);
        assertEquals(usdcPolygon.getPaymentRailName(), restoredMethod.getPaymentRailName());
    }

    @Test
    @DisplayName("fiat payment rail name roundtrip via cookie string")
    void fiat_payment_rail_name_roundtrip() {
        FiatPaymentMethod sepa = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA);
        String railName = sepa.getPaymentRailName();
        assertEquals("SEPA", railName);

        FiatPaymentRail restored = FiatPaymentRail.valueOf(railName);
        FiatPaymentMethod restoredMethod = FiatPaymentMethod.fromPaymentRail(restored);
        assertEquals(sepa.getPaymentRailName(), restoredMethod.getPaymentRailName());
    }

    @Test
    @DisplayName("create quote side payment method specs empty list returns empty")
    void create_quote_side_specs_empty_list() {
        List<PaymentMethodSpec<?>> specs = PaymentMethodSpecUtil.createQuoteSidePaymentMethodSpecs(List.of());
        assertTrue(specs.isEmpty());
    }

    @Test
    @DisplayName("create quote side payment method specs preserves order")
    void create_quote_side_specs_preserves_order() {
        StableCoinPaymentMethod usdcPolygon = StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDC_POLYGON);
        FiatPaymentMethod sepa = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA);
        FiatPaymentMethod zelle = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ZELLE);

        List<PaymentMethodSpec<?>> specs = PaymentMethodSpecUtil.createQuoteSidePaymentMethodSpecs(
                List.of(usdcPolygon, sepa, zelle));
        assertEquals(3, specs.size());
        assertInstanceOf(StableCoinPaymentMethodSpec.class, specs.get(0));
        assertInstanceOf(FiatPaymentMethodSpec.class, specs.get(1));
        assertInstanceOf(FiatPaymentMethodSpec.class, specs.get(2));
    }
}
