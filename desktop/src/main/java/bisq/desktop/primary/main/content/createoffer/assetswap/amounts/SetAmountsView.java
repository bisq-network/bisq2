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

package bisq.desktop.primary.main.content.createoffer.assetswap.amounts;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqComboBox;
import bisq.desktop.components.controls.BisqLabel;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

public class SetAmountsView extends View<HBox, SetAmountsModel, SetAmountsController> {

    private BisqComboBox<String> askCurrencyComboBox;
    private BisqButton flipButton;
    private BisqComboBox<String> bidCurrencyComboBox;

    public SetAmountsView(SetAmountsModel model, SetAmountsController controller) {
        super(new HBox(), model, controller);
    }

    protected void initialize() {
        Label askCurrencyLabel = new BisqLabel("I want (ask):");
        askCurrencyLabel.setPadding(new Insets(4, 8, 0, 0));

        askCurrencyComboBox = new BisqComboBox<>();
        askCurrencyComboBox.getEditor().getStyleClass().add("combo-box-editor-bold");

        flipButton = new BisqButton("<- Flip ->");

        Label bidCurrencyLabel = new BisqLabel("I give (bid):");
        bidCurrencyLabel.setPadding(new Insets(4, 8, 0, 60));
        bidCurrencyComboBox = new BisqComboBox<>();
        bidCurrencyComboBox.getEditor().getStyleClass().add("combo-box-editor-bold");

        HBox.setMargin(flipButton, new Insets(-2, 0, 0, 60));

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        root.getChildren().addAll(askCurrencyLabel, askCurrencyComboBox, flipButton, bidCurrencyLabel,
                bidCurrencyComboBox);
    }

    @Override
    protected void onViewAttached() {
        askCurrencyComboBox.setItems(model.getCurrencies());
        askCurrencyComboBox.getSelectionModel().select(model.getSelectedAskCurrency().get());

        bidCurrencyComboBox.setItems(model.getCurrencies());
        bidCurrencyComboBox.getSelectionModel().select(model.getSelectedBidCurrency().get());
        flipButton.setOnAction(e -> {
            controller.onFlipCurrencies();
            String ask = askCurrencyComboBox.getSelectionModel().getSelectedItem();
            String bid = bidCurrencyComboBox.getSelectionModel().getSelectedItem();
            askCurrencyComboBox.getSelectionModel().select(bid);
            bidCurrencyComboBox.getSelectionModel().select(ask);
        });
        askCurrencyComboBox.setOnAction(e -> controller.onSelectAskCurrency(askCurrencyComboBox.getSelectionModel().getSelectedItem()));
        bidCurrencyComboBox.setOnAction(e -> controller.onSelectBidCurrency(bidCurrencyComboBox.getSelectionModel().getSelectedItem()));
    }
}
