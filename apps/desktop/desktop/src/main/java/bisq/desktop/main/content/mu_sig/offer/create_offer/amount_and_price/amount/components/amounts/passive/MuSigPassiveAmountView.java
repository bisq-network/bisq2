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
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;

import static bisq.common.encoding.UniCodeTable.EN_DASH_SYMBOL;

@Slf4j
public class MuSigPassiveAmountView extends bisq.desktop.common.view.View<HBox, MuSigPassiveAmountModel, MuSigPassiveAmountController> {
    private static final double ICON_SCALE = 0.85;
    private static final double ICON_OPACITY = 0.5;

    private final Label amount;
    private final Label code;
    private final BitcoinAmountDisplay bitcoinAmountDisplay;
    @Nullable
    private BisqTooltip tooltip;
    private final HBox amountBox, bitcoinAmountDisplayBox;
    private Subscription useBitcoinDisplayPin;

    MuSigPassiveAmountView(MuSigPassiveAmountModel model, MuSigPassiveAmountController controller) {
        super(new HBox(), model, controller);

        root.getStyleClass().add("amount-display");

        amount = new Label();
        amount.getStyleClass().add("value");

        code = new Label();
        code.getStyleClass().add("code");

        Label infoIcon = new Label();
        infoIcon.setGraphic(ImageUtil.getImageViewById("info"));
        infoIcon.setScaleX(ICON_SCALE);
        infoIcon.setScaleY(ICON_SCALE);
        infoIcon.setOpacity(ICON_OPACITY);

        boolean isLeftSideRangeAmount = model.isLeftSideRangeAmount();
        bitcoinAmountDisplay = new BitcoinAmountDisplay("0", !isLeftSideRangeAmount);
        bitcoinAmountDisplay.setTextAlignment(TextAlignment.LEFT);
        bitcoinAmountDisplay.applyCompactConfig(15, 12, 28);

        bitcoinAmountDisplayBox = new HBox(7.5, bitcoinAmountDisplay);

        HBox.setMargin(amount, new Insets(0, 0, 3, 0));
        if (isLeftSideRangeAmount) {
            Label dash1 = createDash();
            HBox.setMargin(dash1, new Insets(0, 0, 0, 2));
            amountBox = new HBox(5, amount, dash1);

            Label dash2 = createDash();
            dash2.setTranslateY(-1);

            bitcoinAmountDisplayBox.getChildren().add(dash2);
        } else {
            tooltip = new BisqTooltip(BisqTooltip.Style.DARK);
            infoIcon.setTooltip(tooltip);
            amountBox = new HBox(5, amount, code);
        }
        amountBox.setAlignment(Pos.BASELINE_LEFT);


        root.setMinHeight(30);
        root.setMaxHeight(30);

        HBox.setMargin(amountBox, new Insets(0, 6, 0, 0));
        HBox.setMargin(infoIcon, new Insets(0, 0, 4, 0));
        HBox.setMargin(bitcoinAmountDisplayBox, new Insets(1.5, 6, 0, 0));
        if (isLeftSideRangeAmount) {
            root.getChildren().addAll(amountBox, bitcoinAmountDisplayBox);
        } else {
            root.getChildren().addAll(amountBox, bitcoinAmountDisplayBox, infoIcon);
        }
    }


    @Override
    protected void onViewAttached() {
        amount.textProperty().bind(model.getFormattedAmount());
        code.textProperty().bind(model.getCode());
        bitcoinAmountDisplay.btcAmountProperty().bind(model.getFormattedAmount());

        if (tooltip != null) {
            tooltip.textProperty().bind(model.getTooltip());
        }

        useBitcoinDisplayPin = EasyBind.subscribe(model.getUseBitcoinDisplay(), useBitcoinDisplay -> {
            bitcoinAmountDisplayBox.setVisible(useBitcoinDisplay);
            bitcoinAmountDisplayBox.setManaged(useBitcoinDisplay);
            amountBox.setVisible(!useBitcoinDisplay);
            amountBox.setManaged(!useBitcoinDisplay);
        });
    }

    @Override
    protected void onViewDetached() {
        amount.textProperty().unbind();
        code.textProperty().unbind();
        bitcoinAmountDisplay.btcAmountProperty().unbind();
        if (tooltip != null) {
            tooltip.textProperty().unbind();
        }

        useBitcoinDisplayPin.unsubscribe();
    }

    private Label createDash() {
        Label dash = new Label(EN_DASH_SYMBOL);
        dash.getStyleClass().add("value");
        return dash;
    }
}
