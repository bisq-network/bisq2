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

package bisq.offer.mu_sig.use_case.create_offer.amount;

import bisq.common.monetary.MonetaryRange;
import bisq.common.monetary.TradeAmount;
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;

import java.util.Optional;

public class CreateOfferAmountModel {
    private final Observable<Boolean> initialized = new Observable<>(false);

    private final Observable<Boolean> useBaseCurrencyForAmountInput = new Observable<>(false);
    private final Observable<Boolean> useRangeAmount = new Observable<>(false);

    private final Observable<TradeAmount> fixTradeAmount = new Observable<>();
    private final Observable<TradeAmount> minTradeAmount = new Observable<>();
    private final Observable<TradeAmount> maxTradeAmount = new Observable<>();

    private final Observable<Double> fixAmountSliderValue = new Observable<>(0d);
    private final Observable<Double> minAmountSliderValue = new Observable<>(0d);
    private final Observable<Double> maxAmountSliderValue = new Observable<>(0d);

    private final Observable<Optional<Double>> userSpecificTradeAmountLimitAsSliderValue = new Observable<>(Optional.empty());
    private final Observable<MonetaryRange> inputAmountRange = new Observable<>();

    public CreateOfferAmountModel() {
    }


    /* --------------------------------------------------------------------- */
    // initialized
    /* --------------------------------------------------------------------- */

    void setInitialized(boolean value) {
        initialized.set(value);
    }

    public Observable<Boolean> initializedObservable() {
        return initialized;
    }

    public boolean isInitialized() {
        return initialized.get();
    }



    /* --------------------------------------------------------------------- */
    // useBaseCurrencyForAmountInput
    /* --------------------------------------------------------------------- */

    void setUseBaseCurrencyForAmountInput(boolean value) {
        useBaseCurrencyForAmountInput.set(value);
    }

    public ReadOnlyObservable<Boolean> useBaseCurrencyForAmountInputObservable() {
        return useBaseCurrencyForAmountInput;
    }

    public boolean getUseBaseCurrencyForAmountInput() {
        return useBaseCurrencyForAmountInput.get();
    }


    /* --------------------------------------------------------------------- */
    // useRangeAmount
    /* --------------------------------------------------------------------- */

    void setUseRangeAmount(boolean useRangeAmount) {
        this.useRangeAmount.set(useRangeAmount);
    }

    public ReadOnlyObservable<Boolean> useRangeAmountObservable() {
        return useRangeAmount;
    }

    public boolean getUseRangeAmount() {
        return useRangeAmount.get();
    }


    /* --------------------------------------------------------------------- */
    // fixTradeAmount
    /* --------------------------------------------------------------------- */

    void setFixTradeAmount(TradeAmount fixTradeAmount) {
        this.fixTradeAmount.set(fixTradeAmount);
    }

    public ReadOnlyObservable<TradeAmount> fixTradeAmountObservable() {
        return fixTradeAmount;
    }

    public TradeAmount getFixTradeAmount() {
        return fixTradeAmount.get();
    }



    /* --------------------------------------------------------------------- */
    // minTradeAmount
    /* --------------------------------------------------------------------- */

    void setMinTradeAmount(TradeAmount minTradeAmount) {
        this.minTradeAmount.set(minTradeAmount);
    }

    public ReadOnlyObservable<TradeAmount> minTradeAmountObservable() {
        return minTradeAmount;
    }

    public TradeAmount getMinTradeAmount() {
        return minTradeAmount.get();
    }


    /* --------------------------------------------------------------------- */
    // maxTradeAmount
    /* --------------------------------------------------------------------- */

    void setMaxTradeAmount(TradeAmount maxTradeAmount) {
        this.maxTradeAmount.set(maxTradeAmount);
    }

    public ReadOnlyObservable<TradeAmount> maxTradeAmountObservable() {
        return maxTradeAmount;
    }

    public TradeAmount getMaxTradeAmount() {
        return maxTradeAmount.get();
    }



    /* --------------------------------------------------------------------- */
    // fixAmountSliderValue
    /* --------------------------------------------------------------------- */

    void setFixAmountSliderValue(double sliderValue) {
        fixAmountSliderValue.set(sliderValue);
    }

    public ReadOnlyObservable<Double> fixAmountSliderValueObservable() {
        return fixAmountSliderValue;
    }

    public Double getFixAmountSliderValue() {
        return fixAmountSliderValue.get();
    }


    /* --------------------------------------------------------------------- */
    // minAmountSliderValue
    /* --------------------------------------------------------------------- */

    void setMinAmountSliderValue(double sliderValue) {
        minAmountSliderValue.set(sliderValue);
    }

    public ReadOnlyObservable<Double> minAmountSliderValueObservable() {
        return minAmountSliderValue;
    }

    public Double getMinAmountSliderValue() {
        return minAmountSliderValue.get();
    }


    /* --------------------------------------------------------------------- */
    // maxAmountSliderValue
    /* --------------------------------------------------------------------- */

    void setMaxAmountSliderValue(double sliderValue) {
        maxAmountSliderValue.set(sliderValue);
    }

    public ReadOnlyObservable<Double> maxAmountSliderValueObservable() {
        return maxAmountSliderValue;
    }

    public Double getMaxAmountSliderValue() {
        return maxAmountSliderValue.get();
    }



    /* --------------------------------------------------------------------- */
    // userSpecificTradeAmountLimitAsSliderValue
    /* --------------------------------------------------------------------- */

    void setUserSpecificTradeAmountLimitAsSliderValue(Optional<Double> sliderValue) {
        userSpecificTradeAmountLimitAsSliderValue.set(sliderValue);
    }

    public ReadOnlyObservable<Optional<Double>> userSpecificTradeAmountLimitAsSliderValueObservable() {
        return userSpecificTradeAmountLimitAsSliderValue;
    }

    public Optional<Double> getUserSpecificTradeAmountLimitAsSliderValue() {
        return userSpecificTradeAmountLimitAsSliderValue.get();
    }


    /* --------------------------------------------------------------------- */
    // inputAmountRange
    /* --------------------------------------------------------------------- */

    void setInputAmountRange(MonetaryRange inputAmountRange) {
        this.inputAmountRange.set(inputAmountRange);
    }

    public ReadOnlyObservable<MonetaryRange> inputAmountRangeObservable() {
        return inputAmountRange;
    }

    public MonetaryRange getInputAmountRange() {
        return inputAmountRange.get();
    }
}
