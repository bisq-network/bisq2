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

import bisq.desktop.common.converters.Converters;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.Switch;
import bisq.desktop.components.controls.validator.NumberValidator;
import bisq.desktop.components.controls.validator.PercentageValidator;
import bisq.desktop.components.controls.validator.ValidatorBase;
import bisq.desktop.main.content.settings.SettingsViewUtils;
import bisq.i18n.Res;
import bisq.trade.bisq_easy.BisqEasyTradeService;
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

    private static final ValidatorBase MAX_TRADE_PRICE_DEVIATION_VALIDATOR =
            new PercentageValidator(Res.get("settings.trade.maxTradePriceDeviation.invalid", BisqEasyTradeService.MAX_TRADE_PRICE_DEVIATION * 100),
                    0.01, BisqEasyTradeService.MAX_TRADE_PRICE_DEVIATION);
    private static final double TEXT_FIELD_WIDTH = 500;

    private final Switch closeMyOfferWhenTaken;
    private final MaterialTextField maxTradePriceDeviation;

    public TradeSettingsView(TradeSettingsModel model, TradeSettingsController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_LEFT);

        // Trade
        Label tradeHeadline = SettingsViewUtils.getHeadline(Res.get("settings.trade.headline"));

        closeMyOfferWhenTaken = new Switch(Res.get("settings.trade.closeMyOfferWhenTaken"));

        maxTradePriceDeviation = new MaterialTextField(Res.get("settings.trade.maxTradePriceDeviation"),
                null, Res.get("settings.trade.maxTradePriceDeviation.help"));
        maxTradePriceDeviation.setValidators(MAX_TRADE_PRICE_DEVIATION_VALIDATOR);
        maxTradePriceDeviation.setMaxWidth(TEXT_FIELD_WIDTH);
        maxTradePriceDeviation.setStringConverter(Converters.PERCENTAGE_STRING_CONVERTER);

        VBox.setMargin(maxTradePriceDeviation, new Insets(15, 0, 0, 0));
        VBox tradeVBox = new VBox(10, closeMyOfferWhenTaken, maxTradePriceDeviation);

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
                Converters.PERCENTAGE_STRING_CONVERTER);
        maxTradePriceDeviation.validate(); // Needed to show help field as its shown only if input is valid
    }

    @Override
    protected void onViewDetached() {
        closeMyOfferWhenTaken.selectedProperty().unbindBidirectional(model.getCloseMyOfferWhenTaken());

        Bindings.unbindBidirectional(maxTradePriceDeviation.textProperty(), model.getMaxTradePriceDeviation());

        maxTradePriceDeviation.resetValidation();
    }
}
