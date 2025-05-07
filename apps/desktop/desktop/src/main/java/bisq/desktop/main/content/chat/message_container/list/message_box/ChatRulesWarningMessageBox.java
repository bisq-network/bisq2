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
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.chat.message_container.list.ChatMessageListItem;
import bisq.desktop.main.content.chat.message_container.list.ChatMessagesListController;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ChatRulesWarningMessageBox extends MessageBox {
    private final Hyperlink learnMoreLink;
    private final BisqMenuItem closeIcon;
    private final Tooltip closeTooltip = new BisqTooltip(Res.get("action.dontShowAgain"));

    public ChatRulesWarningMessageBox(
            ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
            ChatMessagesListController controller) {

        Label warningHeadline = new Label(Res.get("chat.private.chatRulesWarningMessage.headline"), ImageUtil.getImageViewById("undelivered-message-grey"));
        warningHeadline.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        warningHeadline.setGraphicTextGap(10);
        warningHeadline.setPadding(new Insets(0, 0, 10, 0));

        String decoded = Res.decode(item.getMessage());
        Label message = new Label(decoded);
        message.getStyleClass().addAll("text-fill-grey-dimmed", "font-light", "medium-text");
        message.setAlignment(Pos.CENTER);
        message.setWrapText(true);

        learnMoreLink = new Hyperlink(Res.get("chat.private.chatRulesWarningMessage.learnMore"));
        learnMoreLink.getStyleClass().addAll("text-fill-green", "font-light", "medium-text");
        learnMoreLink.setOnAction(e -> controller.onLearnMoreAboutChatRules());

        VBox messageContentVBox = new VBox();
        messageContentVBox.setSpacing(5);
        messageContentVBox.getChildren().addAll(warningHeadline, message, learnMoreLink);
        messageContentVBox.setFillWidth(true);
        messageContentVBox.setAlignment(Pos.CENTER_LEFT);

        closeIcon = new BisqMenuItem("close-mini-grey", "close-mini-white");
        closeIcon.useIconOnly(20);
        closeIcon.setCursor(Cursor.HAND);
        Tooltip.install(closeIcon, closeTooltip);
        closeIcon.setOnMouseClicked(e -> {
            controller.onDismissChatRulesWarning();
            e.consume();
        });

        StackPane messageBgStackPane = new StackPane();
        messageBgStackPane.getStyleClass().add("system-message-background");
        HBox.setHgrow(messageBgStackPane, Priority.ALWAYS);

        messageBgStackPane.getChildren().addAll(messageContentVBox, closeIcon);

        StackPane.setAlignment(closeIcon, Pos.TOP_RIGHT);
        StackPane.setMargin(closeIcon, new Insets(0, 0, 5, 0));
        StackPane.setAlignment(messageContentVBox, Pos.CENTER_LEFT);

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
        learnMoreLink.setOnAction(null);
        closeIcon.setOnMouseClicked(null);
        Tooltip.uninstall(closeIcon, closeTooltip);
    }
}