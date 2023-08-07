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

package bisq.desktop.overlay.bisq_easy.take_offer.amount;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TakeOfferAmountView extends View<VBox, TakeOfferAmountModel, TakeOfferAmountController> {

    private final Label headlineLabel;

    public TakeOfferAmountView(TakeOfferAmountModel model, TakeOfferAmountController controller, VBox amountComponentRoot) {
        super(new VBox(10), model, controller);

        root.setAlignment(Pos.TOP_CENTER);

        headlineLabel = new Label();
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        VBox.setMargin(headlineLabel, new Insets(-30, 0, 0, 0));
        root.getChildren().addAll(Spacer.fillVBox(), headlineLabel, amountComponentRoot, Spacer.fillVBox());
    }

    @Override
    protected void onViewAttached() {
        headlineLabel.setText(model.getHeadline());
    }

    @Override
    protected void onViewDetached() {
    }
}
