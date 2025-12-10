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
import bisq.desktop.main.content.chat.message_container.list.ChatMessageListItem;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ExpiredMessagesIndicatorMessageBox extends MessageBox {
    public ExpiredMessagesIndicatorMessageBox(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item) {
        String decoded = Res.decode(item.getMessage());
        Label message = new Label(decoded);
        message.getStyleClass().addAll("expired-messages-indicator", "font-light", "medium-text");
        message.setWrapText(true);

        VBox messageContentVBox = new VBox();
        messageContentVBox.getChildren().add(message);
        messageContentVBox.setFillWidth(true);
        messageContentVBox.setAlignment(Pos.CENTER);

        StackPane messageBgStackPane = new StackPane();
        messageBgStackPane.setPadding(new Insets(15, 25, 25, 25));
        messageBgStackPane.getChildren().add(messageContentVBox);
        HBox.setHgrow(messageBgStackPane, Priority.ALWAYS);

        StackPane.setAlignment(messageContentVBox, Pos.CENTER);

        setFillWidth(true);
        HBox.setHgrow(this, Priority.ALWAYS);
        setPadding(new Insets(0));

        VBox contentWidthLimiterVBox = new VBox(messageBgStackPane);
        contentWidthLimiterVBox.setPrefWidth(Region.USE_PREF_SIZE);
        contentWidthLimiterVBox.setMaxWidth(CHAT_BOX_MAX_WIDTH);
        contentWidthLimiterVBox.setPadding(new Insets(0, 70, 0, 70));
        getChildren().setAll(contentWidthLimiterVBox);
        setAlignment(Pos.CENTER);
    }

    @Override
    public void dispose() {
    }
}
