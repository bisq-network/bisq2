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
import bisq.account.protocol_type.TradeProtocolType;
import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.locale.LocaleRepository;
import bisq.common.market.Market;
import bisq.contract.Party;
import bisq.contract.Role;
import bisq.contract.mu_sig.MuSigContract;
import bisq.offer.Direction;
import bisq.offer.mu_sig.MuSigOffer;
import org.junit.jupiter.api.AfterEach;
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
    void formatBtcSideAmountMatchesBaseForBtcFiatMarket() {
        MuSigContract contract = createContract(createBtcFiatMarket(), 100_000_000L, 12_345_000L);

        assertEquals("1.00000000", MuSigTradeFormatter.formatBtcSideAmount(contract));
    }

    @Test
    void formatBtcSideAmountMatchesQuoteForCryptoBtcMarket() {
        MuSigContract contract = createContract(createCryptoBtcMarket(), 12_345_000_000L, 100_000_000L);

        assertEquals("1.00000000", MuSigTradeFormatter.formatBtcSideAmount(contract));
    }

    @Test
    void formatNonBtcSideAmountMatchesQuoteForBtcFiatMarket() {
        MuSigContract contract = createContract(createBtcFiatMarket(), 100_000_000L, 12_345_000L);

        assertEquals("1234.50", MuSigTradeFormatter.formatNonBtcSideAmount(contract));
    }

    @Test
    void formatNonBtcSideAmountMatchesBaseForCryptoBtcMarket() {
        MuSigContract contract = createContract(createCryptoBtcMarket(), 12_345_000_000L, 100_000_000L);

        assertEquals("0.012345000000", MuSigTradeFormatter.formatNonBtcSideAmount(contract));
    }

    @Test
    void formatBaseAndQuoteAmountsForBtcFiatMarket() {
        MuSigContract contract = createContract(createBtcFiatMarket(), 100_000_000L, 12_345_000L);

        assertEquals("1.00000000", MuSigTradeFormatter.formatBaseSideAmount(contract));
        assertEquals("1234.50", MuSigTradeFormatter.formatQuoteSideAmount(contract));
        assertEquals("1.00000000 BTC", MuSigTradeFormatter.formatBaseSideAmountWithCode(contract));
        assertEquals("1234.50 USD", MuSigTradeFormatter.formatQuoteSideAmountWithCode(contract));
    }

    @Test
    void formatBaseAndQuoteAmountsForCryptoBtcMarket() {
        MuSigContract contract = createContract(createCryptoBtcMarket(), 12_345_000_000L, 100_000_000L);

        assertEquals("0.012345000000", MuSigTradeFormatter.formatBaseSideAmount(contract));
        assertEquals("1.00000000", MuSigTradeFormatter.formatQuoteSideAmount(contract));
        assertEquals("0.012345000000 XMR", MuSigTradeFormatter.formatBaseSideAmountWithCode(contract));
        assertEquals("1.00000000 BTC", MuSigTradeFormatter.formatQuoteSideAmountWithCode(contract));
    }

    @Test
    void formatPriceWithCodeForBtcFiatMarket() {
        MuSigContract contract = createContract(createBtcFiatMarket(), 100_000_000L, 12_345_000L);

        assertEquals("1234.50 BTC/USD", MuSigTradeFormatter.formatPriceWithCode(contract));
    }

    @Test
    void formatPriceWithCodeForCryptoBtcMarket() {
        MuSigContract contract = createContract(createCryptoBtcMarket(), 12_345_000_000L, 100_000_000L);

        assertEquals("81.0045 XMR/BTC", MuSigTradeFormatter.formatPriceWithCode(contract));
    }

    private MuSigContract createContract(Market market, long baseSideAmount, long quoteSideAmount) {
        MuSigOffer offer = new MuSigOffer("test-id",
                null,
                Direction.BUY,
                market,
                null,
                null,
                List.of(),
                List.of(),
                "1.0.0");
        PaymentMethodSpec<?> baseSpec = market.isBaseCurrencyBitcoin()
                ? PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec().get(0)
                : PaymentMethodSpecUtil.createPaymentMethodSpec(new CryptoPaymentMethod("XMR"), "XMR");
        PaymentMethodSpec<?> quoteSpec = market.isBaseCurrencyBitcoin()
                ? PaymentMethodSpecUtil.createPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ACH_TRANSFER), "USD")
                : PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec().get(0);
        return new MuSigContract(System.currentTimeMillis(),
                offer,
                TradeProtocolType.MU_SIG,
                new Party(Role.TAKER, null),
                baseSideAmount,
                quoteSideAmount,
                baseSpec,
                quoteSpec,
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
