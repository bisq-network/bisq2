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

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.RangeSlider;
import bisq.desktop.main.content.mu_sig.offer.draft.amount_components.MuSigAmountLayoutConstants;
import javafx.beans.value.ChangeListener;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Set;

import static bisq.desktop.main.content.mu_sig.offer.draft.amount_components.SliderTrackStyleHelper.getSliderTrackStyle;

@Slf4j
public class MuSigRangeAmountSliderView extends View<HBox, MuSigRangeAmountSliderModel, MuSigRangeAmountSliderController> {
    private final RangeSlider slider;
    private final Set<Subscription> subscriptions = new HashSet<>();
    private boolean applyingModelValue;

    public MuSigRangeAmountSliderView(MuSigRangeAmountSliderModel model,
                                      MuSigRangeAmountSliderController controller) {
        super(new HBox(10), model, controller);

        slider = new RangeSlider();
        slider.setMin(0);
        slider.setMax(1);
        slider.setMinWidth(MuSigAmountLayoutConstants.WIDTH);
        slider.setMaxWidth(MuSigAmountLayoutConstants.WIDTH);

        root.getChildren().add(slider);
    }

    @Override
    protected void onViewAttached() {
        // Origin separation instead of a bidirectional binding: the model carries the domain
        // values and is applied to the thumbs under a guard; every unguarded thumb change (mouse,
        // keyboard, accessibility) is by construction a user gesture and is forwarded to the
        // controller. The registration fire of the model subscriptions applies the initial values
        // under the same guard.
        subscriptions.add(EasyBind.subscribe(model.getLowValue(), value -> {
            if (value != null) {
                applyLowValueFromModel(value.doubleValue());
            }
        }));
        subscriptions.add(EasyBind.subscribe(model.getHighValue(), value -> {
            if (value != null) {
                applyHighValueFromModel(value.doubleValue());
            }
        }));
        slider.getLowValue().addListener(lowGestureListener);
        slider.getHighValue().addListener(highGestureListener);
        subscriptions.add(EasyBind.subscribe(model.getMaxAllowedValue(), maxAllowedValue ->
                slider.setStyle(getSliderTrackStyle(maxAllowedValue.doubleValue()))));
    }

    @Override
    protected void onViewDetached() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        slider.getLowValue().removeListener(lowGestureListener);
        slider.getHighValue().removeListener(highGestureListener);
    }

    private final ChangeListener<Number> lowGestureListener = (observable, oldValue, newValue) -> {
        if (applyingModelValue) {
            return;
        }
        controller.onLowValueChangedByUser(newValue.doubleValue());
        // The domain suppresses equal republications, so a gesture whose clamped result equals
        // the current selection produces no model change; the thumb is restored explicitly.
        applyLowValueFromModel(model.getLowValue().get());
    };

    private final ChangeListener<Number> highGestureListener = (observable, oldValue, newValue) -> {
        if (applyingModelValue) {
            return;
        }
        controller.onHighValueChangedByUser(newValue.doubleValue());
        applyHighValueFromModel(model.getHighValue().get());
    };

    private void applyLowValueFromModel(double value) {
        applyingModelValue = true;
        try {
            slider.setLowValue(value);
        } finally {
            applyingModelValue = false;
        }
    }

    private void applyHighValueFromModel(double value) {
        applyingModelValue = true;
        try {
            slider.setHighValue(value);
        } finally {
            applyingModelValue = false;
        }
    }
}
