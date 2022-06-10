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

package bisq.desktop.primary.overlay.onboarding.offer.complete;

import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class OfferCompletedView extends View<VBox, OfferCompletedModel, OfferCompletedController> {

    OfferCompletedView(OfferCompletedModel model,
                       OfferCompletedController controller,
                       Pane myOfferListView,
                       Pane takersListView) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("bisq-content-bg");

        Label headLineLabel = new Label(Res.get("onboarding.completed.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("onboarding.completed.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().addAll("bisq-text-10", "wrap-text");
       
        Label takeOfferLabel = new Label(Res.get("onboarding.completed.takeOffer"));
        takeOfferLabel.getStyleClass().add("bisq-text-headline-2");

        myOfferListView.setMaxWidth(700);
        myOfferListView.setMinHeight(150);
        myOfferListView.setMaxHeight(150);
        takersListView.setMaxWidth(700);

        VBox.setMargin(headLineLabel, new Insets(20, 0, 4, 0));
        VBox.setMargin(subtitleLabel, new Insets(0, 0, 0, 0));
        VBox.setMargin(takeOfferLabel, new Insets(10, 0, 10, 0));
        //VBox.setMargin(takersListView, new Insets(-90, 0, 20, 0));
        root.getChildren().addAll(headLineLabel, subtitleLabel, myOfferListView, takeOfferLabel, takersListView);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
