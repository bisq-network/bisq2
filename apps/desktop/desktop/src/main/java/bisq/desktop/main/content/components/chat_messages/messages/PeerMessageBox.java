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

package bisq.desktop.main.content.components.chat_messages.messages;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.chat.pub.PublicChatMessage;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqPopup;
import bisq.desktop.components.controls.BisqPopupMenu;
import bisq.desktop.components.controls.BisqPopupMenuItem;
import bisq.desktop.main.content.components.chat_messages.ChatMessageListItem;
import bisq.desktop.main.content.components.chat_messages.list_view.ChatMessagesListView;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class PeerMessageBox extends BubbleMessageBox {
    protected Label replyIcon, pmIcon, moreOptionsIcon;

    public PeerMessageBox(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                          ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> list,
                          ChatMessagesListView.Controller controller, ChatMessagesListView.Model model) {
        super(item, list, controller, model);

        setUpPeerMessage();
        VBox.setMargin(userNameAndDateHBox, new Insets(-5, 0, -5, 10));
        messageHBox.getChildren().setAll(messageBgHBox, Spacer.fillHBox());
        reactionsHBox.getChildren().setAll(replyIcon, pmIcon, moreOptionsIcon, Spacer.fillHBox());

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
        moreOptionsIcon = getIconWithToolTip(AwesomeIcon.ELLIPSIS_HORIZONTAL, Res.get("chat.message.moreOptions"));
        HBox.setMargin(replyIcon, new Insets(4, 0, -4, 10));
        HBox.setMargin(pmIcon, new Insets(4, 0, -4, 0));
        HBox.setMargin(moreOptionsIcon, new Insets(6, 0, -6, 0));
        reactionsHBox.setVisible(false);
    }

    @Override
    protected void addReactionsHandlers() {
        ChatMessage chatMessage = item.getChatMessage();
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
    }

    protected void setUpPeerMessage() {
        // User profile icon
        userProfileIcon.setSize(30);
        userProfileIconVbox.setAlignment(Pos.TOP_LEFT);
        HBox.setMargin(userProfileIconVbox, new Insets(7.5, 0, -5, 5));

        // Message
        quotedMessageVBox.setId("chat-message-quote-box-peer-msg");
        message.setAlignment(Pos.CENTER_LEFT);
        message.maxWidthProperty().bind(list.widthProperty().subtract(140));//165
        VBox messageVBox = new VBox(quotedMessageVBox, message);
        HBox.setMargin(messageVBox, new Insets(0, 0, 0, -10));

        // Message background
        messageBgHBox.getStyleClass().add("chat-message-bg-peer-message");
        messageBgHBox.getChildren().setAll(userProfileIconVbox, messageVBox);
    }

    private void onOpenMoreOptions(Node owner, ChatMessage chatMessage, Runnable onClose) {
        if (chatMessage.equals(model.getSelectedChatMessageForMoreOptionsPopup().get())) {
            return;
        }
        model.getSelectedChatMessageForMoreOptionsPopup().set(chatMessage);

        List<BisqPopupMenuItem> items = new ArrayList<>();
        items.add(new BisqPopupMenuItem(Res.get("chat.message.contextMenu.copyMessage"),
                () -> onCopyMessage(chatMessage)));

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

    @Override
    public void cleanup() {
        message.maxWidthProperty().unbind();

        userName.setOnMouseClicked(null);
        userProfileIcon.setOnMouseClicked(null);
        replyIcon.setOnMouseClicked(null);
        pmIcon.setOnMouseClicked(null);
        moreOptionsIcon.setOnMouseClicked(null);

        userProfileIcon.releaseResources();
    }
}
