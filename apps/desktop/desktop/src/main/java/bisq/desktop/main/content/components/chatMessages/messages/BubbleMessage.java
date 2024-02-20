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

package bisq.desktop.main.content.components.chatMessages.messages;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.chat.Citation;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.desktop.common.Icons;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.desktop.main.content.components.chatMessages.ChatMessageListItem;
import bisq.desktop.main.content.components.chatMessages.ChatMessagesListView;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Optional;

public abstract class BubbleMessage extends Message {
    protected final static double CHAT_MESSAGE_BOX_MAX_WIDTH = 630;

    protected final ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item;
    protected final ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> list;
    protected final ChatMessagesListView.Controller controller;
    protected final ChatMessagesListView.Model model;
    protected final UserProfileIcon userProfileIcon = new UserProfileIcon(60);
    protected final HBox reactionsHBox = new HBox(20);
    protected final VBox quotedMessageVBox, contentVBox;
    protected Label supportedLanguages, userName, dateTime, message;
    protected HBox userNameAndDateHBox, messageBgHBox, messageHBox;
    protected VBox userProfileIconVbox;

    public BubbleMessage(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                         ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> list,
                         ChatMessagesListView.Controller controller, ChatMessagesListView.Model model) {
        this.item = item;
        this.list = list;
        this.controller = controller;
        this.model = model;

        setUpUserNameAndDateTime();
        setUpUserProfileIcon();
        setUpReactions();
        addReactionsHandlers();
        addOnMouseEventHandlers();

        supportedLanguages = createAndGetSupportedLanguagesLabel();
        quotedMessageVBox = createAndGetQuotedMessageVBox();
        handleQuoteMessageBox();
        message = createAndGetMessage();
        messageBgHBox = createAndGetMessageBackground();
        messageHBox = createAndGetMessageBox();

        setFillWidth(true);
        HBox.setHgrow(this, Priority.ALWAYS);

        contentVBox = new VBox();
        contentVBox.setMaxWidth(CHAT_BOX_MAX_WIDTH);
        getChildren().setAll(contentVBox);
        setAlignment(Pos.CENTER);
    }

    protected void setUpUserNameAndDateTime() {
        userName = new Label();
        userName.getStyleClass().addAll("text-fill-white", "font-size-09", "font-default");
        dateTime = new Label();
        dateTime.getStyleClass().addAll("text-fill-grey-dimmed", "font-size-09", "font-light");
        dateTime.setVisible(false);
        dateTime.setText(item.getDate());
    }

    private void setUpUserProfileIcon() {
        userProfileIcon.setSize(60);
        userProfileIconVbox = new VBox(userProfileIcon);

        item.getSenderUserProfile().ifPresent(author -> {
            userName.setText(author.getUserName());
            userName.setOnMouseClicked(e -> controller.onMention(author));

            userProfileIcon.setUserProfile(author);
            userProfileIcon.setCursor(Cursor.HAND);
            Tooltip.install(userProfileIcon, new BisqTooltip(author.getTooltipString()));
            userProfileIcon.setOnMouseClicked(e -> controller.onShowChatUserDetails(item.getChatMessage()));
        });
    }

    protected void setUpReactions() {
    }

    protected void addReactionsHandlers() {
    }

    protected void hideReactionsBox() {
        reactionsHBox.setVisible(false);
    }

    private void addOnMouseEventHandlers() {
        setOnMouseEntered(e -> {
            if (model.getSelectedChatMessageForMoreOptionsPopup().get() != null) {
                return;
            }
            dateTime.setVisible(true);
            reactionsHBox.setVisible(true);
        });

        setOnMouseExited(e -> {
            if (model.getSelectedChatMessageForMoreOptionsPopup().get() == null) {
                hideReactionsBox();
                dateTime.setVisible(false);
                reactionsHBox.setVisible(false);
            }
        });
    }

    private Label createAndGetSupportedLanguagesLabel() {
        Label label = new Label();
        if (item.isBisqEasyPublicChatMessageWithOffer()) {
            BisqEasyOfferbookMessage chatMessage = (BisqEasyOfferbookMessage) item.getChatMessage();
            label.setText(item.getSupportedLanguageCodes(chatMessage));
            label.setTooltip(new BisqTooltip(item.getSupportedLanguageCodesForTooltip(chatMessage)));
        }
        return label;
    }

    private VBox createAndGetQuotedMessageVBox() {
        VBox vBox = new VBox(5);
        vBox.setVisible(false);
        vBox.setManaged(false);
        VBox.setMargin(vBox, new Insets(15, 0, 10, 5));
        return vBox;
    }

    private void handleQuoteMessageBox() {
        Optional<Citation> optionalCitation = item.getCitation();
        if (optionalCitation.isPresent()) {
            Citation citation = optionalCitation.get();
            if (citation.isValid()) {
                quotedMessageVBox.setVisible(true);
                quotedMessageVBox.setManaged(true);
                Label quotedMessageField = new Label();
                quotedMessageField.setWrapText(true);
                quotedMessageField.setText(citation.getText());
                quotedMessageField.setStyle("-fx-fill: -fx-mid-text-color");
                Label userName = new Label(controller.getUserName(citation.getAuthorUserProfileId()));
                userName.getStyleClass().add("font-medium");
                userName.setStyle("-fx-text-fill: -bisq-mid-grey-30");
                quotedMessageVBox.getChildren().setAll(userName, quotedMessageField);
            }
        } else {
            quotedMessageVBox.getChildren().clear();
            quotedMessageVBox.setVisible(false);
            quotedMessageVBox.setManaged(false);
        }
    }

    private Label createAndGetMessage() {
        Label label = new Label();
        label.maxWidthProperty().unbind();
        label.setWrapText(true);
        label.setPadding(new Insets(10));
        label.getStyleClass().addAll("text-fill-white", "normal-text", "font-default");
        label.setText(item.getMessage());
        return label;
    }

    private HBox createAndGetMessageBackground() {
        HBox hBox = new HBox(15);
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.setMaxWidth(CHAT_MESSAGE_BOX_MAX_WIDTH);
        HBox.setHgrow(hBox, Priority.SOMETIMES);
        if (item.hasTradeChatOffer()) {
            hBox.setPadding(new Insets(15));
        } else {
            hBox.setPadding(new Insets(5, 15, 5, 15));
        }
        return hBox;
    }

    private HBox createAndGetMessageBox() {
        HBox hBox = new HBox();
        VBox.setMargin(hBox, new Insets(10, 0, 0, 0));
        return hBox;
    }

    protected static Label getIconWithToolTip(AwesomeIcon icon, String tooltipString) {
        Label iconLabel = Icons.getIcon(icon);
        iconLabel.setCursor(Cursor.HAND);
        iconLabel.setTooltip(new BisqTooltip(tooltipString, true));
        return iconLabel;
    }

    protected static void onCopyMessage(ChatMessage chatMessage) {
        ClipboardUtil.copyToClipboard(chatMessage.getText());
    }
}
