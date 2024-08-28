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

package bisq.desktop.main.content.bisq_easy.open_trades;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.desktop.main.content.chat.message_container.list.ChatMessageListItem;
import bisq.desktop.main.content.chat.message_container.list.message_box.MessageBox;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class PeerProtocolLogMessageBox extends MessageBox {
    protected final ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item;
    protected final VBox tradeLogMessageBg = new VBox();
    protected final VBox contentVBox;
    protected final HBox dateTimeHBox;
    protected final Label message, dateTime;

    public PeerProtocolLogMessageBox(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item) {
        this.item = item;

        String decoded = Res.decode(item.getMessage());
        message = new Label(decoded);
        message.getStyleClass().addAll("text-fill-white", "system-message-labels");
        message.setAlignment(Pos.CENTER);
        message.setWrapText(true);

        dateTime = new Label(item.getDate());
        dateTime.getStyleClass().addAll("text-fill-grey-dimmed", "system-message-labels");
        dateTimeHBox = new HBox(10, dateTime);
        dateTimeHBox.setAlignment(Pos.CENTER);

        tradeLogMessageBg.setSpacing(5);
        tradeLogMessageBg.getChildren().addAll(message, dateTimeHBox);
        tradeLogMessageBg.setFillWidth(true);
        tradeLogMessageBg.setAlignment(Pos.CENTER);
        tradeLogMessageBg.getStyleClass().add("system-message-background");
        HBox.setHgrow(tradeLogMessageBg, Priority.ALWAYS);

        setFillWidth(true);
        HBox.setHgrow(this, Priority.ALWAYS);
        setPadding(new Insets(0));

        contentVBox = new VBox(tradeLogMessageBg);
        contentVBox.setMaxWidth(CHAT_BOX_MAX_WIDTH);
        getChildren().setAll(contentVBox);
        setAlignment(Pos.CENTER);
    }

    @Override
    public void dispose() {
    }
}
