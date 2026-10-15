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
import bisq.common.observable.ReadOnlyObservable;
import bisq.offer.amount.spec.AmountSpec;

import java.util.Optional;

public interface TakeOfferAmountReadOnlyModel {
    ReadOnlyObservable<Boolean> useBaseCurrencyForAmountInputObservable();

    boolean getUseBaseCurrencyForAmountInput();


    ReadOnlyObservable<TradeAmount> fixTradeAmountObservable();

    TradeAmount getFixTradeAmount();


    AmountSpec getAmountSpec();


    TradeAmountRange getTradeAmountLimits();

    ReadOnlyObservable<TradeAmountRange> tradeAmountLimitsObservable();

    ReadOnlyObservable<Integer> constraintsRecomputeRevisionObservable();


    ReadOnlyObservable<Optional<TradeAmount>> userSpecificTradeAmountLimitObservable();

    Optional<TradeAmount> getUserSpecificTradeAmountLimit();


    ReadOnlyObservable<Optional<Double>> userSpecificTradeAmountLimitAsSliderValueObservable();

    Optional<Double> getUserSpecificTradeAmountLimitAsSliderValue();


    ReadOnlyObservable<MonetaryRange> inputAmountLimitsObservable();

    MonetaryRange getInputAmountLimits();


    ReadOnlyObservable<Double> fixAmountSliderValueObservable();

    Double getFixAmountSliderValue();


    ReadOnlyObservable<Boolean> amountValidObservable();

    boolean isAmountValid();
}
