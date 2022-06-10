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

package bisq.desktop.primary.overlay.onboarding.offer.method;

import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PaymentMethodView extends View<VBox, PaymentMethodModel, PaymentMethodController> {

    public PaymentMethodView(PaymentMethodModel model, PaymentMethodController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);

        Label headLineLabel = new Label(Res.get("onboarding.method.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("onboarding.method.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().addAll("bisq-text-10", "wrap-text");

        VBox.setMargin(headLineLabel, new Insets(38, 0, 4, 0));
        VBox.setMargin(subtitleLabel, new Insets(0, 0, 60, 0));
        root.getChildren().addAll(headLineLabel, subtitleLabel);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
