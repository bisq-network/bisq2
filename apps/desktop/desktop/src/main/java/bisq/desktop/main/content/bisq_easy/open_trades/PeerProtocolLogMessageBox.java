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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class PeerProtocolLogMessageBox extends MessageBox {
    protected final ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item;
    protected final VBox systemMessageBg = new VBox();
    protected final VBox contentVBox;
    protected final Label message, dateTime;

    public PeerProtocolLogMessageBox(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item) {
        this.item = item;

        message = new Label(item.getMessage());
        message.getStyleClass().addAll("text-fill-white", "system-message-labels");
        message.setAlignment(Pos.CENTER);
        message.setWrapText(true);

        dateTime = new Label(item.getDate());
        dateTime.getStyleClass().addAll("text-fill-grey-dimmed", "system-message-labels");

        systemMessageBg.setSpacing(5);
        systemMessageBg.getChildren().addAll(message, dateTime);
        systemMessageBg.setFillWidth(true);
        systemMessageBg.setAlignment(Pos.CENTER);
        systemMessageBg.getStyleClass().add("system-message-background");
        HBox.setHgrow(systemMessageBg, Priority.ALWAYS);

        setFillWidth(true);
        HBox.setHgrow(this, Priority.ALWAYS);
        setPadding(new Insets(0));

        contentVBox = new VBox(systemMessageBg);
        contentVBox.setMaxWidth(CHAT_BOX_MAX_WIDTH);
        getChildren().setAll(contentVBox);
        setAlignment(Pos.CENTER);
    }

    @Override
    public void cleanup() {
    }
}
