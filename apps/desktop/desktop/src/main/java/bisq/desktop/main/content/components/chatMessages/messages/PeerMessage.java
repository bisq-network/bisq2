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
import bisq.chat.pub.PublicChatMessage;
import bisq.desktop.common.Icons;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqPopup;
import bisq.desktop.components.controls.BisqPopupMenu;
import bisq.desktop.components.controls.BisqPopupMenuItem;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.components.ReputationScoreDisplay;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.desktop.main.content.components.chatMessages.ChatMessageListItem;
import bisq.desktop.main.content.components.chatMessages.ChatMessagesListView;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PeerMessage extends Message {
    private final static double CHAT_MESSAGE_BOX_MAX_WIDTH = 630;

    private final ChatMessagesListView.Controller controller;
    private final ChatMessagesListView.Model model;
    private final UserProfileIcon userProfileIcon = new UserProfileIcon(60);
    private final VBox quotedMessageVBox;
    private final Label message, userName, dateTime, replyIcon, pmIcon, moreOptionsIcon;
    private final HBox messageHBox, messageBgHBox;
    private final HBox reactionsHBox;
    private final ReputationScoreDisplay reputationScoreDisplay;
    private final Button takeOfferButton;
    private final Label quotedMessageField = new Label();

    public PeerMessage(final ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                       ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> list,
                       ChatMessagesListView.Controller controller, ChatMessagesListView.Model model) {
        this.controller = controller;
        this.model = model;

        // userName and DateTime
        userName = new Label();
        userName.getStyleClass().addAll("text-fill-white", "font-size-09", "font-default");
        dateTime = new Label();
        dateTime.getStyleClass().addAll("text-fill-grey-dimmed", "font-size-09", "font-light");
        dateTime.setVisible(false);
        dateTime.setText(item.getDate());
        HBox userNameAndDateHBox = new HBox(10, userName, dateTime);
        userNameAndDateHBox.setAlignment(Pos.CENTER_LEFT);

        // userProfileIcon
        userProfileIcon.setSize(60);
        VBox userProfileIconVbox = new VBox(userProfileIcon);

        item.getSenderUserProfile().ifPresent(author -> {
            userName.setText(author.getUserName());
            userName.setOnMouseClicked(e -> controller.onMention(author));

            userProfileIcon.setUserProfile(author);
            userProfileIcon.setCursor(Cursor.HAND);
            Tooltip.install(userProfileIcon, new BisqTooltip(author.getTooltipString()));
            userProfileIcon.setOnMouseClicked(e -> controller.onShowChatUserDetails(item.getChatMessage()));
        });

        // reactions
        replyIcon = getIconWithToolTip(AwesomeIcon.REPLY, Res.get("chat.message.reply"));
        pmIcon = getIconWithToolTip(AwesomeIcon.COMMENT_ALT, Res.get("chat.message.privateMessage"));
        moreOptionsIcon = getIconWithToolTip(AwesomeIcon.ELLIPSIS_HORIZONTAL, Res.get("chat.message.moreOptions"));
        Label supportedLanguages = new Label();
        HBox.setMargin(replyIcon, new Insets(4, 0, -4, 10));
        HBox.setMargin(pmIcon, new Insets(4, 0, -4, 0));
        HBox.setMargin(moreOptionsIcon, new Insets(6, 0, -6, 0));
        reactionsHBox = new HBox(20);
        reactionsHBox.setVisible(false);
        handleReactionsBox(item);

        // quoted message
        quotedMessageVBox = new VBox(5);
        quotedMessageVBox.setVisible(false);
        quotedMessageVBox.setManaged(false);
        quotedMessageVBox.setId("chat-message-quote-box-peer-msg");
        VBox.setMargin(quotedMessageVBox, new Insets(15, 0, 10, 5));
        quotedMessageField.setWrapText(true);
        handleQuoteMessageBox(item);

        // HBox for message reputation vBox and action button
        message = new Label();
        message.maxWidthProperty().unbind();
        message.setWrapText(true);
        message.setPadding(new Insets(10));
        message.getStyleClass().addAll("text-fill-white", "normal-text", "font-default");
        message.setAlignment(Pos.CENTER_LEFT);
        message.setText(item.getMessage());

        // message background
        messageBgHBox = new HBox(15);
        messageBgHBox.setAlignment(Pos.CENTER_LEFT);
        messageBgHBox.setMaxWidth(CHAT_MESSAGE_BOX_MAX_WIDTH);
        messageBgHBox.getStyleClass().add("chat-message-bg-peer-message");
        HBox.setHgrow(messageBgHBox, Priority.SOMETIMES);
        if (item.hasTradeChatOffer()) {
            messageBgHBox.setPadding(new Insets(15));
        } else {
            messageBgHBox.setPadding(new Insets(5, 15, 5, 15));
        }

        // reputation
        reputationScoreDisplay = new ReputationScoreDisplay();

        // takeOfferButton
        takeOfferButton = new Button(Res.get("offer.takeOffer"));

        // messageHBox
        messageHBox = new HBox();
        VBox.setMargin(messageHBox, new Insets(10, 0, 0, 0));

        if (item.isBisqEasyPublicChatMessageWithOffer()) {
            supportedLanguages.setText(item.getSupportedLanguageCodes(((BisqEasyOfferbookMessage) item.getChatMessage())));
            supportedLanguages.setTooltip(new BisqTooltip(item.getSupportedLanguageCodesForTooltip(((BisqEasyOfferbookMessage) item.getChatMessage()))));

            reactionsHBox.getChildren().setAll(replyIcon, pmIcon, moreOptionsIcon, supportedLanguages, Spacer.fillHBox());
            message.maxWidthProperty().bind(list.widthProperty().subtract(430));
            userProfileIconVbox.setAlignment(Pos.CENTER_LEFT);

            Label reputationLabel = new Label(Res.get("chat.message.reputation").toUpperCase());
            reputationLabel.getStyleClass().add("bisq-text-7");

            reputationScoreDisplay.setReputationScore(item.getReputationScore());
            VBox reputationVBox = new VBox(4, reputationLabel, reputationScoreDisplay);
            reputationVBox.setAlignment(Pos.CENTER_LEFT);

            BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) item.getChatMessage();
            takeOfferButton.setOnAction(e -> controller.onTakeOffer(bisqEasyOfferbookMessage, item.isCanTakeOffer()));
            takeOfferButton.setDefaultButton(item.isCanTakeOffer());
            takeOfferButton.setMinWidth(Control.USE_PREF_SIZE);

            VBox messageVBox = new VBox(quotedMessageVBox, message);
            HBox.setMargin(userProfileIconVbox, new Insets(-5, 0, -5, 0));
            HBox.setMargin(messageVBox, new Insets(0, 0, 0, -10));
            HBox.setMargin(reputationVBox, new Insets(-5, 10, 0, 0));
            HBox.setMargin(takeOfferButton, new Insets(0, 10, 0, 0));
            messageBgHBox.getChildren().setAll(userProfileIconVbox, messageVBox, Spacer.fillHBox(), reputationVBox, takeOfferButton);

            VBox.setMargin(userNameAndDateHBox, new Insets(-5, 0, 5, 10));
            getChildren().setAll(userNameAndDateHBox, messageBgHBox, reactionsHBox);
        } else {
            reactionsHBox.getChildren().setAll(replyIcon, pmIcon, moreOptionsIcon, Spacer.fillHBox());
            message.maxWidthProperty().bind(list.widthProperty().subtract(140));//165
            userProfileIcon.setSize(30);
            userProfileIconVbox.setAlignment(Pos.TOP_LEFT);

            VBox messageVBox = new VBox(quotedMessageVBox, message);
            HBox.setMargin(userProfileIconVbox, new Insets(7.5, 0, -5, 5));
            HBox.setMargin(messageVBox, new Insets(0, 0, 0, -10));
            messageBgHBox.getChildren().setAll(userProfileIconVbox, messageVBox);
            messageHBox.getChildren().setAll(messageBgHBox, Spacer.fillHBox());

            VBox.setMargin(userNameAndDateHBox, new Insets(-5, 0, -5, 10));

            getChildren().setAll(userNameAndDateHBox, messageHBox, reactionsHBox);
        }
        setFillWidth(true);
        HBox.setHgrow(this, Priority.ALWAYS);
    }

    // TODO: move outside
    private static Label getIconWithToolTip(AwesomeIcon icon, String tooltipString) {
        Label iconLabel = Icons.getIcon(icon);
        iconLabel.setCursor(Cursor.HAND);
        iconLabel.setTooltip(new BisqTooltip(tooltipString, true));
        return iconLabel;
    }

    // TODO: move outside
    private void handleQuoteMessageBox(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item) {
        Optional<Citation> optionalCitation = item.getCitation();
        if (optionalCitation.isPresent()) {
            Citation citation = optionalCitation.get();
            if (citation.isValid()) {
                quotedMessageVBox.setVisible(true);
                quotedMessageVBox.setManaged(true);
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

    // TODO: move outside
    private void handleReactionsBox(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item) {
        ChatMessage chatMessage = item.getChatMessage();

        // !isMyMessage
        moreOptionsIcon.setOnMouseClicked(e -> onOpenMoreOptions(pmIcon, chatMessage, () -> {
            hideReactionsBox();
            model.getSelectedChatMessageForMoreOptionsPopup().set(null);
        }));
        replyIcon.setOnMouseClicked(e -> controller.onReply(chatMessage));
        pmIcon.setOnMouseClicked(e -> controller.onOpenPrivateChannel(chatMessage));

        replyIcon.setVisible(true);
        replyIcon.setManaged(true);

        pmIcon.setVisible(chatMessage instanceof PublicChatMessage);
        pmIcon.setManaged(chatMessage instanceof PublicChatMessage);

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

    // TODO: can be reused
    private void hideReactionsBox() {
        reactionsHBox.setVisible(false);
    }

    // TODO: reuse
    void onOpenMoreOptions(Node owner, ChatMessage chatMessage, Runnable onClose) {
        if (chatMessage.equals(model.getSelectedChatMessageForMoreOptionsPopup().get())) {
            return;
        }
        model.getSelectedChatMessageForMoreOptionsPopup().set(chatMessage);

        List<BisqPopupMenuItem> items = new ArrayList<>();
        items.add(new BisqPopupMenuItem(Res.get("chat.message.contextMenu.copyMessage"),
                () -> onCopyMessage(chatMessage)));

        // !myMessage
        if (chatMessage instanceof PublicChatMessage) {
            items.add(new BisqPopupMenuItem(Res.get("chat.message.contextMenu.ignoreUser"),
                    () -> controller.onIgnoreUser(chatMessage)));
        }
        items.add(new BisqPopupMenuItem(Res.get("chat.message.contextMenu.reportUser"),
                () -> controller.onReportUser(chatMessage)));

        BisqPopupMenu menu = new BisqPopupMenu(items, onClose);
        menu.setAlignment(BisqPopup.Alignment.LEFT);
        menu.show(owner);
    }

    void onCopyMessage(ChatMessage chatMessage) {
        ClipboardUtil.copyToClipboard(chatMessage.getText());
    }

    @Override
    public void cleanup() {
        message.maxWidthProperty().unbind();

        takeOfferButton.setOnAction(null);

        userName.setOnMouseClicked(null);
        userProfileIcon.setOnMouseClicked(null);
        replyIcon.setOnMouseClicked(null);
        pmIcon.setOnMouseClicked(null);
        moreOptionsIcon.setOnMouseClicked(null);

        userProfileIcon.releaseResources();
    }
}
