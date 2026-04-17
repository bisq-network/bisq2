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

package bisq.desktop.main.content.mu_sig.offer.create_offer.amount_and_price.amount.components.amounts.input.fix;

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
public class FixAmountLayoutHelper extends HBox {
    private final MuSigFixAmountModel model;
    private final Text amount;
    private double lastSize;
    private final Set<Subscription> subscriptions = new HashSet<>();

    public FixAmountLayoutHelper(MuSigFixAmountModel model) {
        super(5);
        this.model = model;

        setMouseTransparent(true);
        setMinWidth(WIDTH);
        setMaxWidth(WIDTH);

        amount = new Text();
        amount.getStyleClass().add("amount-input-helper");

        Region leftPadding = Spacer.width(PADDING);
        Region rightPadding = Spacer.width(PADDING);
        getChildren().addAll(leftPadding, amount, rightPadding);
    }

    void onViewAttached() {
        amount.textProperty().bind(model.getAmountInputText());
        model.getAmountInputFieldWidth().bind(amount.layoutBoundsProperty().map(Bounds::getWidth));

        subscriptions.add(EasyBind.subscribe(model.getSumOfNumChars(), sumOfNumChars -> {
            if (sumOfNumChars != null) {
                updateFontsize(sumOfNumChars.intValue());
            }
        }));
    }

    void onViewDetached() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        amount.textProperty().unbind();
        model.getAmountInputFieldWidth().unbind();
    }

    private void updateFontsize(int length) {
        double size = AmountTextInputLayout.computeFontSize(length);
        if (Math.abs(size - lastSize) > 0.1) {
            amount.setStyle("-fx-font-size: " + size + "em;");
            lastSize = size;
        }
    }
}
