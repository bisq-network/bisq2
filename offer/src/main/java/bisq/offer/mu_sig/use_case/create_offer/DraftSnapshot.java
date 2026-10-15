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

package bisq.offer.mu_sig.use_case.create_offer;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.market.Market;
import bisq.common.monetary.PriceQuote;
import bisq.offer.Direction;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.price.spec.PriceSpec;
import com.google.common.collect.ImmutableMap;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An immutable, mutually consistent capture of the create-offer draft, taken in a single
 * synchronized read. The review step builds its display model and the offer from this record so
 * a concurrent market-price update cannot produce mixed values.
 */
public record DraftSnapshot(Market market,
                            Direction displayDirection,
                            AmountSpec amountSpec,
                            PriceSpec priceSpec,
                            ImmutableMap<PaymentMethod<?>, Account<?, ?>> accountByPaymentMethod,
                            Optional<PriceQuote> resolvedPriceQuote,
                            Optional<PriceQuote> marketPriceQuote,
                            double pricePercentage) {
    public DraftSnapshot {
        checkNotNull(market, "market must not be null");
        checkNotNull(displayDirection, "displayDirection must not be null");
        checkNotNull(amountSpec, "amountSpec must not be null");
        checkNotNull(priceSpec, "priceSpec must not be null");
        checkNotNull(accountByPaymentMethod, "accountByPaymentMethod must not be null");
        checkNotNull(resolvedPriceQuote, "resolvedPriceQuote must not be null");
        checkNotNull(marketPriceQuote, "marketPriceQuote must not be null");
    }
}
