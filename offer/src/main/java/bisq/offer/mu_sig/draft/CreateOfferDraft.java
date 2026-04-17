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
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.observable.map.ReadOnlyObservableMap;
import bisq.offer.Direction;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.price.spec.PriceSpec;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class CreateOfferDraft extends ReadOnlyCreateOfferDraft {
    protected final Observable<Market> market = new Observable<>();

    protected final Observable<Direction> direction = new Observable<>();

    private final ObservableHashMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod = new ObservableHashMap<>();

    private final Observable<PriceQuote> priceQuote = new Observable<>();
    private final Observable<PriceSpec> priceSpec = new Observable<>();

    private final Observable<Boolean> useBaseCurrencyForAmountInput = new Observable<>();
    private final Observable<Boolean> useRangeAmount = new Observable<>();
    private final Observable<TradeAmount> defaultTradeAmount = new Observable<>();
    private final Observable<TradeAmount> fixTradeAmount = new Observable<>();
    private final Observable<TradeAmount> minTradeAmount = new Observable<>();
    private final Observable<TradeAmount> maxTradeAmount = new Observable<>();
    private final Observable<AmountSpec> amountSpec = new Observable<>();

    public CreateOfferDraft() {
    }


    /* --------------------------------------------------------------------- */
    // Market
    /* --------------------------------------------------------------------- */

    void setMarket(Market market) {
        this.market.set(market);
    }

    @Override
    public ReadOnlyObservable<Market> marketObservable() {
        return market;
    }

    @Override
    public Market getMarket() {
        return market.get();
    }


    /* --------------------------------------------------------------------- */
    // Direction
    /* --------------------------------------------------------------------- */

    void setDirection(Direction direction) {
        this.direction.set(direction);
    }

    @Override
    public ReadOnlyObservable<Direction> directionObservable() {
        return direction;
    }

    @Override
    public Direction getDirection() {
        return direction.get();
    }


    /* --------------------------------------------------------------------- */
    // selectedAccountByPaymentMethod
    /* --------------------------------------------------------------------- */

    @Override
    public ReadOnlyObservableMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethodObservable() {
        return selectedAccountByPaymentMethod;
    }

    @Override
    public ImmutableMap<PaymentMethod<?>, Account<?, ?>> getSelectedAccountByPaymentMethod() {
        return ImmutableMap.copyOf(selectedAccountByPaymentMethod);
    }

    void clearSelectedAccountByPaymentMethod() {
        selectedAccountByPaymentMethod.clear();
    }

    void putAllSelectedAccountByPaymentMethod(Map<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod) {
        this.selectedAccountByPaymentMethod.putAll(selectedAccountByPaymentMethod);
    }



    /* --------------------------------------------------------------------- */
    // PriceQuote
    /* --------------------------------------------------------------------- */

    void setPriceQuote(PriceQuote priceQuote) {
        this.priceQuote.set(priceQuote);
    }

    @Override
    public ReadOnlyObservable<PriceQuote> priceQuoteObservable() {
        return priceQuote;
    }

    @Override
    public PriceQuote getPriceQuote() {
        return priceQuote.get();
    }

    /* --------------------------------------------------------------------- */
    // PriceSpec
    /* --------------------------------------------------------------------- */

    void setPriceSpec(PriceSpec priceSpec) {
        this.priceSpec.set(priceSpec);
    }

    @Override
    public ReadOnlyObservable<PriceSpec> priceSpecObservable() {
        return priceSpec;
    }

    @Override
    public PriceSpec getPriceSpec() {
        return priceSpec.get();
    }


    /* --------------------------------------------------------------------- */
    // useBaseCurrencyForAmountInput
    /* --------------------------------------------------------------------- */

    public void setUseBaseCurrencyForAmountInput(boolean value) {
        useBaseCurrencyForAmountInput.set(value);
    }

    @Override
    public ReadOnlyObservable<Boolean> useBaseCurrencyForAmountInputObservable() {
        return useBaseCurrencyForAmountInput;
    }

    @Override
    public boolean getUseBaseCurrencyForAmountInput() {
        return useBaseCurrencyForAmountInput.get();
    }


    /* --------------------------------------------------------------------- */
    // useRangeAmount
    /* --------------------------------------------------------------------- */

    public void setUseRangeAmount(boolean useRangeAmount) {
        this.useRangeAmount.set(useRangeAmount);
    }

    @Override
    public ReadOnlyObservable<Boolean> useRangeAmountObservable() {
        return useRangeAmount;
    }

    @Override
    public boolean getUseRangeAmount() {
        return useRangeAmount.get();
    }

    /* --------------------------------------------------------------------- */
    // defaultTradeAmount
    /* --------------------------------------------------------------------- */

    void setDefaultTradeAmount(TradeAmount defaultTradeAmount) {
        this.defaultTradeAmount.set(defaultTradeAmount);
    }

    @Override
    public ReadOnlyObservable<TradeAmount> defaultTradeAmountObservable() {
        return defaultTradeAmount;
    }

    @Override
    public TradeAmount getDefaultTradeAmount() {
        return defaultTradeAmount.get();
    }

    /* --------------------------------------------------------------------- */
    // fixTradeAmount
    /* --------------------------------------------------------------------- */

    void setFixTradeAmount(TradeAmount fixTradeAmount) {
        this.fixTradeAmount.set(fixTradeAmount);
    }

    @Override
    public ReadOnlyObservable<TradeAmount> fixTradeAmountObservable() {
        return fixTradeAmount;
    }

    @Override
    public TradeAmount getFixTradeAmount() {
        return fixTradeAmount.get();
    }


    /* --------------------------------------------------------------------- */
    // minTradeAmount
    /* --------------------------------------------------------------------- */

    void setMinTradeAmount(TradeAmount minTradeAmount) {
        this.minTradeAmount.set(minTradeAmount);
    }

    @Override
    public ReadOnlyObservable<TradeAmount> minTradeAmountObservable() {
        return minTradeAmount;
    }

    @Override
    public TradeAmount getMinTradeAmount() {
        return minTradeAmount.get();
    }


    /* --------------------------------------------------------------------- */
    // maxTradeAmount
    /* --------------------------------------------------------------------- */

    void setMaxTradeAmount(TradeAmount maxTradeAmount) {
        this.maxTradeAmount.set(maxTradeAmount);
    }

    @Override
    public ReadOnlyObservable<TradeAmount> maxTradeAmountObservable() {
        return maxTradeAmount;
    }

    @Override
    public TradeAmount getMaxTradeAmount() {
        return maxTradeAmount.get();
    }

    /* --------------------------------------------------------------------- */
    // AmountSpec
    /* --------------------------------------------------------------------- */

    void setAmountSpec(AmountSpec amountSpec) {
        this.amountSpec.set(amountSpec);
    }

    @Override
    public ReadOnlyObservable<AmountSpec> amountSpecObservable() {
        return amountSpec;
    }

    @Override
    public AmountSpec getAmountSpec() {
        return amountSpec.get();
    }
}
