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
import bisq.offer.amount.spec.AmountSpec;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Delegate;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class TakeOfferAmountService {
    @Getter(AccessLevel.PACKAGE)
    @Delegate
    private final TakeOfferAmountModel model;

    public TakeOfferAmountService() {
        this.model = new TakeOfferAmountModel();
    }

    public void setAmountSpec(AmountSpec amountSpec) {
        model.setAmountSpec(checkNotNull(amountSpec, "amountSpec must not be null"));
    }

    public void setUseBaseCurrencyForAmountInput(boolean value) {
        model.setUseBaseCurrencyForAmountInput(value);
    }

    public void setFixTradeAmount(TradeAmount fixTradeAmount) {
        model.setFixTradeAmount(fixTradeAmount);
    }

    public void setUserSpecificTradeAmountLimit(Optional<TradeAmount> userSpecificTradeAmountLimit) {
        model.setUserSpecificTradeAmountLimit(userSpecificTradeAmountLimit);
    }

    public void setUserSpecificTradeAmountLimitAsSliderValue(Optional<Double> sliderValue) {
        model.setUserSpecificTradeAmountLimitAsSliderValue(sliderValue);
    }

    public void setTradeAmountLimits(TradeAmountRange tradeAmountLimits) {
        model.setTradeAmountLimits(tradeAmountLimits);
    }

    public void setInputAmountLimits(MonetaryRange inputAmountLimits) {
        model.setInputAmountLimits(inputAmountLimits);
    }

    public void setFixAmountSliderValue(double sliderValue) {
        model.setFixAmountSliderValue(sliderValue);
    }
}
