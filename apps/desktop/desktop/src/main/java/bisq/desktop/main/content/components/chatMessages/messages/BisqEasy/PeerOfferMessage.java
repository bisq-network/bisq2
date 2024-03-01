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

package bisq.desktop.main.content.components.chatMessages.messages.BisqEasy;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.main.content.components.ReputationScoreDisplay;
import bisq.desktop.main.content.components.chatMessages.ChatMessageListItem;
import bisq.desktop.main.content.components.chatMessages.ChatMessagesListView;
import bisq.desktop.main.content.components.chatMessages.messages.PeerMessage;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class PeerOfferMessage extends PeerMessage {
    private Button takeOfferButton;

    public PeerOfferMessage(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                            ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> list,
                            ChatMessagesListView.Controller controller, ChatMessagesListView.Model model) {
        super(item, list, controller, model);

        reactionsHBox.getChildren().setAll(replyIcon, pmIcon, moreOptionsIcon, supportedLanguages, Spacer.fillHBox());

        VBox.setMargin(userNameAndDateHBox, new Insets(-5, 0, 5, 10));
        contentVBox.getChildren().setAll(userNameAndDateHBox, messageBgHBox, reactionsHBox);
    }

    @Override
    protected void setUpPeerMessage() {
        super.setUpPeerMessage();

        // User profile icon
        userProfileIcon.setSize(60);
        userProfileIconVbox.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(userProfileIconVbox, new Insets(-5, 0, -5, 0));

        // Message
        message.maxWidthProperty().bind(list.widthProperty().subtract(430));
        VBox messageVBox = new VBox(quotedMessageVBox, message);
        HBox.setMargin(messageVBox, new Insets(0, 0, 0, -10));

        // Reputation
        Label reputationLabel = new Label(Res.get("chat.message.reputation").toUpperCase());
        reputationLabel.getStyleClass().add("bisq-text-7");
        ReputationScoreDisplay reputationScoreDisplay = new ReputationScoreDisplay();
        reputationScoreDisplay.setReputationScore(item.getReputationScore());
        VBox reputationVBox = new VBox(4, reputationLabel, reputationScoreDisplay);
        reputationVBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(reputationVBox, new Insets(-5, 10, 0, 0));

        // Take offer button
        takeOfferButton = new Button(Res.get("offer.takeOffer"));
        BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) item.getChatMessage();
        takeOfferButton.setOnAction(e -> controller.onTakeOffer(bisqEasyOfferbookMessage));
        takeOfferButton.setDefaultButton(!item.isOfferAlreadyTaken());
        takeOfferButton.setMinWidth(Control.USE_PREF_SIZE);
        HBox.setMargin(takeOfferButton, new Insets(0, 10, 0, 0));

        // Message background
        messageBgHBox.getChildren().setAll(userProfileIconVbox, messageVBox, Spacer.fillHBox(), reputationVBox, takeOfferButton);
    }

    @Override
    public void cleanup() {
        super.cleanup();

        takeOfferButton.setOnAction(null);
    }
}
