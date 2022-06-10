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

import bisq.desktop.common.utils.Layout;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.primary.main.content.components.ChatUserIcon;
import bisq.desktop.primary.main.content.components.ReputationScoreDisplay;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class OfferCompletedView extends View<VBox, OfferCompletedModel, OfferCompletedController> {
    private final Label userNameLabel;
    private final Label dateTime;
    private final Label messageLabel;
    private final Button actionButton;
    private final HBox messageHBox;
    private final ReputationScoreDisplay reputationScoreDisplay;
    private final ChatUserIcon chatUserIcon;

    OfferCompletedView(OfferCompletedModel model, OfferCompletedController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("bisq-content-bg");

        Label headLineLabel = new Label(Res.get("onboarding.completed.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("onboarding.completed.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().addAll("bisq-text-10", "wrap-text");

        VBox.setMargin(headLineLabel, new Insets(38, 0, 4, 0));
        VBox.setMargin(subtitleLabel, new Insets(0, 0, 60, 0));

        chatUserIcon = new ChatUserIcon(42);

        messageLabel = new Label();
        messageLabel.setId("chat-messages-message");
        messageLabel.setWrapText(true);

        Label reputationLabel = new Label(Res.get("reputation").toUpperCase());
        reputationLabel.getStyleClass().add("bisq-text-7");
        reputationScoreDisplay = new ReputationScoreDisplay();
        VBox reputationVBox = new VBox(4, reputationLabel, reputationScoreDisplay);
        reputationVBox.setAlignment(Pos.CENTER_LEFT);

        actionButton = new Button();
       
        HBox.setMargin(actionButton, new Insets(0, 10, 0, 0));
        messageHBox = Layout.hBoxWith(messageLabel, Spacer.fillHBox(), reputationVBox, actionButton);
        messageHBox.setPadding(new Insets(15));
        messageHBox.setAlignment(Pos.CENTER_LEFT);

        userNameLabel = new Label();
        dateTime = new Label();
        HBox userInfoHBox = new HBox(5, userNameLabel, dateTime);

        VBox messageAndUserInfoVoBox = new VBox(0, userInfoHBox, messageHBox);
        HBox.setHgrow(messageAndUserInfoVoBox, Priority.ALWAYS);
        HBox tradeMessageHBox = Layout.hBoxWith(chatUserIcon, messageAndUserInfoVoBox);

        root.getChildren().addAll(headLineLabel, subtitleLabel, tradeMessageHBox);
    }

    @Override
    protected void onViewAttached() {
        root.setOnMouseReleased(e -> {
            e.consume();
            if (e.getY() < 280) {
                controller.onPublishOffer();
            } else {
                controller.onTakeOffer();
            }
        });
    }

    @Override
    protected void onViewDetached() {
    }
}
