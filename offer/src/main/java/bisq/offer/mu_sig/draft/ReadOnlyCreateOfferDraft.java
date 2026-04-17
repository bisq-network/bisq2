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

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.common.market.Market;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.observable.map.ReadOnlyObservableMap;
import bisq.offer.Direction;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.price.spec.PriceSpec;
import com.google.common.collect.ImmutableMap;

public abstract class ReadOnlyCreateOfferDraft extends ReadOnlyOfferDraft {

    public abstract ReadOnlyObservable<Market> marketObservable();


    public abstract ReadOnlyObservable<Direction> directionObservable();


    public abstract ReadOnlyObservableMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethodObservable();

    public abstract ImmutableMap<PaymentMethod<?>, Account<?, ?>> getSelectedAccountByPaymentMethod();


    public abstract ReadOnlyObservable<Boolean> useBaseCurrencyForAmountInputObservable();

    public abstract boolean getUseBaseCurrencyForAmountInput();


    public abstract ReadOnlyObservable<PriceQuote> priceQuoteObservable();

    public abstract PriceQuote getPriceQuote();


    public abstract ReadOnlyObservable<PriceSpec> priceSpecObservable();




    public abstract ReadOnlyObservable<TradeAmount> defaultTradeAmountObservable();

    public abstract TradeAmount getDefaultTradeAmount();


    public abstract ReadOnlyObservable<TradeAmount> fixTradeAmountObservable();

    public abstract TradeAmount getFixTradeAmount();


    public abstract ReadOnlyObservable<TradeAmount> minTradeAmountObservable();

    public abstract TradeAmount getMinTradeAmount();


    public abstract ReadOnlyObservable<TradeAmount> maxTradeAmountObservable();

    public abstract TradeAmount getMaxTradeAmount();


    public abstract ReadOnlyObservable<AmountSpec> amountSpecObservable();


    public abstract ReadOnlyObservable<Boolean> useRangeAmountObservable();

    public abstract boolean getUseRangeAmount();
}
