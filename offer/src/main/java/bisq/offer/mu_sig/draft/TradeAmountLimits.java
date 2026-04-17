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

package bisq.offer.mu_sig.draft;

import bisq.account.payment_method.PaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.monetary.Fiat;
import bisq.presentation.formatters.AmountFormatter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeAmountLimits {
    public static final Fiat MIN_TRADE_AMOUNT_IN_USD = Fiat.fromFaceValue(10, "USD");
    public static final Fiat MAX_USD_TRADE_AMOUNT = Fiat.fromFaceValue(10000, "USD");

    public static Fiat getMaxTradeLimitInUsd(PaymentRail paymentRail) {
        return getMaxTradeLimitInUsd(paymentRail, MAX_USD_TRADE_AMOUNT);
    }

    public static String getFormattedMaxTradeLimitInUsd(PaymentRail paymentRail) {
        Fiat maxTradeLimit = getMaxTradeLimitInUsd(paymentRail);
        return AmountFormatter.formatQuoteAmount(maxTradeLimit);
    }

    public static Fiat getMaxTradeLimitInUsd(PaymentRail paymentRail, Fiat maxTradeLimitByProtocol) {
        if (paymentRail instanceof FiatPaymentRail fiatPaymentRail) {
            switch (fiatPaymentRail.getChargebackRisk()) {
                case VERY_LOW -> {
                    return maxTradeLimitByProtocol;
                }
                case LOW -> {
                    return maxTradeLimitByProtocol.multiply(0.8);
                }
                case MEDIUM -> {
                    return maxTradeLimitByProtocol.multiply(0.65);
                }
                case MODERATE -> {
                    return maxTradeLimitByProtocol.multiply(0.5);
                }
                default -> {
                    return maxTradeLimitByProtocol.multiply(0d);
                }
            }
        } else {
            return maxTradeLimitByProtocol;
        }
    }
}
