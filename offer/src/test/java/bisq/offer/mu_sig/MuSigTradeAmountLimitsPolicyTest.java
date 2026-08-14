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

package bisq.offer.mu_sig;

import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.monetary.Fiat;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MuSigTradeAmountLimitsPolicyTest {
    @Test
    void railDependentLimitsFollowTheChargebackRiskTiers() {
        assertThat(MuSigTradeAmountLimitsPolicy.getMaxTradeLimitInUsd(FiatPaymentRail.ADVANCED_CASH))
                .isEqualTo(Fiat.fromFaceValue(10000, "USD"));
        assertThat(MuSigTradeAmountLimitsPolicy.getMaxTradeLimitInUsd(FiatPaymentRail.ALI_PAY))
                .isEqualTo(Fiat.fromFaceValue(8000, "USD"));
        assertThat(MuSigTradeAmountLimitsPolicy.getMaxTradeLimitInUsd(FiatPaymentRail.MONEY_GRAM))
                .isEqualTo(Fiat.fromFaceValue(6500, "USD"));
        assertThat(MuSigTradeAmountLimitsPolicy.getMaxTradeLimitInUsd(FiatPaymentRail.WISE))
                .isEqualTo(Fiat.fromFaceValue(5000, "USD"));
    }

    @Test
    void nonFiatRailsGetTheProtocolLimit() {
        assertThat(MuSigTradeAmountLimitsPolicy.getMaxTradeLimitInUsd(BitcoinPaymentRail.MAIN_CHAIN))
                .isEqualTo(MuSigTradeAmountLimitsPolicy.MAX_USD_TRADE_AMOUNT);
    }
}
