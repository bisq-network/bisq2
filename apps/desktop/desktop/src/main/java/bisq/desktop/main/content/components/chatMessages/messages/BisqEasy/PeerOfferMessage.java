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
        // User profile icon
        userProfileIcon.setSize(80);
        userProfileIconVbox.setAlignment(Pos.CENTER_LEFT);

        // Reputation
        Label reputationLabel = new Label(Res.get("chat.message.reputation").toUpperCase());
        ReputationScoreDisplay reputationScoreDisplay = new ReputationScoreDisplay();
        reputationScoreDisplay.setReputationScore(item.getReputationScore());
        VBox reputationVBox = new VBox(4, reputationLabel, reputationScoreDisplay);
        reputationVBox.setAlignment(Pos.CENTER);
        reputationVBox.getStyleClass().add("reputation");

        // Take offer button
        takeOfferButton = new Button(Res.get("offer.takeOffer"));
        BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) item.getChatMessage();
        takeOfferButton.setOnAction(e -> controller.onTakeOffer(bisqEasyOfferbookMessage));
        takeOfferButton.setDefaultButton(!item.isOfferAlreadyTaken());
        takeOfferButton.getStyleClass().add("take-offer-button");

        HBox hBox = new HBox(15, userProfileIconVbox, reputationVBox, message);
        hBox.setAlignment(Pos.CENTER);
        message.getStyleClass().add("chat-peer-offer-message");

        VBox vBox = new VBox(5, hBox, takeOfferButton);
        vBox.setAlignment(Pos.CENTER_RIGHT);

        // Message background
        messageBgHBox.getStyleClass().add("chat-peer-offer-message-bg");
        messageBgHBox.getChildren().setAll(vBox);
        messageBgHBox.setAlignment(Pos.CENTER_LEFT);
        messageBgHBox.setMaxWidth(Control.USE_PREF_SIZE);
    }

    @Override
    public void cleanup() {
        super.cleanup();

        takeOfferButton.setOnAction(null);
    }
}
