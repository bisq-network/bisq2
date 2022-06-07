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

package bisq.desktop.primary.onboardingOld.onboardNewbie;

import bisq.desktop.common.view.View;
import bisq.desktop.common.utils.Layout;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OnboardNewbieView extends View<VBox, OnboardNewbieModel, OnboardNewbieController> {
    private final Button publishButton, skipButton;

    public OnboardNewbieView(OnboardNewbieModel model,
                             OnboardNewbieController controller,
                             Pane marketSelection,
                             Pane amountPrice,
                             PaymentMethodsSelection paymentMethods) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.setSpacing(30);
        root.getStyleClass().add("bisq-content-bg");

        Label headLineLabel = new Label(Res.get("satoshisquareapp.createOffer.headline"));
        headLineLabel.setWrapText(true);
        headLineLabel.getStyleClass().add("bisq-big-light-headline-label");
        VBox.setMargin(headLineLabel, new Insets(50, 200, 0, 200));
        VBox.setVgrow(headLineLabel, Priority.ALWAYS);

        int width = 500;
        amountPrice.setMaxWidth(width);

        double leftWidth = (width) / 2d;
        Pane paymentMethodsRoot = paymentMethods.getRoot();

        marketSelection.setMaxWidth(leftWidth);
        VBox.setMargin(marketSelection, new Insets(0, 0, 0, -leftWidth));

        paymentMethodsRoot.setMaxWidth(leftWidth);
        VBox.setMargin(paymentMethodsRoot, new Insets(0, 0, 0, -leftWidth));

        publishButton = new Button(Res.get("satoshisquareapp.createOffer.publish"));
        publishButton.setDefaultButton(true);

        skipButton = new Button(Res.get("satoshisquareapp.createOffer.skip"));

        HBox buttons = Layout.hBoxWith(skipButton, publishButton);
        buttons.setAlignment(Pos.CENTER);
        buttons.setMaxWidth(width);

        root.getChildren().addAll(headLineLabel, marketSelection, amountPrice, paymentMethodsRoot, buttons);
    }

    @Override
    public void onViewAttached() {
        publishButton.visibleProperty().bind(model.getCreateOfferButtonVisibleProperty());
        publishButton.managedProperty().bind(model.getCreateOfferButtonVisibleProperty());
        publishButton.disableProperty().bind(model.getIsInvalidTradeIntent());
        publishButton.setOnAction(e -> controller.onCreateOffer());
        skipButton.setOnAction(e -> controller.onSkip());
    }

    @Override
    public void onViewDetached() {
        publishButton.visibleProperty().unbind();
        publishButton.managedProperty().unbind();
        publishButton.disableProperty().unbind();
        publishButton.setOnAction(null);
        skipButton.setOnAction(null);
    }
}
