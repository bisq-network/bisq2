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

package bisq.api.dto.mappings.account;

import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.crypto.CryptoPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.monetary.Fiat;
import bisq.presentation.formatters.AmountFormatter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PaymentMethodDtoMappingHelperTest {

    @Test
    void tradeLimitInfoReflectsChargebackRiskBasedLimitInUsd() {
        // VERY_LOW risk: the full protocol limit
        assertEquals(formattedUsd(10_000),
                PaymentMethodDtoMappingHelper.getTradeLimitInfo(FiatPaymentRail.ADVANCED_CASH));
        // LOW risk: 80%
        assertEquals(formattedUsd(8_000),
                PaymentMethodDtoMappingHelper.getTradeLimitInfo(FiatPaymentRail.ALI_PAY));
        // MEDIUM risk: 65%
        assertEquals(formattedUsd(6_500),
                PaymentMethodDtoMappingHelper.getTradeLimitInfo(FiatPaymentRail.MONEY_GRAM));
        // MODERATE risk: 50%
        assertEquals(formattedUsd(5_000),
                PaymentMethodDtoMappingHelper.getTradeLimitInfo(FiatPaymentRail.WISE));
    }

    @Test
    void tradeLimitInfoForNonFiatRailIsTheProtocolLimit() {
        assertEquals(formattedUsd(10_000),
                PaymentMethodDtoMappingHelper.getTradeLimitInfo(BitcoinPaymentRail.MAIN_CHAIN));
        assertEquals(formattedUsd(10_000),
                PaymentMethodDtoMappingHelper.getTradeLimitInfo(CryptoPaymentRail.NATIVE_CHAIN));
    }

    private static String formattedUsd(long faceValue) {
        return AmountFormatter.formatQuoteAmount(Fiat.fromFaceValue(faceValue, "USD"));
    }
}
