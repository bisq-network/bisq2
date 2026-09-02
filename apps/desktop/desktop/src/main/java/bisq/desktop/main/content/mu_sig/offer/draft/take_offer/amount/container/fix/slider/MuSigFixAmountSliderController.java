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

package bisq.desktop.main.content.mu_sig.offer.draft.take_offer.amount.container.fix.slider;

import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.offer.mu_sig.use_case.take_offer.TakeOfferUseCase;
import bisq.offer.mu_sig.use_case.take_offer.amount.TakeOfferAmountService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class MuSigFixAmountSliderController implements Controller {
    private final MuSigFixAmountSliderModel model;
    @Getter
    private final MuSigFixAmountSliderView view;
    private final TakeOfferUseCase takeOfferService;
    private final TakeOfferAmountService takeOfferAmountService;
    private final Set<Pin> pins = new HashSet<>();

    public MuSigFixAmountSliderController(TakeOfferUseCase takeOfferService) {
        this.takeOfferService = takeOfferService;
        takeOfferAmountService = takeOfferService.getAmountService();
        model = new MuSigFixAmountSliderModel();
        view = new MuSigFixAmountSliderView(model, this);
    }

    @Override
    public void onActivate() {
        // Direct observers re-read the domain at execution time: a queued projection carrying
        // a captured value could overwrite the model with a stale snapshot after concurrent
        // user input.
        pins.add(takeOfferAmountService.userSpecificTradeAmountLimitAsSliderValueObservable().addObserver(ignored ->
                UIThread.run(() -> model.getMaxAllowedValue().set(
                        takeOfferAmountService.getUserSpecificTradeAmountLimitAsSliderValue().orElse(1d)))));

        pins.add(takeOfferAmountService.fixAmountSliderValueObservable().addObserver(ignored ->
                UIThread.run(() -> {
                    Double domainValue = takeOfferAmountService.getFixAmountSliderValue();
                    if (domainValue != null) {
                        model.getGetSliderValue().set(domainValue);
                    }
                })));
    }

    @Override
    public void onDeactivate() {
        pins.forEach(Pin::unbind);
        pins.clear();
    }

    // Origin separation: only genuine slider gestures reach this method (the view marks its
    // own programmatic thumb writes); the user's value is clamped at the marker before the
    // domain write - user-initiated clamping is allowed and visible, and the domain's
    // published value flows back to the thumb through the projection above.
    void onSliderValueChangedByUser(double value) {
        takeOfferService.setFixTradeAmountFromSliderValue(Math.min(value, model.getMaxAllowedValue().get()));
    }
}
