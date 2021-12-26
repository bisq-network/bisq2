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

package network.misq.desktop.main.content.createoffer.assetswap.amounts;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import network.misq.desktop.common.view.View;
import network.misq.desktop.components.controls.AutoTooltipButton;
import network.misq.desktop.components.controls.AutoTooltipLabel;
import network.misq.desktop.components.controls.AutocompleteComboBox;

public class SetAmountsView extends View<HBox, SetAmountsModel, SetAmountsController> {

    private AutocompleteComboBox<String> askCurrencyComboBox;
    private AutoTooltipButton flipButton;
    private AutocompleteComboBox<String> bidCurrencyComboBox;

    public SetAmountsView(SetAmountsModel model, SetAmountsController controller) {
        super(new HBox(), model, controller);
    }

    protected void setupView() {
        Label askCurrencyLabel = new AutoTooltipLabel("I want (ask):");
        askCurrencyLabel.setPadding(new Insets(4, 8, 0, 0));

        askCurrencyComboBox = new AutocompleteComboBox<>();
        askCurrencyComboBox.getEditor().getStyleClass().add("combo-box-editor-bold");

        flipButton = new AutoTooltipButton("<- Flip ->");

        Label bidCurrencyLabel = new AutoTooltipLabel("I give (bid):");
        bidCurrencyLabel.setPadding(new Insets(4, 8, 0, 60));
        bidCurrencyComboBox = new AutocompleteComboBox<>();
        bidCurrencyComboBox.getEditor().getStyleClass().add("combo-box-editor-bold");

        HBox.setMargin(flipButton, new Insets(-2, 0, 0, 60));

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        root.getChildren().addAll(askCurrencyLabel, askCurrencyComboBox, flipButton, bidCurrencyLabel,
                bidCurrencyComboBox);
    }

    @Override
    protected void configModel() {
        askCurrencyComboBox.setAutocompleteItems(model.getCurrencies());
        askCurrencyComboBox.getSelectionModel().select(model.getSelectedAskCurrency().get());

        bidCurrencyComboBox.setAutocompleteItems(model.getCurrencies());
        bidCurrencyComboBox.getSelectionModel().select(model.getSelectedBidCurrency().get());
    }

    @Override
    protected void configController() {
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
