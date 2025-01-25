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

package bisq.desktop.main.content.settings.trade;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.Switch;
import bisq.desktop.components.controls.validator.NumberValidator;
import bisq.desktop.components.controls.validator.ValidatorBase;
import bisq.desktop.main.content.settings.SettingsViewUtils;
import bisq.i18n.Res;
import bisq.user.reputation.ReputationScore;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeSettingsView extends View<VBox, TradeSettingsModel, TradeSettingsController> {
    private static final ValidatorBase REPUTATION_SCORE_VALIDATOR =
            new NumberValidator(Res.get("settings.trade.minReputationScore.invalid", ReputationScore.MAX_VALUE),
                    0, ReputationScore.MAX_VALUE);

    private static final double TEXT_FIELD_WIDTH = 500;

    private final Switch closeMyOfferWhenTaken;
    private final MaterialTextField maxTradePriceDeviation, numDaysAfterRedactingTradeData;

    public TradeSettingsView(TradeSettingsModel model, TradeSettingsController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_LEFT);

        // Trade
        Label tradeHeadline = SettingsViewUtils.getHeadline(Res.get("settings.trade.headline"));

        closeMyOfferWhenTaken = new Switch(Res.get("settings.trade.closeMyOfferWhenTaken"));

        maxTradePriceDeviation = new MaterialTextField(Res.get("settings.trade.maxTradePriceDeviation"),
                null, Res.get("settings.trade.maxTradePriceDeviation.help"));
        maxTradePriceDeviation.setValidators(model.getMaxTradePriceDeviationValidator());
        maxTradePriceDeviation.setMaxWidth(TEXT_FIELD_WIDTH);
        maxTradePriceDeviation.setStringConverter(model.getMaxTradePriceDeviationConverter());

        numDaysAfterRedactingTradeData = new MaterialTextField(Res.get("settings.trade.numDaysAfterRedactingTradeData"),
                null, Res.get("settings.trade.numDaysAfterRedactingTradeData.help"));
        numDaysAfterRedactingTradeData.setValidators(model.getNumDaysAfterRedactingTradeDataValidator());
        numDaysAfterRedactingTradeData.setMaxWidth(TEXT_FIELD_WIDTH);
        numDaysAfterRedactingTradeData.setStringConverter(model.getNumDaysAfterRedactingTradeDataConverter());

        VBox.setMargin(maxTradePriceDeviation, new Insets(15, 0, 0, 0));
        VBox tradeVBox = new VBox(10, closeMyOfferWhenTaken, maxTradePriceDeviation, numDaysAfterRedactingTradeData);

        VBox.setMargin(tradeVBox, new Insets(0, 5, 0, 5));
        VBox contentBox = new VBox(50);
        contentBox.getChildren().addAll(tradeHeadline, SettingsViewUtils.getLineAfterHeadline(contentBox.getSpacing()), tradeVBox);
        contentBox.getStyleClass().add("bisq-common-bg");
        root.getChildren().add(contentBox);
        root.setPadding(new Insets(0, 40, 20, 40));
    }

    @Override
    protected void onViewAttached() {
        closeMyOfferWhenTaken.selectedProperty().bindBidirectional(model.getCloseMyOfferWhenTaken());

        Bindings.bindBidirectional(maxTradePriceDeviation.textProperty(), model.getMaxTradePriceDeviation(),
                model.getMaxTradePriceDeviationConverter());
        maxTradePriceDeviation.validate(); // Needed to show help field as its shown only if input is valid

        Bindings.bindBidirectional(numDaysAfterRedactingTradeData.textProperty(), model.getNumDaysAfterRedactingTradeData(),
                model.getNumDaysAfterRedactingTradeDataConverter());
        numDaysAfterRedactingTradeData.validate(); // Needed to show help field as its shown only if input is valid
    }

    @Override
    protected void onViewDetached() {
        closeMyOfferWhenTaken.selectedProperty().unbindBidirectional(model.getCloseMyOfferWhenTaken());

        Bindings.unbindBidirectional(maxTradePriceDeviation.textProperty(), model.getMaxTradePriceDeviation());
        maxTradePriceDeviation.resetValidation();

        Bindings.unbindBidirectional(numDaysAfterRedactingTradeData.textProperty(), model.getNumDaysAfterRedactingTradeData());
        numDaysAfterRedactingTradeData.resetValidation();
    }
}
