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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.range.slider;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.RangeSlider;
import bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.MuSigAmountLayoutConstants;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Set;

import static bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.SliderTrackStyleHelper.getSliderTrackStyle;

@Slf4j
public class MuSigRangeAmountSliderView extends View<HBox, MuSigRangeAmountSliderModel, MuSigRangeAmountSliderController> {
    private final RangeSlider slider;
    private final Set<Subscription> subscriptions = new HashSet<>();

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
        slider.getLowValue().bindBidirectional(model.getLowValue());
        slider.getHighValue().bindBidirectional(model.getHighValue());

        subscriptions.add(EasyBind.subscribe(slider.getHighValue(), value -> {
            double maxAllowedValue = model.getMaxAllowedValue().get();
            if (value.doubleValue() > maxAllowedValue) {
                slider.setHighValue(maxAllowedValue);
            }

            String style = getSliderTrackStyle(maxAllowedValue);
            slider.setStyle(style);
        }));
        subscriptions.add(EasyBind.subscribe(slider.getLowValue(), value -> {
            double maxAllowedValue = model.getMaxAllowedValue().get();
            if (value.doubleValue() > maxAllowedValue) {
                slider.setLowValue(maxAllowedValue);
            }
        }));
    }

    @Override
    protected void onViewDetached() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        slider.getLowValue().unbindBidirectional(model.getLowValue());
        slider.getHighValue().unbindBidirectional(model.getHighValue());
    }
}
