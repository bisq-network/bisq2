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

package bisq.desktop.main.content.chat.message_container.list;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.desktop.main.content.bisq_easy.open_trades.message_box.MyProtocolLogMessageBox;
import bisq.desktop.main.content.bisq_easy.open_trades.message_box.PeerProtocolLogMessageBox;
import bisq.desktop.main.content.chat.message_container.list.message_box.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
final class ChatMessageListCellFactory
        implements Callback<ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>>,
        ListCell<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>>> {
    private final ChatMessagesListController controller;

    public ChatMessageListCellFactory(ChatMessagesListController controller) {
        this.controller = controller;
    }

    @Override
    public ListCell<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> call(
            ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> list) {
        return new ListCell<>() {
            private final static double CHAT_BOX_MAX_WIDTH = 1200;
            private final static String STYLE_CLASS_WITHOUT_SCROLLBAR = "chat-message-list-cell-wo-scrollbar";
            private final static String STYLE_CLASS_WITH_SCROLLBAR_FULL_WIDTH = "chat-message-list-cell-w-scrollbar-full-width";
            private final static String STYLE_CLASS_WITH_SCROLLBAR_MAX_WIDTH = "chat-message-list-cell-w-scrollbar-max-width";

            private final HBox cellHBox = new HBox();
            private Subscription listWidthPropertyPin;
            private MessageBox messageBox;

            {
                cellHBox.setPadding(new Insets(15, 0, 15, 0));
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                                      boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    Node flow = getListView().lookup(".virtual-flow");
                    if (flow != null && !flow.isVisible()) {
                        cleanup();
                        return;
                    }

                    messageBox = createMessage(item, list);
                    cellHBox.getChildren().setAll(messageBox);
                    listWidthPropertyPin = EasyBind.subscribe(messageBox.widthProperty(), w -> updateMessageStyle());
                    setGraphic(cellHBox);
                } else {
                    cleanup();
                }
            }

            private void cleanup() {
                cellHBox.getChildren().clear();
                if (messageBox != null) {
                    messageBox.dispose();
                    messageBox = null;
                }

                if (listWidthPropertyPin != null) {
                    listWidthPropertyPin.unsubscribe();
                    listWidthPropertyPin = null;
                }

                setGraphic(null);
            }

            private void updateMessageStyle() {
                String messageStyleClass = getMessageStyleClass(list.getWidth(), getWidth());
                if (!messageBox.getStyleClass().contains(messageStyleClass)) {
                    messageBox.getStyleClass().removeIf(style -> style.equals(STYLE_CLASS_WITHOUT_SCROLLBAR)
                            || style.equals(STYLE_CLASS_WITH_SCROLLBAR_FULL_WIDTH)
                            || style.equals(STYLE_CLASS_WITH_SCROLLBAR_MAX_WIDTH)
                    );
                    messageBox.getStyleClass().add(messageStyleClass);
                }
            }

            private String getMessageStyleClass(double listWidth, double cellWidth) {
                // List and cell have the same width, therefore there isn't a scrollbar
                if (listWidth == cellWidth) {
                    return STYLE_CLASS_WITHOUT_SCROLLBAR;
                }

                // With scrollbar
                return cellWidth < CHAT_BOX_MAX_WIDTH
                        ? STYLE_CLASS_WITH_SCROLLBAR_FULL_WIDTH
                        : STYLE_CLASS_WITH_SCROLLBAR_MAX_WIDTH;
            }
        };
    }

    private MessageBox createMessage(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                                     ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> list) {
        if (item.isLeaveChatMessage()) {
            if (item.getChatMessage() instanceof BisqEasyOpenTradeMessage) {
                return new TradePeerLeftMessageBox(item, controller);
            } else {
                return new PeerLeftMessageBox(item, controller);
            }
        }

        if (item.isChatRulesWarningMessage()) {
            return new ChatRulesWarningMessageBox(item, controller);
        }

        if (item.isMyMessage()) {
            if (item.isProtocolLogMessage()) {
                return new MyProtocolLogMessageBox(item, controller);
            } else {
                return item.isBisqEasyPublicChatMessageWithOffer()
                        ? new MyOfferMessageBox(item, list, controller)
                        : new MyTextMessageBox(item, list, controller);
            }
        } else {
            if (item.isProtocolLogMessage()) {
                return new PeerProtocolLogMessageBox(item);
            } else {
                return item.isBisqEasyPublicChatMessageWithOffer()
                        ? new PeerOfferMessageBox(item, list, controller)
                        : new PeerTextMessageBox(item, list, controller);
            }
        }
    }
}
