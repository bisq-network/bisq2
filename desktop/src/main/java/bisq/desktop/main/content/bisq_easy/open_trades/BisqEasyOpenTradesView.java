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

import bisq.chat.bisqeasy.channel.open_trades.BisqEasyOpenTradeChatChannel;
import bisq.desktop.common.Layout;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.main.content.chat.ChatView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqEasyOpenTradesView extends ChatView {
    private final BisqEasyOpenTradesModel bisqEasyOpenTradesModel;

    public BisqEasyOpenTradesView(BisqEasyOpenTradesModel model,
                                  BisqEasyOpenTradesController controller,
                                  VBox chatMessagesComponent,
                                  Pane channelSidebar,
                                  VBox tradeStateViewRoot) {
        super(model,
                controller,
                chatMessagesComponent,
                channelSidebar);

        bisqEasyOpenTradesModel = model;

        root.setPadding(new Insets(0, 0, -67, 0));
    }

    protected void configTitleHBox() {
        titleHBox.setAlignment(Pos.CENTER);
        titleHBox.setPadding(new Insets(12.5, 25, 12.5, 25));
        titleHBox.getStyleClass().add("bisq-easy-chat-title-bg");
        titleHBox.setCursor(Cursor.HAND);

        channelTitle.setId("chat-messages-headline");

        double scale = 1.15;
        helpButton = BisqIconButton.createIconButton("icon-help");
        helpButton.setScaleX(scale);
        helpButton.setScaleY(scale);
        infoButton = BisqIconButton.createIconButton("icon-info");
        infoButton.setScaleX(scale);
        infoButton.setScaleY(scale);

        HBox.setMargin(channelTitle, new Insets(0, 0, 0, 4));
        HBox.setMargin(helpButton, new Insets(-2, 0, 0, 0));
        HBox.setMargin(infoButton, new Insets(-2, 0, 0, 0));
        titleHBox.getChildren().addAll(
                channelTitle,
                Spacer.fillHBox(),
                helpButton, infoButton
        );
    }

    protected void configCenterVBox() {
        centerVBox.setSpacing(0);
        centerVBox.setFillWidth(true);

        VBox topPanelVBox = new VBox(titleHBox); //todo
        topPanelVBox.getStyleClass().add("bisq-easy-chat-tools-bg");

        chatMessagesComponent.setMinWidth(700);
        chatMessagesComponent.getStyleClass().add("bisq-easy-chat-messages-bg");

        VBox.setVgrow(chatMessagesComponent, Priority.ALWAYS);
        centerVBox.getChildren().addAll(titleHBox, Layout.hLine(), chatMessagesComponent);
    }

    protected void configSideBarVBox() {
        sideBar.getChildren().add(channelSidebar);
        sideBar.getStyleClass().add("bisq-easy-chat-sidebar-bg");
        sideBar.setAlignment(Pos.TOP_RIGHT);
        sideBar.setFillWidth(true);
    }

    protected void configContainerHBox() {
        containerHBox.setSpacing(10);
        containerHBox.setFillHeight(true);
        HBox.setHgrow(centerVBox, Priority.ALWAYS);
        HBox.setHgrow(sideBar, Priority.NEVER);
        containerHBox.getChildren().addAll(centerVBox, sideBar);

        Layout.pinToAnchorPane(containerHBox, 30, 0, 0, 0);
        root.getChildren().add(containerHBox);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
    }


    @Getter
    @EqualsAndHashCode
    static class ChannelItem {
        private final BisqEasyOpenTradeChatChannel channel;

        public ChannelItem(BisqEasyOpenTradeChatChannel channel) {
            this.channel = channel;
        }
    }
}
