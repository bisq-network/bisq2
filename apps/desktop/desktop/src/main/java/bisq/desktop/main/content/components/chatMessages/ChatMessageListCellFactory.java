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

package bisq.desktop.main.content.components.chatMessages;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.desktop.main.content.components.chatMessages.messages.*;
import bisq.desktop.main.content.components.chatMessages.messages.BisqEasy.MyOfferMessage;
import bisq.desktop.main.content.components.chatMessages.messages.BisqEasy.PeerOfferMessage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

final class ChatMessageListCellFactory
        implements Callback<ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>>,
        ListCell<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>>> {
    private final ChatMessagesListView.Controller controller;
    private final ChatMessagesListView.Model model;

    public ChatMessageListCellFactory(ChatMessagesListView.Controller controller, ChatMessagesListView.Model model) {
        this.controller = controller;
        this.model = model;
    }

    @Override
    public ListCell<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> call(
            ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> list) {
        return new ListCell<>() {
            private final static double CHAT_BOX_MAX_WIDTH = 1200;

            private final HBox cellHBox;
            private Subscription listWidthPropertyPin;
            private Message message;

            {
                cellHBox = new HBox(15);
                cellHBox.setMaxWidth(CHAT_BOX_MAX_WIDTH);
                cellHBox.setAlignment(Pos.CENTER);
                cellHBox.setPadding(new Insets(15, 0, 15, 0));
            }

            @Override
            public void updateItem(final ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                                   boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    cleanup();
                    return;
                }

                Node flow = this.getListView().lookup(".virtual-flow");
                if (flow != null && !flow.isVisible()) {
                    return;
                }

                message = createMessage(item, list);
                cellHBox.getChildren().setAll(message);

                listWidthPropertyPin = EasyBind.subscribe(message.widthProperty(), width -> {
                    if (width == null) {
                        return;
                    }
                    message.getStyleClass().clear();

                    // List cell has no padding, so it must have the same width as list view (no scrollbar)
                    if (width.doubleValue() == list.widthProperty().doubleValue()) {
                        message.getStyleClass().add("chat-message-list-cell-wo-scrollbar");
                        return;
                    }

                    if (width.doubleValue() < CHAT_BOX_MAX_WIDTH) {
                        // List cell has different size as list view, therefore there's a scrollbar
                        message.getStyleClass().add("chat-message-list-cell-w-scrollbar-full-width");
                    } else {
                        // FIXME (low prio): needs to take into account whether there's scrollbar
                        message.getStyleClass().add("chat-message-list-cell-w-scrollbar-max-width");
                    }
                });

                setGraphic(cellHBox);
                setAlignment(Pos.CENTER);
            }

            private void cleanup() {
                if (message != null) {
                    message.cleanup();
                }

                cellHBox.setOnMouseEntered(null);
                cellHBox.setOnMouseExited(null);

                if (listWidthPropertyPin != null) {
                    listWidthPropertyPin.unsubscribe();
                }

                setGraphic(null);
            }
        };
    }

    private Message createMessage(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                                  ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> list) {
        if (item.isSystemMessage()) {
            return new SystemMessage(item);
        }

        if (item.isLeaveChatMessage()) {
            return new LeaveChatMessage(item, controller);
        }

        boolean isMyMessage = model.isMyMessage(item.getChatMessage());
        if (isMyMessage) {
            return item.isBisqEasyPublicChatMessageWithOffer()
                    ? new MyOfferMessage(item, list, controller, model)
                    : new MyMessage(item, list, controller, model);
        } else {
            return item.isBisqEasyPublicChatMessageWithOffer()
                    ? new PeerOfferMessage(item, list, controller, model)
                    : new PeerMessage(item, list, controller, model);
        }
    }
}
