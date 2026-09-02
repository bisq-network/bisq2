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

package bisq.offer.mu_sig.use_case.take_offer.amount;

import bisq.common.monetary.MonetaryRange;
import bisq.common.monetary.TradeAmount;
import bisq.common.monetary.TradeAmountRange;
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.offer.amount.spec.AmountSpec;

import java.util.Optional;

public class TakeOfferAmountModel implements TakeOfferAmountReadOnlyModel {
    private AmountSpec amountSpec;
    protected final Observable<Boolean> useBaseCurrencyForAmountInput = new Observable<>(false);
    protected final Observable<TradeAmount> fixTradeAmount = new Observable<>();
    protected final Observable<Optional<TradeAmount>> userSpecificTradeAmountLimit = new Observable<>(Optional.empty());
    protected final Observable<Optional<Double>> userSpecificTradeAmountLimitAsSliderValue = new Observable<>(Optional.empty());
    protected final Observable<TradeAmountRange> tradeAmountLimits = new Observable<>();
    protected final Observable<MonetaryRange> inputAmountLimits = new Observable<>();
    protected final Observable<Double> fixAmountSliderValue = new Observable<>(0d);

    public TakeOfferAmountModel() {
    }

    /* --------------------------------------------------------------------- */
    // amountSpec
    /* --------------------------------------------------------------------- */

    void setAmountSpec(AmountSpec amountSpec) {
        this.amountSpec = amountSpec;
    }

    @Override
    public AmountSpec getAmountSpec() {
        return amountSpec;
    }

    /* --------------------------------------------------------------------- */
    // useBaseCurrencyForAmountInput
    /* --------------------------------------------------------------------- */

    void setUseBaseCurrencyForAmountInput(boolean value) {
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
    // TradeAmountLimits
    /* --------------------------------------------------------------------- */

    void setTradeAmountLimits(TradeAmountRange tradeAmountLimits) {
        this.tradeAmountLimits.set(tradeAmountLimits);
    }

    @Override
    public TradeAmountRange getTradeAmountLimits() {
        return tradeAmountLimits.get();
    }

    @Override
    public ReadOnlyObservable<TradeAmountRange> tradeAmountLimitsObservable() {
        return tradeAmountLimits;
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
        userSpecificTradeAmountLimitAsSliderValue.set(sliderValue);
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
        fixAmountSliderValue.set(sliderValue);
    }

    @Override
    public ReadOnlyObservable<Double> fixAmountSliderValueObservable() {
        return fixAmountSliderValue;
    }

    @Override
    public Double getFixAmountSliderValue() {
        return fixAmountSliderValue.get();
    }
}
