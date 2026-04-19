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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.container.range;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.MuSigAmountLayoutConstants;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import static bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.MuSigAmountLayoutConstants.PADDING;
import static bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.MuSigAmountLayoutConstants.WIDTH;

@Slf4j
public class MuSigRangeAmountView extends View<VBox, MuSigRangeAmountModel, MuSigRangeAmountController> {
    private final BisqMenuItem inputModeToggle;
    private final RangeAmountLayoutHelper layoutHelper;

    public MuSigRangeAmountView(MuSigRangeAmountModel model,
                                MuSigRangeAmountController controller,
                                HBox minAmountInput,
                                HBox maxAmountInput,
                                HBox minPassiveAmount,
                                HBox maxPassiveAmount,
                                HBox amountSlider) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.setMinWidth(MuSigAmountLayoutConstants.WIDTH);
        root.setMaxWidth(MuSigAmountLayoutConstants.WIDTH);

        layoutHelper = new RangeAmountLayoutHelper(model);
        layoutHelper.setVisible(false);
        layoutHelper.setManaged(false);

        inputModeToggle = new BisqMenuItem("flip-fields-arrows-green", "flip-fields-arrows-white");
        inputModeToggle.setTooltip(Res.get("muSig.offer.create.amount.selection.flipCurrenciesButton.tooltip"));

        HBox amountInputHBox = new HBox(5, minAmountInput, maxAmountInput);
        amountInputHBox.setMinWidth(WIDTH);
        amountInputHBox.setMaxWidth(WIDTH);
        amountInputHBox.setPadding(new Insets(0, PADDING, 0, PADDING));

        Pane amountInputHBoxPane = new Pane(layoutHelper, amountInputHBox);

        minPassiveAmount.setAlignment(Pos.CENTER_RIGHT);
        HBox passiveAmountAndToggle = new HBox(Spacer.fillHBox(), minPassiveAmount, maxPassiveAmount, Spacer.fillHBox(), inputModeToggle);
        passiveAmountAndToggle.setPadding(new Insets(0, 10, 0, 0));
        passiveAmountAndToggle.setAlignment(Pos.CENTER_RIGHT);

        VBox.setMargin(amountSlider, new Insets(32.5, 0, 0, 0));
        root.getChildren().addAll(amountInputHBoxPane, passiveAmountAndToggle, amountSlider);
    }

    @Override
    protected void onViewAttached() {
        layoutHelper.onViewAttached();
        inputModeToggle.setOnAction(e -> controller.onToggleInputMode());
    }

    @Override
    protected void onViewDetached() {
        layoutHelper.onViewDetached();
        inputModeToggle.setOnAction(null);
    }
}
