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

package bisq.desktop.main.content.mu_sig.offer.draft.create_offer.amount_and_price.amount.container.range.slider;

import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.offer.mu_sig.use_case.create_offer.CreateOfferUseCase;
import bisq.offer.mu_sig.use_case.create_offer.amount.AmountSelection;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class MuSigRangeAmountSliderController implements Controller {
    private final MuSigRangeAmountSliderModel model;
    @Getter
    private final MuSigRangeAmountSliderView view;
    private final AmountSelection amountSelection;
    private final Set<Pin> pins = new HashSet<>();

    public MuSigRangeAmountSliderController(CreateOfferUseCase createOfferUseCase) {
        amountSelection = createOfferUseCase.getAmountSelection();
        model = new MuSigRangeAmountSliderModel();
        view = new MuSigRangeAmountSliderView(model, this);
    }

    @Override
    public void onActivate() {
        // Direct observers re-read the domain at execution time so a queued projection cannot
        // overwrite the model with a stale snapshot after concurrent user input.
        pins.add(amountSelection.userSpecificTradeAmountLimitAsSliderValueObservable().addObserver(ignored ->
                UIThread.run(() -> model.getMaxAllowedValue().set(
                        amountSelection.getUserSpecificTradeAmountLimitAsSliderValue().orElse(1d)))));

        pins.add(amountSelection.minAmountSliderValueObservable().addObserver(ignored ->
                UIThread.run(() -> {
                    Double domainValue = amountSelection.getMinAmountSliderValue();
                    if (domainValue != null) {
                        model.getLowValue().set(domainValue);
                    }
                })));

        pins.add(amountSelection.maxAmountSliderValueObservable().addObserver(ignored ->
                UIThread.run(() -> {
                    Double domainValue = amountSelection.getMaxAmountSliderValue();
                    if (domainValue != null) {
                        model.getHighValue().set(domainValue);
                    }
                })));
    }

    @Override
    public void onDeactivate() {
        pins.forEach(Pin::unbind);
        pins.clear();
    }

    // Origin separation: only genuine gestures on the low thumb reach this method (the view marks
    // its own programmatic thumb writes); the value is clamped at the marker before the domain
    // write and the domain's published value flows back to the thumb through the projection above.
    void onLowValueChangedByUser(double value) {
        amountSelection.onSetMinTradeAmountFromSliderValue(Math.min(value, model.getMaxAllowedValue().get()));
    }

    void onHighValueChangedByUser(double value) {
        amountSelection.onSetMaxTradeAmountFromSliderValue(Math.min(value, model.getMaxAllowedValue().get()));
    }
}
