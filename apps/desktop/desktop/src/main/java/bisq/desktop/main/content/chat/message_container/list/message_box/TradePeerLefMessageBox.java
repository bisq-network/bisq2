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
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeMessage;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.main.content.bisq_easy.open_trades.PeerProtocolLogMessageBox;
import bisq.desktop.main.content.chat.message_container.list.ChatMessageListItem;
import bisq.desktop.main.content.chat.message_container.list.ChatMessagesListController;
import bisq.i18n.Res;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public final class TradePeerLefMessageBox extends PeerProtocolLogMessageBox {
    public TradePeerLefMessageBox(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                                  ChatMessagesListController controller) {
        super(item);

        BisqEasyOpenTradeMessage bisqEasyOpenTradeMessage = (BisqEasyOpenTradeMessage) item.getChatMessage();
        String peerUserName = controller.getUserName(bisqEasyOpenTradeMessage.getSenderUserProfile().getId());

        Label headline = new Label(Res.get("bisqEasy.openTrades.chat.peerLeft.headline", peerUserName));
        headline.getStyleClass().addAll("system-message-labels", "text-fill-green");

        ImageView icon = ImageUtil.getImageViewById("leave-chat-green");
        HBox hBox = new HBox(10, icon, headline);
        hBox.setAlignment(Pos.CENTER);

        Label subHeadline = new Label(Res.get("bisqEasy.openTrades.chat.peerLeft.subHeadline"));
        subHeadline.getStyleClass().addAll("text-fill-white", "system-message-labels");
        subHeadline.setAlignment(Pos.CENTER);
        subHeadline.setWrapText(true);

        tradeLogMessageBg.getChildren().setAll(hBox, subHeadline, dateTime);
    }
}
