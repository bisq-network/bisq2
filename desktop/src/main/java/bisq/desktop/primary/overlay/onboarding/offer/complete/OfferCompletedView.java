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

import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
class OfferCompletedView extends View<StackPane, OfferCompletedModel, OfferCompletedController> {
    private final Label headLineLabel, subtitleLabel, takeOfferLabel;
    private final Pane takersListView;
    private Subscription matchingOffersFoundPin;
    private final VBox content, feedback;
    private Button viewOfferButton;
    private Subscription showFeedbackPin;

    OfferCompletedView(OfferCompletedModel model,
                       OfferCompletedController controller,
                       Pane myOfferListView,
                       Pane takersListView) {
        super(new StackPane(), model, controller);
        this.takersListView = takersListView;

        content = new VBox();
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("bisq-content-bg");

        headLineLabel = new Label(Res.get("onboarding.completed.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        subtitleLabel = new Label(Res.get("onboarding.completed.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");

        takeOfferLabel = new Label(Res.get("onboarding.completed.takeOffer"));
        takeOfferLabel.getStyleClass().add("bisq-text-headline-2");

        myOfferListView.setMaxWidth(700);
        myOfferListView.setMinHeight(170);
        myOfferListView.setMaxHeight(170);
        takersListView.setMaxWidth(700);
        takersListView.setMinHeight(170);

        content.getChildren().addAll(headLineLabel, subtitleLabel, myOfferListView, takeOfferLabel, takersListView);

        feedback = new VBox();
        setupFeedback();

        StackPane.setMargin(feedback, new Insets(-55, 0, 380, 0));
        root.getChildren().addAll(content, feedback);
    }

    @Override
    protected void onViewAttached() {
        Transitions.removeEffect(content);
        viewOfferButton.setOnAction(e -> controller.onOpenBisqEasy());
        matchingOffersFoundPin = EasyBind.subscribe(model.getMatchingOffersFound(), matchingOffersFound -> {
            takeOfferLabel.setVisible(matchingOffersFound);
            takeOfferLabel.setManaged(matchingOffersFound);
            takersListView.setVisible(matchingOffersFound);
            takersListView.setManaged(matchingOffersFound);
            if (matchingOffersFound) {
                VBox.setMargin(headLineLabel, new Insets(20, 0, 4, 0));
                VBox.setMargin(subtitleLabel, new Insets(0, 0, 0, 0));
                VBox.setMargin(takeOfferLabel, new Insets(10, 0, 10, 0));
            } else {
                VBox.setMargin(headLineLabel, new Insets(60, 0, 4, 0));
                VBox.setMargin(subtitleLabel, new Insets(0, 0, 0, 0));
            }
        });

        showFeedbackPin = EasyBind.subscribe(model.getShowFeedback(),
                showFeedback -> {
                    feedback.setVisible(showFeedback);
                    if (showFeedback) {
                        Transitions.blurLight(content, -0.5);
                        Transitions.slideInTop(feedback, 450);
                    } else {
                        Transitions.removeEffect(content);
                    }
                });
    }

    @Override
    protected void onViewDetached() {
        viewOfferButton.setOnAction(null);
        matchingOffersFoundPin.unsubscribe();
        showFeedbackPin.unsubscribe();
    }

    private void setupFeedback() {
        double width = 700;
        feedback.setAlignment(Pos.TOP_CENTER);
        feedback.setMaxWidth(width);
        feedback.setId("sellBtcWarning");

        Label headLineLabel = new Label(Res.get("onboarding.completed.feedback.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("onboarding.completed.feedback.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setMinWidth(width - 200);
        subtitleLabel.setMaxWidth(subtitleLabel.getMinWidth());
        subtitleLabel.setMinHeight(100);
        subtitleLabel.getStyleClass().addAll("bisq-text-13", "wrap-text");

        viewOfferButton = new Button(Res.get("onboarding.completed.feedback.viewOffer"));
        viewOfferButton.setDefaultButton(true);
        
        VBox.setMargin(headLineLabel, new Insets(40, 0, 30, 0));
        VBox.setMargin(viewOfferButton, new Insets(50, 0, 30, 0));
        feedback.getChildren().addAll(headLineLabel, subtitleLabel, viewOfferButton);
    }
}
