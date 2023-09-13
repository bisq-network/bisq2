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

package bisq.desktop.main.content.bisq_easy.trade_wizard.amount;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.main.content.bisq_easy.components.AmountComponent;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeWizardAmountView extends View<VBox, TradeWizardAmountModel, TradeWizardAmountController> {
    private final Button toggleButton;
    private final VBox minAmountRoot;
    private final Label headLineLabel;

    public TradeWizardAmountView(TradeWizardAmountModel model, TradeWizardAmountController controller, AmountComponent minAmountComponent, AmountComponent maxOrFixAmountComponent) {
        super(new VBox(10), model, controller);

        root.setAlignment(Pos.TOP_CENTER);

        headLineLabel = new Label();
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        minAmountRoot = minAmountComponent.getView().getRoot();
        HBox amountBox = new HBox(30, minAmountRoot, maxOrFixAmountComponent.getView().getRoot());
        amountBox.setAlignment(Pos.CENTER);

        toggleButton = new Button(Res.get("bisqEasy.createOffer.amount.addMinAmountOption"));
        toggleButton.getStyleClass().add("outlined-button");
        toggleButton.setMinWidth(AmountComponent.View.AMOUNT_BOX_WIDTH);

        VBox.setMargin(headLineLabel, new Insets(-30, 0, 10, 0));
        VBox.setMargin(toggleButton, new Insets(25, 0, 0, 0));
        root.getChildren().addAll(Spacer.fillVBox(), headLineLabel, amountBox, toggleButton, Spacer.fillVBox());
    }

    @Override
    protected void onViewAttached() {
        headLineLabel.setText(model.getHeadline());
        minAmountRoot.visibleProperty().bind(model.getIsMinAmountEnabled());
        minAmountRoot.managedProperty().bind(model.getIsMinAmountEnabled());
        toggleButton.visibleProperty().bind(model.getShowRangeAmounts());
        toggleButton.managedProperty().bind(model.getShowRangeAmounts());
        toggleButton.textProperty().bind(model.getToggleButtonText());

        toggleButton.setOnAction(e -> controller.onToggleMinAmountVisibility());
    }

    @Override
    protected void onViewDetached() {
        minAmountRoot.visibleProperty().unbind();
        minAmountRoot.managedProperty().unbind();
        toggleButton.textProperty().unbind();

        toggleButton.setOnAction(null);
    }
}
