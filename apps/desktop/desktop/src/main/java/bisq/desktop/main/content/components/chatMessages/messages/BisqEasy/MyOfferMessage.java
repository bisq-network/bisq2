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
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.main.content.components.chatMessages.ChatMessageListItem;
import bisq.desktop.main.content.components.chatMessages.ChatMessagesListView;
import bisq.desktop.main.content.components.chatMessages.messages.BubbleMessage;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class MyOfferMessage extends BubbleMessage {
    private final Button removeOfferButton;
    private Label copyIcon;

    public MyOfferMessage(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                          ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> list,
                          ChatMessagesListView.Controller controller, ChatMessagesListView.Model model) {
        super(item, list, controller, model);

        removeOfferButton = createAndGetRemoveOfferButton();
        message.setAlignment(Pos.CENTER_RIGHT);
        messageBgHBox.getStyleClass().add("chat-message-bg-my-message");

        VBox messageVBox = new VBox(message);

        message.maxWidthProperty().bind(list.widthProperty().subtract(160));
        userProfileIcon.setSize(60);
        userProfileIconVbox.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(userProfileIconVbox, new Insets(-5, 0, -5, 0));
        HBox.setMargin(messageVBox, new Insets(0, -10, 0, 0));

        removeOfferButton.setOnAction(e -> controller.onDeleteMessage(item.getChatMessage()));
        reactionsHBox.getChildren().setAll(Spacer.fillHBox(), supportedLanguages, copyIcon);
        reactionsHBox.setAlignment(Pos.CENTER_RIGHT);

        HBox.setMargin(userProfileIconVbox, new Insets(0, 0, 10, 0));
        HBox hBox = new HBox(15, messageVBox, userProfileIconVbox);
        HBox removeOfferButtonHBox = new HBox(Spacer.fillHBox(), removeOfferButton);
        VBox vBox = new VBox(hBox, removeOfferButtonHBox);
        messageBgHBox.getChildren().setAll(vBox);

        messageHBox.getChildren().setAll(Spacer.fillHBox(), messageBgHBox);
        getChildren().setAll(userNameAndDateHBox, messageHBox, reactionsHBox);
    }

    @Override
    protected void setUpUserNameAndDateTime() {
        super.setUpUserNameAndDateTime();

        userNameAndDateHBox = new HBox(10, dateTime, userName);
        userNameAndDateHBox.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(userNameAndDateHBox, new Insets(-5, 10, -5, 0));
    }

    @Override
    protected void setUpReactions() {
        copyIcon = getIconWithToolTip(AwesomeIcon.COPY, Res.get("action.copyToClipboard"));
        reactionsHBox.setVisible(false);
    }

    @Override
    protected void addReactionsHandlers() {
        copyIcon.setOnMouseClicked(e -> onCopyMessage(item.getChatMessage()));
    }

    private Button createAndGetRemoveOfferButton() {
        Button button = new Button(Res.get("offer.deleteOffer"));
        button.getStyleClass().addAll("red-small-button", "no-background");
        button.setVisible(item.isPublicChannel());
        button.setManaged(item.isPublicChannel());
        return button;
    }

    @Override
    public void cleanup() {
        removeOfferButton.setOnAction(null);
    }
}
