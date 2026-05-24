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

package bisq.trade.mu_sig;

import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.locale.LocaleRepository;
import bisq.common.market.Market;
import bisq.contract.mu_sig.MuSigContract;
import bisq.offer.Direction;
import bisq.offer.mu_sig.MuSigOffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MuSigTradeFormatterTest {
    private Locale previousLocale;

    @BeforeEach
    void setUp() {
        previousLocale = LocaleRepository.getDefaultLocale();
        LocaleRepository.setDefaultLocale(Locale.US);
    }

    @AfterEach
    void tearDown() {
        LocaleRepository.setDefaultLocale(previousLocale);
    }

    @Test
    @DisplayName("format btc side amount matches base for btc fiat market")
    void format_btc_side_amount_matches_base_for_btc_fiat_market() {
        MuSigContract contract = createContract(createBtcFiatMarket(), 100_000_000L, 12_345_000L);

        assertEquals("1.00000000", MuSigTradeFormatter.formatBtcSideAmount(contract));
    }

    @Test
    @DisplayName("format btc side amount matches quote for crypto btc market")
    void format_btc_side_amount_matches_quote_for_crypto_btc_market() {
        MuSigContract contract = createContract(createCryptoBtcMarket(), 12_345_000_000L, 100_000_000L);

        assertEquals("1.00000000", MuSigTradeFormatter.formatBtcSideAmount(contract));
    }

    @Test
    @DisplayName("format non btc side amount matches quote for btc fiat market")
    void format_non_btc_side_amount_matches_quote_for_btc_fiat_market() {
        MuSigContract contract = createContract(createBtcFiatMarket(), 100_000_000L, 12_345_000L);

        assertEquals("1234.50", MuSigTradeFormatter.formatNonBtcSideAmount(contract));
    }

    @Test
    @DisplayName("format non btc side amount matches base for crypto btc market")
    void format_non_btc_side_amount_matches_base_for_crypto_btc_market() {
        MuSigContract contract = createContract(createCryptoBtcMarket(), 12_345_000_000L, 100_000_000L);

        assertEquals("0.012345000000", MuSigTradeFormatter.formatNonBtcSideAmount(contract));
    }

    @Test
    @DisplayName("format base and quote amounts for btc fiat market")
    void format_base_and_quote_amounts_for_btc_fiat_market() {
        MuSigContract contract = createContract(createBtcFiatMarket(), 100_000_000L, 12_345_000L);

        assertEquals("1.00000000", MuSigTradeFormatter.formatBaseSideAmount(contract));
        assertEquals("1234.50", MuSigTradeFormatter.formatQuoteSideAmount(contract));
        assertEquals("1.00000000 BTC", MuSigTradeFormatter.formatBaseSideAmountWithCode(contract));
        assertEquals("1234.50 USD", MuSigTradeFormatter.formatQuoteSideAmountWithCode(contract));
    }

    @Test
    @DisplayName("format base and quote amounts for crypto btc market")
    void format_base_and_quote_amounts_for_crypto_btc_market() {
        MuSigContract contract = createContract(createCryptoBtcMarket(), 12_345_000_000L, 100_000_000L);

        assertEquals("0.012345000000", MuSigTradeFormatter.formatBaseSideAmount(contract));
        assertEquals("1.00000000", MuSigTradeFormatter.formatQuoteSideAmount(contract));
        assertEquals("0.012345000000 XMR", MuSigTradeFormatter.formatBaseSideAmountWithCode(contract));
        assertEquals("1.00000000 BTC", MuSigTradeFormatter.formatQuoteSideAmountWithCode(contract));
    }

    @Test
    @DisplayName("format price with code for btc fiat market")
    void format_price_with_code_for_btc_fiat_market() {
        MuSigContract contract = createContract(createBtcFiatMarket(), 100_000_000L, 12_345_000L);

        assertEquals("1234.50 BTC/USD", MuSigTradeFormatter.formatPriceWithCode(contract));
    }

    @Test
    @DisplayName("format price with code for crypto btc market")
    void format_price_with_code_for_crypto_btc_market() {
        MuSigContract contract = createContract(createCryptoBtcMarket(), 12_345_000_000L, 100_000_000L);

        assertEquals("81.0045 XMR/BTC", MuSigTradeFormatter.formatPriceWithCode(contract));
    }

    private MuSigContract createContract(Market market, long baseSideAmount, long quoteSideAmount) {
        PaymentMethodSpec<?> nonBtcPaymentMethodSpec = market.isBaseCurrencyBitcoin()
                ? PaymentMethodSpecUtil.createPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER), "USD")
                : PaymentMethodSpecUtil.createPaymentMethodSpec(new CryptoPaymentMethod("XMR"), "XMR");
        MuSigOffer offer = new MuSigOffer("test-id",
                null,
                Direction.BUY,
                market,
                null,
                null,
                List.of(nonBtcPaymentMethodSpec.getPaymentMethod()),
                List.of(),
                "1.0.0");
        return new MuSigContract(System.currentTimeMillis(),
                offer,
                null,
                baseSideAmount,
                quoteSideAmount,
                nonBtcPaymentMethodSpec,
                new byte[20],
                Optional.empty(),
                Optional.empty(),
                null,
                0);
    }

    private Market createBtcFiatMarket() {
        return new Market("BTC", "USD", "Bitcoin", "US Dollar");
    }

    private Market createCryptoBtcMarket() {
        return new Market("XMR", "BTC", "Monero", "Bitcoin");
    }
}
