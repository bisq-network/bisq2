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
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.main.content.bisq_easy.open_trades.message_box.PeerProtocolLogMessageBox;
import bisq.desktop.main.content.chat.message_container.list.ChatMessageListItem;
import bisq.desktop.main.content.chat.message_container.list.ChatMessagesListController;
import bisq.i18n.Res;
import javafx.scene.control.Hyperlink;

public final class PeerLeftMessageBox extends PeerProtocolLogMessageBox {
    public PeerLeftMessageBox(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                              ChatMessagesListController controller) {
        super(item);

        Hyperlink hyperlink = new Hyperlink(Res.get("chat.leave"));
        hyperlink.setGraphic(ImageUtil.getImageViewById("leave-chat-green"));
        hyperlink.getStyleClass().addAll("system-message-labels", "leave-chat-message");
        hyperlink.setOnAction(e -> controller.onLeaveChannel());
        tradeLogMessageBg.getChildren().setAll(message, hyperlink, dateTime);
    }
}
