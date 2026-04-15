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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.slider;

import bisq.desktop.common.view.View;
import javafx.geometry.Pos;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MuSigActiveAmountSliderView extends View<VBox, MuSigActiveAmountSliderModel, MuSigAmountSliderController> {

    private final Slider slider;

    public MuSigActiveAmountSliderView(MuSigActiveAmountSliderModel model,
                                       MuSigAmountSliderController controller) {
        super(new VBox(10), model, controller);

        slider = new Slider(0, 1, 0);
        root.getChildren().addAll(slider);
        root.setAlignment(Pos.TOP_CENTER);
    }

    @Override
    protected void onViewAttached() {
        slider.valueProperty().bind(model.getValue());
    }

    @Override
    protected void onViewDetached() {
        slider.valueProperty().unbind();
    }
}
