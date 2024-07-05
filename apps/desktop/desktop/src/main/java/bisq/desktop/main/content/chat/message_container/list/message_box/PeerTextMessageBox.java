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

package bisq.desktop.main.content.chat.message_container.list.message_box;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.chat.pub.PublicChatMessage;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.DropdownMenu;
import bisq.desktop.components.controls.DropdownMenuItem;
import bisq.desktop.main.content.chat.message_container.list.ChatMessageListItem;
import bisq.desktop.main.content.chat.message_container.list.ChatMessagesListController;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

public class PeerTextMessageBox extends BubbleMessageBox {
    private Subscription isMenuShowingPin;
    protected BisqMenuItem replyAction, openPrivateChatAction;
    protected DropdownMenuItem ignoreUserMenuItem, reportUserMenuItem;

    public PeerTextMessageBox(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                              ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> list,
                              ChatMessagesListController controller) {
        super(item, list, controller);

        setUpPeerMessage();
        setMargin(userNameAndDateHBox, new Insets(-5, 0, -5, 10));
        messageHBox.getChildren().setAll(messageBgHBox, addedReactions, Spacer.fillHBox());
        actionsHBox.getChildren().setAll(replyAction, openPrivateChatAction, copyAction, reactMenu, moreActionsMenu, Spacer.fillHBox());

        contentVBox.getChildren().setAll(userNameAndDateHBox, messageHBox, actionsHBox);
    }

    @Override
    protected void setUpUserNameAndDateTime() {
        super.setUpUserNameAndDateTime();

        userNameAndDateHBox = new HBox(10, userName, dateTimeHBox);
        userNameAndDateHBox.setAlignment(Pos.CENTER_LEFT);
    }

    @Override
    protected void setUpActions() {
        super.setUpActions();

        reactMenu.addItems(reactionMenuItems);
        replyAction = new BisqMenuItem("reply-grey", "reply-white");
        replyAction.useIconOnly();
        replyAction.setTooltip(Res.get("chat.message.reply"));
        openPrivateChatAction = new BisqMenuItem("open-p-chat-grey", "open-p-chat-white");
        openPrivateChatAction.useIconOnly();
        openPrivateChatAction.setTooltip(Res.get("chat.message.privateMessage"));

        // More actions dropdown menu
        ignoreUserMenuItem = new DropdownMenuItem("ignore-grey", "ignore-white",
                Res.get("chat.message.contextMenu.ignoreUser"));
        reportUserMenuItem = new DropdownMenuItem("report-grey", "report-white",
                Res.get("chat.message.contextMenu.reportUser"));
        moreActionsMenu = new DropdownMenu("more-actions-grey", "more-actions-white", true);
        moreActionsMenu.setTooltip(Res.get("chat.message.moreOptions"));
        moreActionsMenu.addMenuItems(ignoreUserMenuItem, reportUserMenuItem);
        moreActionsMenu.setOpenToTheRight(true);

        HBox.setMargin(replyAction, ACTION_ITEMS_MARGIN);
        HBox.setMargin(openPrivateChatAction, ACTION_ITEMS_MARGIN);
        HBox.setMargin(moreActionsMenu, ACTION_ITEMS_MARGIN);
    }

    @Override
    protected void addActionsHandlers() {
        ChatMessage chatMessage = item.getChatMessage();

        replyAction.setOnAction(e -> controller.onReply(chatMessage));
        openPrivateChatAction.setOnAction(e -> controller.onOpenPrivateChannel(chatMessage));
        copyAction.setOnAction(e -> onCopyMessage(chatMessage));
        ignoreUserMenuItem.setOnAction(e -> controller.onIgnoreUser(chatMessage));
        reportUserMenuItem.setOnAction(e -> controller.onReportUser(chatMessage));

        replyAction.setVisible(true);
        replyAction.setManaged(true);

        openPrivateChatAction.setVisible(chatMessage instanceof PublicChatMessage);
        openPrivateChatAction.setManaged(chatMessage instanceof PublicChatMessage);

        isMenuShowingPin = EasyBind.subscribe(moreActionsMenu.getIsMenuShowing(), isShowing -> {
           if (!isShowing && !isHover()) {
               dateTimeHBox.setVisible(false);
               actionsHBox.setVisible(false);
           }
        });
    }

    protected void setUpPeerMessage() {
        // User profile icon
        userProfileIcon.setSize(30);
        userProfileIconVbox.setAlignment(Pos.TOP_LEFT);
        HBox.setMargin(userProfileIconVbox, new Insets(7.5, 0, -5, 5));

        // Message
        quotedMessageVBox.setId("chat-message-quote-box-peer-msg");
        message.setAlignment(Pos.CENTER_LEFT);
        message.maxWidthProperty().bind(list.widthProperty().subtract(140));
        VBox messageVBox = new VBox(quotedMessageVBox, message);
        HBox.setMargin(messageVBox, new Insets(0, 0, 0, -10));

        // Message background
        messageBgHBox.getStyleClass().add("chat-message-bg-peer-message");
        messageBgHBox.getChildren().setAll(userProfileIconVbox, messageVBox);
    }

    @Override
    public void cleanup() {
        super.cleanup();

        message.maxWidthProperty().unbind();

        userName.setOnMouseClicked(null);
        userProfileIcon.setOnMouseClicked(null);

        replyAction.setOnAction(null);
        openPrivateChatAction.setOnAction(null);
        copyAction.setOnAction(null);
        ignoreUserMenuItem.setOnAction(null);
        reportUserMenuItem.setOnAction(null);

        userProfileIcon.releaseResources();

        isMenuShowingPin.unsubscribe();
    }
}
