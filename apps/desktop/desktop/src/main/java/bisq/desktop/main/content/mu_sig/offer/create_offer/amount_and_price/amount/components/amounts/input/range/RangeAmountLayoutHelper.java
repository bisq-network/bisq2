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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.range;

import bisq.common.encoding.UniCodeTable;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.AmountTextInputLayout;
import javafx.geometry.Bounds;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Set;

import static bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.AmountTextInputLayout.PADDING;
import static bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.AmountTextInputLayout.WIDTH;

@Slf4j
public class RangeAmountLayoutHelper extends HBox {
    private final MuSigRangeAmountModel model;
    private final Text minAmount, dash, maxAmount;
    private double lastSize;
    private final Set<Subscription> subscriptions = new HashSet<>();

    public RangeAmountLayoutHelper(MuSigRangeAmountModel model) {
        super(5);
        this.model = model;

        setMouseTransparent(true);
        setMinWidth(WIDTH);
        setMaxWidth(WIDTH);

        minAmount = new Text();
        minAmount.getStyleClass().add("amount-input-helper");
        dash = new Text(UniCodeTable.EN_DASH_SYMBOL);
        dash.getStyleClass().add("amount-input-helper");
        maxAmount = new Text();
        maxAmount.getStyleClass().add("amount-input-helper");

        Region leftPadding = Spacer.width(PADDING);
        Region rightPadding = Spacer.width(PADDING);
        getChildren().addAll(leftPadding, minAmount, dash, maxAmount, rightPadding);
    }

    void onViewAttached() {
        minAmount.textProperty().bind(model.getMinAmountInputText());
        maxAmount.textProperty().bind(model.getMaxAmountInputText());
        model.getMinAmountWidth().bind(minAmount.layoutBoundsProperty().map(Bounds::getWidth));
        model.getDashWidth().bind(dash.layoutBoundsProperty().map(Bounds::getWidth));
        model.getMaxAmountWidth().bind(maxAmount.layoutBoundsProperty().map(Bounds::getWidth));

        subscriptions.add(EasyBind.subscribe(model.getSumOfNumChars(), sumOfNumChars -> {
            if (sumOfNumChars != null) {
                updateFontsize(sumOfNumChars.intValue());
            }
        }));
    }

    void onViewDetached() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        minAmount.textProperty().unbind();
        maxAmount.textProperty().unbind();
        model.getMinAmountWidth().unbind();
        model.getDashWidth().unbind();
        model.getMaxAmountWidth().unbind();
    }

    private void updateFontsize(int length) {
        double size = AmountTextInputLayout.computeFontSize(length);
        if (Math.abs(size - lastSize) > 0.1) {
            minAmount.setStyle("-fx-font-size: " + size + "em;");
            dash.setStyle("-fx-font-size: " + size + "em;");
            maxAmount.setStyle("-fx-font-size: " + size + "em;");
            lastSize = size;
        }
    }
}
