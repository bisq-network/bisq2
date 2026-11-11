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

package bisq.account.payment_method.fiat;

import bisq.common.asset.FiatCurrencyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A fiat payment method supports the currencies of its payment rail (see docs/specifications/offer/payment-methods.md).
 */
class FiatPaymentMethodSupportsCurrencyTest {
    @ParameterizedTest
    @EnumSource(FiatPaymentRail.class)
    void paymentMethodSupportsEachCurrencyOfItsPaymentRail(FiatPaymentRail paymentRail) {
        FiatPaymentMethod paymentMethod = FiatPaymentMethod.fromPaymentRail(paymentRail);

        paymentRail.getSupportedCurrencyCodes().forEach(code ->
                assertTrue(paymentMethod.supportsCurrency(code), paymentRail + " must support " + code));
    }

    @ParameterizedTest
    @EnumSource(FiatPaymentRail.class)
    void paymentMethodSupportsOnlyTheCurrenciesOfItsPaymentRail(FiatPaymentRail paymentRail) {
        FiatPaymentMethod paymentMethod = FiatPaymentMethod.fromPaymentRail(paymentRail);

        FiatCurrencyRepository.getAllFiatCurrencyCodes().forEach(code ->
                assertEquals(paymentRail.supportsCurrency(code), paymentMethod.supportsCurrency(code),
                        paymentRail + " and " + code));
    }

    @Test
    void singleCurrencyPaymentMethodDoesNotSupportOtherCurrencies() {
        assertTrue(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA).supportsCurrency("EUR"));
        assertFalse(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA).supportsCurrency("USD"));
        assertTrue(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ZELLE).supportsCurrency("USD"));
        assertFalse(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ZELLE).supportsCurrency("EUR"));
        assertFalse(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.PIX).supportsCurrency("USD"));
    }

    @Test
    void multiCurrencyPaymentMethodSupportsOnlyItsCurrencies() {
        FiatPaymentMethod revolut = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.REVOLUT);
        assertTrue(revolut.supportsCurrency("USD"));
        assertTrue(revolut.supportsCurrency("EUR"));
        assertFalse(revolut.supportsCurrency("BRL"));

        FiatPaymentMethod wise = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.WISE);
        assertTrue(wise.supportsCurrency("BRL"));
        assertFalse(wise.supportsCurrency("RUB"));
    }

    // The list of all fiat currencies comes from the currency data of the JDK. A node with other currency data must
    // not reject a national bank transfer in a currency which only the other node knows.
    @Test
    void paymentMethodsOfAllCurrenciesSupportAlsoACurrencyWhichTheNodeDoesNotKnow() {
        String unknownCode = "QQQ";
        assertFalse(FiatCurrencyRepository.getAllFiatCurrencyCodes().contains(unknownCode));

        List.of(FiatPaymentRail.NATIONAL_BANK, FiatPaymentRail.SAME_BANK, FiatPaymentRail.SWIFT,
                        FiatPaymentRail.F2F, FiatPaymentRail.CASH_BY_MAIL, FiatPaymentRail.CASH_DEPOSIT,
                        FiatPaymentRail.CUSTOM)
                .forEach(paymentRail -> {
                    assertTrue(paymentRail.supportsAllCurrencies(), paymentRail + " supports all currencies");
                    assertTrue(FiatPaymentMethod.fromPaymentRail(paymentRail).supportsCurrency(unknownCode),
                            paymentRail + " must support " + unknownCode);
                });
        assertFalse(FiatPaymentRail.MONEY_GRAM.supportsAllCurrencies());
        assertFalse(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.MONEY_GRAM).supportsCurrency(unknownCode));
    }

    @Test
    void customPaymentMethodSupportsEveryCurrency() {
        FiatPaymentMethod custom = FiatPaymentMethod.fromCustomName("My bank");

        assertTrue(custom.isCustomPaymentMethod());
        assertTrue(custom.supportsCurrency("USD"));
        assertTrue(custom.supportsCurrency("EUR"));
        assertTrue(custom.supportsCurrency("QQQ"));
    }
}
