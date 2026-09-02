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

package bisq.desktop.main.content.mu_sig.offer.draft.create_offer.amount_and_price.amount.container.fix.slider;

import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.offer.mu_sig.use_case.create_offer.CreateOfferUseCase;
import bisq.offer.mu_sig.use_case.create_offer.amount.AmountSelection;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class MuSigFixAmountSliderController implements Controller {
    private final MuSigFixAmountSliderModel model;
    @Getter
    private final MuSigFixAmountSliderView view;
    private final AmountSelection amountSelection;
    private final Set<Subscription> subscriptions = new HashSet<>();
    private final Set<Pin> pins = new HashSet<>();

    public MuSigFixAmountSliderController(CreateOfferUseCase createOfferUseCase) {
        amountSelection = createOfferUseCase.getAmountSelection();
        model = new MuSigFixAmountSliderModel();
        view = new MuSigFixAmountSliderView(model, this);
    }

    @Override
    public void onActivate() {
        subscriptions.add(EasyBind.subscribe(model.getGetSliderValue(),
                value -> {
                    if (value != null) {
                        amountSelection.onSetFixTradeAmountFromSliderValue(clamp(value.doubleValue()));
                    }
                }));

        pins.add(amountSelection.userSpecificTradeAmountLimitAsSliderValueObservable().addObserver(value -> {
            UIThread.run(() -> {
                if (value != null) {
                    model.getMaxAllowedValue().set(value.orElse(1d));
                }
            });
        }));

        pins.add(FxBindings.bind(model.getGetSliderValue())
                .to(amountSelection.fixAmountSliderValueObservable()));
    }

    @Override
    public void onDeactivate() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        pins.forEach(Pin::unbind);
        pins.clear();
    }

    private double clamp(double doubleValue) {
        return Math.min(doubleValue, model.getMaxAllowedValue().get());
    }
}
