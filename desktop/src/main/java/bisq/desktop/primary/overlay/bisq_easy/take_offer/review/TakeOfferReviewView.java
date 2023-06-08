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

package bisq.desktop.primary.overlay.bisq_easy.take_offer.review;

import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.primary.overlay.bisq_easy.take_offer.TakeOfferView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
class TakeOfferReviewView extends View<StackPane, TakeOfferReviewModel, TakeOfferReviewController> {
    private final static int BUTTON_WIDTH = 140;
    private final static int FEEDBACK_WIDTH = 700;

    private final Label headline;
    private final Button createOfferButton;
    private final Label subtitleLabel;
    private final VBox content, createOfferSuccess, takeOfferSuccess;
    private final Button viewOfferButton;
    private final Button openPrivateChannelButton;
    private final MaterialTextField amount, paymentMethods;
    private final Text offerTitle;
    private final VBox offerInfoHBox;
    private Subscription showCreateOfferSuccessPin, showTakeOfferSuccessPin;
    private Subscription widthPin;

    TakeOfferReviewView(TakeOfferReviewModel model, TakeOfferReviewController controller) {
        super(new StackPane(), model, controller);


        content = new VBox(10);
        content.setAlignment(Pos.TOP_CENTER);

        headline = new Label();
        headline.getStyleClass().add("bisq-text-headline-2");

        subtitleLabel = new Label();
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");

        createOfferButton = new Button(Res.get("createOffer"));
        createOfferButton.setDefaultButton(true);
        createOfferButton.setMinWidth(BUTTON_WIDTH);
        createOfferButton.setMaxWidth(BUTTON_WIDTH);

        amount = getField(Res.get("tradeAssistant.offer.amount"));
        paymentMethods = getField(Res.get("tradeAssistant.offer.paymentMethods"));

        offerTitle = new Text();
        offerTitle.getStyleClass().addAll("bisq-text-9");

        VBox.setMargin(offerTitle, new Insets(0, 0, 5, 0));
        offerInfoHBox = new VBox(10, offerTitle, amount, paymentMethods);
        offerInfoHBox.getStyleClass().add("trade-chat-offer-info-bg");
        offerInfoHBox.setAlignment(Pos.CENTER_LEFT);
        offerInfoHBox.setPadding(new Insets(20));
        VBox.setMargin(offerInfoHBox, new Insets(0, 0, 10, 0));
        content.getChildren().addAll(Spacer.fillVBox(), headline, /*subtitleLabel,*/  offerInfoHBox, /*createOfferButton, */Spacer.fillVBox());

        viewOfferButton = new Button(Res.get("onboarding.completed.createOfferSuccess.viewOffer"));
        createOfferSuccess = new VBox(20);
        configCreateOfferSuccess();

        openPrivateChannelButton = new Button(Res.get("onboarding.completed.takeOfferSuccess.openPrivateChannel"));
        takeOfferSuccess = new VBox(20);
        configTakeOfferSuccess();

        StackPane.setMargin(createOfferSuccess, new Insets(-TakeOfferView.TOP_PANE_HEIGHT, 0, 0, 0));
        StackPane.setMargin(takeOfferSuccess, new Insets(-TakeOfferView.TOP_PANE_HEIGHT, 0, 0, 0));
        root.getChildren().addAll(content, createOfferSuccess, takeOfferSuccess);
    }

    @Override
    protected void onViewAttached() {
        headline.textProperty().bind(model.getHeadline());
        offerTitle.textProperty().bind(model.getOfferTitle());
        amount.textProperty().bind(model.getAmount());
        paymentMethods.textProperty().bind(model.getPaymentMethods());
        widthPin = EasyBind.subscribe(offerInfoHBox.widthProperty(), w -> {
            if (w.doubleValue() > 0) {
                amount.setPrefWidth(w.doubleValue() - 30);
                paymentMethods.setPrefWidth(w.doubleValue() - 30);
            }
        });

        Transitions.removeEffect(content);

        viewOfferButton.setOnAction(e -> controller.onOpenBisqEasy());
        openPrivateChannelButton.setOnAction(e -> controller.onOpenPrivateChat());
        createOfferButton.setOnAction(e -> controller.onCreateOffer());

        subtitleLabel.setText(Res.get("onboarding.completed.createOfferMode"));

        showCreateOfferSuccessPin = EasyBind.subscribe(model.getShowCreateOfferSuccess(),
                show -> {
                    createOfferSuccess.setVisible(show);
                    if (show) {
                        Transitions.blurStrong(content, 0);
                        Transitions.slideInTop(createOfferSuccess, 450);
                    } else {
                        Transitions.removeEffect(content);
                    }
                });
        showTakeOfferSuccessPin = EasyBind.subscribe(model.getShowTakeOfferSuccess(),
                show -> {
                    takeOfferSuccess.setVisible(show);
                    if (show) {
                        Transitions.blurStrong(content, 0);
                        Transitions.slideInTop(takeOfferSuccess, 450);
                    } else {
                        Transitions.removeEffect(content);
                    }
                });
    }

    @Override
    protected void onViewDetached() {
        headline.textProperty().unbind();
        offerTitle.textProperty().unbind();
        amount.textProperty().unbind();
        paymentMethods.textProperty().unbind();
        widthPin.unsubscribe();

        viewOfferButton.setOnAction(null);
        openPrivateChannelButton.setOnAction(null);
        showCreateOfferSuccessPin.unsubscribe();
        showTakeOfferSuccessPin.unsubscribe();
    }

    private MaterialTextField getField(String description) {
        MaterialTextField field = new MaterialTextField(description, null);
        field.setEditable(false);
        return field;
    }

    private void configCreateOfferSuccess() {
        VBox contentBox = getFeedbackContentBox();

        createOfferSuccess.setVisible(false);
        createOfferSuccess.setAlignment(Pos.TOP_CENTER);

        Label headLineLabel = new Label(Res.get("onboarding.completed.createOfferSuccess.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("onboarding.completed.createOfferSuccess.subTitle"));
        configFeedbackSubtitleLabel(subtitleLabel);

        viewOfferButton.setDefaultButton(true);
        VBox.setMargin(viewOfferButton, new Insets(10, 0, 0, 0));
        contentBox.getChildren().addAll(headLineLabel, subtitleLabel, viewOfferButton);
        createOfferSuccess.getChildren().addAll(contentBox, Spacer.fillVBox());
    }

    private void configTakeOfferSuccess() {
        VBox contentBox = getFeedbackContentBox();

        takeOfferSuccess.setVisible(false);
        takeOfferSuccess.setAlignment(Pos.TOP_CENTER);

        Label headLineLabel = new Label(Res.get("onboarding.completed.takeOfferSuccess.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("onboarding.completed.takeOfferSuccess.subTitle"));
        configFeedbackSubtitleLabel(subtitleLabel);

        openPrivateChannelButton.setDefaultButton(true);
        VBox.setMargin(openPrivateChannelButton, new Insets(10, 0, 0, 0));
        contentBox.getChildren().addAll(headLineLabel, subtitleLabel, openPrivateChannelButton);
        takeOfferSuccess.getChildren().addAll(contentBox, Spacer.fillVBox());
    }

    private VBox getFeedbackContentBox() {
        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.getStyleClass().setAll("create-offer-feedback-bg");
        contentBox.setPadding(new Insets(30));
        contentBox.setMaxWidth(FEEDBACK_WIDTH);
        return contentBox;
    }

    private void configFeedbackSubtitleLabel(Label subtitleLabel) {
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setMinWidth(FEEDBACK_WIDTH - 200);
        subtitleLabel.setMaxWidth(subtitleLabel.getMinWidth());
        subtitleLabel.setMinHeight(100);
        subtitleLabel.getStyleClass().addAll("bisq-text-21", "wrap-text");
    }
}
