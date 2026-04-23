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
import bisq.common.monetary.Monetary;
import bisq.common.monetary.MonetaryRange;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;
import bisq.common.monetary.TradeAmountRange;
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.observable.map.ReadOnlyObservableMap;
import bisq.offer.Direction;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CreateOfferDraft extends ReadOnlyCreateOfferDraft {
    protected final Observable<Market> market = new Observable<>();

    protected final Observable<Direction> direction = new Observable<>();

    private final Observable<PriceQuote> priceQuote = new Observable<>();

    private final Observable<Boolean> useBaseCurrencyForAmountInput = new Observable<>(false);
    private final Observable<Boolean> useRangeAmount = new Observable<>(false);
    private final Observable<TradeAmount> fixTradeAmount = new Observable<>();
    private final Observable<TradeAmount> minTradeAmount = new Observable<>();
    private final Observable<TradeAmount> maxTradeAmount = new Observable<>();
    private final Observable<Optional<TradeAmount>> userSpecificTradeAmountLimit = new Observable<>(Optional.empty());
    private final Observable<Optional<Double>> userSpecificTradeAmountLimitAsSliderValue = new Observable<>(Optional.empty());
    private final Observable<Optional<Monetary>> userSpecificInputAmountLimit = new Observable<>(Optional.empty());
    private final Observable<TradeAmountRange> tradeAmountLimits = new Observable<>();
    private final Observable<MonetaryRange> inputAmountLimits = new Observable<>();
    private final Observable<Double> fixAmountSliderValue = new Observable<>(0d);
    private final Observable<Double> minAmountSliderValue = new Observable<>(0d);
    private final Observable<Double> maxAmountSliderValue = new Observable<>(0d);

    private final ObservableHashMap<PaymentMethod<?>, List<Account<?, ?>>> accountsByPaymentMethod = new ObservableHashMap<>();
    private final ObservableHashMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod = new ObservableHashMap<>();

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
    // TradeAmountLimits
    /* --------------------------------------------------------------------- */

    void setTradeAmountLimits(TradeAmountRange tradeAmountLimits) {
        this.tradeAmountLimits.set(tradeAmountLimits);
    }

    @Override
    public ReadOnlyObservable<TradeAmountRange> tradeAmountLimitsObservable() {
        return tradeAmountLimits;
    }

    @Override
    public TradeAmountRange getTradeAmountLimits() {
        return tradeAmountLimits.get();
    }


    /* --------------------------------------------------------------------- */
    // userSpecificTradeAmountLimit
    /* --------------------------------------------------------------------- */

    void setUserSpecificTradeAmountLimit(Optional<TradeAmount> userSpecificTradeAmountLimit) {
        this.userSpecificTradeAmountLimit.set(userSpecificTradeAmountLimit);
    }

    @Override
    public ReadOnlyObservable<Optional<TradeAmount>> userSpecificTradeAmountLimitObservable() {
        return userSpecificTradeAmountLimit;
    }

    @Override
    public Optional<TradeAmount> getUserSpecificTradeAmountLimit() {
        return userSpecificTradeAmountLimit.get();
    }


    /* --------------------------------------------------------------------- */
    // userSpecificTradeAmountLimitAsSliderValue
    /* --------------------------------------------------------------------- */

    void setUserSpecificTradeAmountLimitAsSliderValue(Optional<Double> sliderValue) {
        this.userSpecificTradeAmountLimitAsSliderValue.set(sliderValue);
    }

    @Override
    public ReadOnlyObservable<Optional<Double>> userSpecificTradeAmountLimitAsSliderValueObservable() {
        return userSpecificTradeAmountLimitAsSliderValue;
    }

    @Override
    public Optional<Double> getUserSpecificTradeAmountLimitAsSliderValue() {
        return userSpecificTradeAmountLimitAsSliderValue.get();
    }


    /* --------------------------------------------------------------------- */
    // InputAmountLimits
    /* --------------------------------------------------------------------- */

    void setInputAmountLimits(MonetaryRange inputAmountLimits) {
        this.inputAmountLimits.set(inputAmountLimits);
    }

    @Override
    public ReadOnlyObservable<MonetaryRange> inputAmountLimitsObservable() {
        return inputAmountLimits;
    }

    @Override
    public MonetaryRange getInputAmountLimits() {
        return inputAmountLimits.get();
    }

    /* --------------------------------------------------------------------- */
    // fixAmountSliderValue
    /* --------------------------------------------------------------------- */

    void setFixAmountSliderValue(double sliderValue) {
        this.fixAmountSliderValue.set(sliderValue);
    }

    @Override
    public ReadOnlyObservable<Double> fixAmountSliderValueObservable() {
        return fixAmountSliderValue;
    }

    @Override
    public Double getFixAmountSliderValue() {
        return fixAmountSliderValue.get();
    }

    /* --------------------------------------------------------------------- */
    // minAmountSliderValue
    /* --------------------------------------------------------------------- */

    void setMinAmountSliderValue(double sliderValue) {
        this.minAmountSliderValue.set(sliderValue);
    }

    @Override
    public ReadOnlyObservable<Double> minAmountSliderValueObservable() {
        return minAmountSliderValue;
    }

    @Override
    public Double getMinAmountSliderValue() {
        return minAmountSliderValue.get();
    }


    /* --------------------------------------------------------------------- */
    // maxAmountSliderValue
    /* --------------------------------------------------------------------- */

    void setMaxAmountSliderValue(double sliderValue) {
        this.maxAmountSliderValue.set(sliderValue);
    }

    @Override
    public ReadOnlyObservable<Double> maxAmountSliderValueObservable() {
        return maxAmountSliderValue;
    }

    @Override
    public Double getMaxAmountSliderValue() {
        return maxAmountSliderValue.get();
    }



    /* --------------------------------------------------------------------- */
    // accountsByPaymentMethod
    /* --------------------------------------------------------------------- */

    void clearAccountsByPaymentMethod() {
        accountsByPaymentMethod.clear();
    }

    void putAccountsByPaymentMethod(PaymentMethod<?> paymentMethod, List<Account<?, ?>> account) {
        accountsByPaymentMethod.put(paymentMethod, account);
    }

    void removeAccountsByPaymentMethod(PaymentMethod<?> paymentMethod) {
        accountsByPaymentMethod.remove(paymentMethod);
    }

    void putAllAccountsByPaymentMethod(Map<PaymentMethod<?>, List<Account<?, ?>>> accountsByPaymentMethod) {
        this.accountsByPaymentMethod.putAll(accountsByPaymentMethod);
    }

    @Override
    public ReadOnlyObservableMap<PaymentMethod<?>, List<Account<?, ?>>> accountsByPaymentMethodObservable() {
        return accountsByPaymentMethod;
    }

    @Override
    public ImmutableMap<PaymentMethod<?>, List<Account<?, ?>>> getAccountsByPaymentMethod() {
        return ImmutableMap.copyOf(accountsByPaymentMethod);
    }


    /* --------------------------------------------------------------------- */
    // selectedAccountByPaymentMethod
    /* --------------------------------------------------------------------- */

    void clearSelectedAccountByPaymentMethod() {
        selectedAccountByPaymentMethod.clear();
    }

    void putSelectedAccountByPaymentMethod(PaymentMethod<?> paymentMethod, Account<?, ?> account) {
        selectedAccountByPaymentMethod.put(paymentMethod, account);
    }

    void removeSelectedAccountByPaymentMethod(PaymentMethod<?> paymentMethod) {
        selectedAccountByPaymentMethod.remove(paymentMethod);
    }

    void putAllSelectedAccountByPaymentMethod(Map<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethod) {
        this.selectedAccountByPaymentMethod.putAll(selectedAccountByPaymentMethod);
    }

    @Override
    public ReadOnlyObservableMap<PaymentMethod<?>, Account<?, ?>> selectedAccountByPaymentMethodObservable() {
        return selectedAccountByPaymentMethod;
    }

    @Override
    public ImmutableMap<PaymentMethod<?>, Account<?, ?>> getSelectedAccountByPaymentMethod() {
        return ImmutableMap.copyOf(selectedAccountByPaymentMethod);
    }
}
