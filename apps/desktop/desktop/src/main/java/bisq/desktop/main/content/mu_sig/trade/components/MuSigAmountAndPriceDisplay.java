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

package bisq.desktop.main.content.mu_sig.trade.components;

import bisq.common.market.Market;
import bisq.contract.mu_sig.MuSigContract;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.presentation.formatters.PriceFormatter;
import bisq.trade.mu_sig.MuSigTradeFormatter;
import bisq.trade.mu_sig.MuSigTradeUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import static bisq.desktop.components.helpers.LabeledValueRowFactory.getValueLabel;

public class MuSigAmountAndPriceDisplay extends HBox {
    private final Label nonBtcAmountLabel;
    private final Label nonBtcCurrencyLabel;
    private final Label btcAmountLabel;
    private final Label priceLabel;
    private final Label priceCodesLabel;

    public MuSigAmountAndPriceDisplay() {
        super(5);
        setAlignment(Pos.BASELINE_LEFT);

        nonBtcAmountLabel = getValueLabel();
        nonBtcCurrencyLabel = new Label();
        nonBtcCurrencyLabel.getStyleClass().addAll("text-fill-white", "small-text");

        Label openParenthesisLabel = new Label("(");
        openParenthesisLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        btcAmountLabel = getValueLabel();
        btcAmountLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        btcAmountLabel.setPadding(new Insets(0, 5, 0, 0));
        Label btcLabel = new Label("BTC");
        btcLabel.getStyleClass().addAll("text-fill-grey-dimmed", "small-text");
        Label closingParenthesisLabel = new Label(")");
        closingParenthesisLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        HBox btcAmountBox = new HBox(openParenthesisLabel, btcAmountLabel, btcLabel, closingParenthesisLabel);
        btcAmountBox.setAlignment(Pos.BASELINE_LEFT);

        Label atLabel = new Label("@");
        atLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        priceLabel = getValueLabel();
        priceCodesLabel = new Label();
        priceCodesLabel.getStyleClass().addAll("text-fill-white", "small-text");

        getChildren().addAll(nonBtcAmountLabel, nonBtcCurrencyLabel, btcAmountBox, atLabel, priceLabel, priceCodesLabel);
    }

    public void setContract(MuSigContract contract) {
        MuSigOffer offer = contract.getOffer();
        Market market = offer.getMarket();
        boolean isBaseCurrencyBitcoin = market.isBaseCurrencyBitcoin();

        nonBtcAmountLabel.setText(MuSigTradeFormatter.formatNonBtcSideAmount(contract));
        nonBtcCurrencyLabel.setText(market.getRelevantCurrencyCode());
        btcAmountLabel.setText(MuSigTradeFormatter.formatBtcSideAmount(contract));
        priceLabel.setText(PriceFormatter.format(MuSigTradeUtils.getPriceQuote(contract), isBaseCurrencyBitcoin));
        priceCodesLabel.setText(market.getMarketCodes());
    }
}
