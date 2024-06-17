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
import bisq.desktop.components.controls.DropdownMenu;
import bisq.desktop.components.controls.DropdownMenuItem;
import bisq.desktop.main.content.chat.message_container.list.ChatMessageListItem;
import bisq.desktop.main.content.chat.message_container.list.ChatMessagesListController;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

public class PeerTextMessageBox extends BubbleMessageBox {
    private Subscription isMenuShowingPin;
    protected Label replyIcon, pmIcon, copyIcon;
    protected DropdownMenuItem ignoreUserMenuItem, reportUserMenuItem;

    public PeerTextMessageBox(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                              ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> list,
                              ChatMessagesListController controller) {
        super(item, list, controller);

        setUpPeerMessage();
        setMargin(userNameAndDateHBox, new Insets(-5, 0, -5, 10));
        messageHBox.getChildren().setAll(messageBgHBox, Spacer.fillHBox());
        reactionsHBox.getChildren().setAll(replyIcon, pmIcon, copyIcon, moreOptionsMenu, Spacer.fillHBox());

        contentVBox.getChildren().setAll(userNameAndDateHBox, messageHBox, reactionsHBox);
    }

    @Override
    protected void setUpUserNameAndDateTime() {
        super.setUpUserNameAndDateTime();

        userNameAndDateHBox = new HBox(10, userName, dateTime);
        userNameAndDateHBox.setAlignment(Pos.CENTER_LEFT);
    }

    @Override
    protected void setUpReactions() {
        replyIcon = getIconWithToolTip(AwesomeIcon.REPLY, Res.get("chat.message.reply"));
        pmIcon = getIconWithToolTip(AwesomeIcon.COMMENT_ALT, Res.get("chat.message.privateMessage"));
        copyIcon = getIconWithToolTip(AwesomeIcon.COPY, Res.get("action.copyToClipboard"));

        // More options dropdown menu
        ignoreUserMenuItem = new DropdownMenuItem(Res.get("chat.message.contextMenu.ignoreUser"));
        reportUserMenuItem = new DropdownMenuItem(Res.get("chat.message.contextMenu.reportUser"));
        moreOptionsMenu = new DropdownMenu("ellipsis-h-grey", "ellipsis-h-white", true);
        moreOptionsMenu.setTooltip(Res.get("chat.message.moreOptions"));
        moreOptionsMenu.addMenuItems(ignoreUserMenuItem, reportUserMenuItem);
        moreOptionsMenu.setOpenToTheRight(true);

        HBox.setMargin(replyIcon, new Insets(4, 0, -4, 10));
        HBox.setMargin(pmIcon, new Insets(3, 0, -3, 0));
        HBox.setMargin(copyIcon, new Insets(4, 0, -4, 0));
        HBox.setMargin(moreOptionsMenu, new Insets(2, 0, -2, 0));
        reactionsHBox.setVisible(false);
    }

    @Override
    protected void addReactionsHandlers() {
        ChatMessage chatMessage = item.getChatMessage();

        replyIcon.setOnMouseClicked(e -> controller.onReply(chatMessage));
        pmIcon.setOnMouseClicked(e -> controller.onOpenPrivateChannel(chatMessage));
        copyIcon.setOnMouseClicked(e -> onCopyMessage(chatMessage));
        ignoreUserMenuItem.setOnAction(e -> controller.onIgnoreUser(chatMessage));
        reportUserMenuItem.setOnAction(e -> controller.onReportUser(chatMessage));

        replyIcon.setVisible(true);
        replyIcon.setManaged(true);

        pmIcon.setVisible(chatMessage instanceof PublicChatMessage);
        pmIcon.setManaged(chatMessage instanceof PublicChatMessage);

        isMenuShowingPin = EasyBind.subscribe(moreOptionsMenu.getIsMenuShowing(), isShowing -> {
           if (!isShowing && !isHover()) {
               dateTime.setVisible(false);
               reactionsHBox.setVisible(false);
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
        replyIcon.setOnMouseClicked(null);
        pmIcon.setOnMouseClicked(null);
        copyIcon.setOnMouseClicked(null);
        ignoreUserMenuItem.setOnAction(null);
        reportUserMenuItem.setOnAction(null);

        userProfileIcon.releaseResources();

        isMenuShowingPin.unsubscribe();
    }
}
