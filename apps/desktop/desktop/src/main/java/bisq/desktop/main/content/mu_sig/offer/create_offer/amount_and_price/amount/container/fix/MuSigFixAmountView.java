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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.container.fix;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
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
public class MuSigFixAmountView extends View<VBox, MuSigFixAmountModel, MuSigFixAmountController> {
    private final BisqMenuItem inputModeToggle;
    private final FixAmountLayoutHelper layoutHelper;

    public MuSigFixAmountView(MuSigFixAmountModel model,
                              MuSigFixAmountController controller,
                              HBox amountInput,
                              HBox passiveAmount,
                              VBox amountSlider) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);

        layoutHelper = new FixAmountLayoutHelper(model);
        layoutHelper.setVisible(false);
        layoutHelper.setManaged(false);

        amountInput.setMinWidth(WIDTH);
        amountInput.setMaxWidth(WIDTH);
        amountInput.setPadding(new Insets(0, PADDING, 0, PADDING));

        inputModeToggle = new BisqMenuItem("flip-fields-arrows-green", "flip-fields-arrows-white");
        inputModeToggle.setTooltip(Res.get("muSig.offer.create.amount.selection.flipCurrenciesButton.tooltip"));

        HBox.setMargin(passiveAmount, new Insets(0, 0, 0, 20));
        HBox passiveAmountAndToggle = new HBox(Spacer.fillHBox(), passiveAmount, Spacer.fillHBox(), inputModeToggle);
        passiveAmountAndToggle.setPadding(new Insets(0, 10, 0, 10));

        Pane amountInputHBoxPane = new Pane(layoutHelper, amountInput);

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
