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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.limits;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MuSigAmountLimitsView extends View<HBox, MuSigAmountLimitsModel, MuSigAmountLimitsController> {
    private final Label minTradeAmountLimitValue, maxTradeAmountLimitValue, minTradeAmountLimitCode, maxTradeAmountLimitCode;

    public MuSigAmountLimitsView(MuSigAmountLimitsModel model,
                                 MuSigAmountLimitsController controller) {
        super(new HBox(10), model, controller);

        root.getStyleClass().add("amount-slider-limits");

        minTradeAmountLimitValue = new Label();
        minTradeAmountLimitValue.getStyleClass().add("value");
        minTradeAmountLimitCode = new Label();
        minTradeAmountLimitCode.getStyleClass().add("code");
        HBox minHBox = new HBox(2, minTradeAmountLimitValue, minTradeAmountLimitCode);
        minHBox.setAlignment(Pos.BASELINE_LEFT);
        Tooltip.install(minHBox, new BisqTooltip(model.getMinInUsd()));

        maxTradeAmountLimitValue = new Label();
        maxTradeAmountLimitValue.getStyleClass().add("value");
        maxTradeAmountLimitCode = new Label();
        maxTradeAmountLimitCode.getStyleClass().add("code");
        HBox maxHBox = new HBox(2, maxTradeAmountLimitValue, maxTradeAmountLimitCode);
        maxHBox.setAlignment(Pos.BASELINE_RIGHT);
        Tooltip.install(maxHBox, new BisqTooltip(model.getMaxInUsd()));

        root.getChildren().addAll(minHBox, Spacer.fillHBox(), maxHBox);
    }

    @Override
    protected void onViewAttached() {
        minTradeAmountLimitValue.textProperty().bind(model.getMin());
        minTradeAmountLimitCode.textProperty().bind(model.getCode());
        maxTradeAmountLimitValue.textProperty().bind(model.getMax());
        maxTradeAmountLimitCode.textProperty().bind(model.getCode());
    }

    @Override
    protected void onViewDetached() {
        minTradeAmountLimitValue.textProperty().unbind();
        minTradeAmountLimitCode.textProperty().unbind();
        maxTradeAmountLimitValue.textProperty().unbind();
        maxTradeAmountLimitCode.textProperty().unbind();
    }
}
