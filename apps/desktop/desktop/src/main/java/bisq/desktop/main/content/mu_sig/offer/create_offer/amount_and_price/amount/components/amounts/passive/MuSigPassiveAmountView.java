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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.passive;

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.BitcoinAmountDisplay;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.text.TextAlignment;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import static bisq.common.encoding.UniCodeTable.EN_DASH_SYMBOL;

public class MuSigPassiveAmountView extends bisq.desktop.common.view.View<HBox, MuSigPassiveAmountModel, MuSigPassiveAmountController> {
    private static final double ICON_SCALE = 0.85;
    private static final double ICON_OPACITY = 0.5;

    private final Label amount, code, infoIcon;
    private final BitcoinAmountDisplay bitcoinAmountDisplay;
    private final BisqTooltip tooltip;
    private final HBox amountAndCode;
    private final Label dash;
    private Subscription isBtcPin;

    MuSigPassiveAmountView(MuSigPassiveAmountModel model, MuSigPassiveAmountController controller) {
        super(new HBox(), model, controller);

        root.getStyleClass().add("amount-display");

        amount = new Label();
        amount.setPadding(new Insets(0, 7, 3, 0));
        amount.getStyleClass().add("value");

        code = new Label();
        code.getStyleClass().add("code");

        dash = new Label(EN_DASH_SYMBOL);
        dash.getStyleClass().add("value");

        if (model.isLeftSideRangeAmount()) {
            amountAndCode = new HBox(amount, dash);
        } else {
            amountAndCode = new HBox(amount, code);
        }
        amountAndCode.setAlignment(Pos.BASELINE_LEFT);

        bitcoinAmountDisplay = new BitcoinAmountDisplay("0", true);
        bitcoinAmountDisplay.setTextAlignment(TextAlignment.LEFT);
        bitcoinAmountDisplay.setTranslateY(2);
        bitcoinAmountDisplay.applyCompactConfig(15, 12, 28);

        tooltip = new BisqTooltip(BisqTooltip.Style.DARK);

        infoIcon = new Label();
        infoIcon.setGraphic(ImageUtil.getImageViewById("info"));
        infoIcon.setTooltip(tooltip);
        infoIcon.setScaleX(ICON_SCALE);
        infoIcon.setScaleY(ICON_SCALE);
        infoIcon.setOpacity(ICON_OPACITY);
        infoIcon.setPadding(new Insets(0, 0, 5, 0));

        HBox.setMargin(infoIcon, new Insets(0, 0, 0, 4));
        HBox.setMargin(bitcoinAmountDisplay, new Insets(0, 7, 0, 0));
        root.getChildren().addAll(amountAndCode, bitcoinAmountDisplay, infoIcon);
    }

    @Override
    protected void onViewAttached() {
        amount.textProperty().bind(model.getFormattedAmount());
        code.textProperty().bind(model.getCode());
        bitcoinAmountDisplay.btcAmountProperty().bind(model.getFormattedAmount());

        tooltip.textProperty().bind(model.getTooltip());

        isBtcPin = EasyBind.subscribe(model.getUseBitcoinDisplay(), useBitcoinDisplay -> {
            infoIcon.setTranslateY(model.getUseBitcoinDisplay().get() ? -2 : 0);
            bitcoinAmountDisplay.setVisible(useBitcoinDisplay);
            bitcoinAmountDisplay.setManaged(useBitcoinDisplay);
            amountAndCode.setVisible(!useBitcoinDisplay);
            amountAndCode.setManaged(!useBitcoinDisplay);
        });

    }


    @Override
    protected void onViewDetached() {
        amount.textProperty().unbind();
        code.textProperty().unbind();
        bitcoinAmountDisplay.btcAmountProperty().unbind();
        tooltip.textProperty().unbind();

        isBtcPin.unsubscribe();
    }
}
