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

package bisq.desktop.main.content.chat;

import bisq.desktop.common.Layout;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ChatView<V extends ChatView<V, M>, M extends ChatModel> extends BaseChatView {
    protected static final double SIDE_PADDING = 40;

    public ChatView(ChatModel model,
                    ChatController<V, M> controller,
                    Pane chatMessagesComponent,
                    Pane channelInfo) {
        super(model, controller, chatMessagesComponent, channelInfo);
    }

    @Override
    protected void configTitleHBox() {
        titleHBox.getStyleClass().add("chat-container-header");

        HBox headerTitle = new HBox(10, channelTitle, channelDescription);
        headerTitle.setAlignment(Pos.BASELINE_LEFT);
        headerTitle.setPadding(new Insets(7, 0, 0, 0));
        HBox.setHgrow(headerTitle, Priority.ALWAYS);

        channelTitle.getStyleClass().add("chat-header-title");
        channelDescription.getStyleClass().add("chat-header-description");

        searchBox.setMaxWidth(200);
        double searchBoxHeight = 29;
        searchBox.setMinHeight(searchBoxHeight);
        searchBox.setMaxHeight(searchBoxHeight);
        searchBox.setPrefHeight(searchBoxHeight);

        HBox.setMargin(channelIcon, new Insets(0, 0, -2, 5));
        HBox.setMargin(notificationsSettingsMenu, new Insets(0, 0, 0, -5));
        titleHBox.getChildren().addAll(channelIcon, headerTitle, searchBox, ellipsisMenu, notificationsSettingsMenu);
    }

    @Override
    protected void configCenterVBox() {
        VBox.setVgrow(chatMessagesComponent, Priority.ALWAYS);
        centerVBox.getChildren().addAll(titleHBox, Layout.hLine(), chatMessagesComponent);
        centerVBox.setAlignment(Pos.CENTER);
        centerVBox.getStyleClass().add("bisq-easy-container");
        centerVBox.setFillWidth(true);
    }

    @Override
    protected void configSideBarVBox() {
        sideBar.getChildren().add(channelSidebar);
        sideBar.getStyleClass().add("bisq-easy-chat-sidebar-bg");
        sideBar.setAlignment(Pos.TOP_RIGHT);
        sideBar.setFillWidth(true);
    }

    @Override
    protected void configContainerHBox() {
        containerHBox.setSpacing(10);
        containerHBox.setFillHeight(true);
        Layout.pinToAnchorPane(containerHBox, 0, 0, 0, 0);

        AnchorPane wrapper = new AnchorPane();
        wrapper.setPadding(new Insets(0, SIDE_PADDING, 0, SIDE_PADDING));
        wrapper.getChildren().add(containerHBox);

        root.setContent(wrapper);

        HBox.setHgrow(centerVBox, Priority.ALWAYS);
        HBox.setHgrow(sideBar, Priority.NEVER);
        containerHBox.getChildren().addAll(centerVBox, sideBar);
        containerHBox.setAlignment(Pos.CENTER);
    }
}
