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
public class ChatView<V extends ChatView<V, M>, M extends ChatModel> extends BaseChatView {
    protected static final double SIDE_PADDING = 40;
    protected static final double CHAT_BOX_MAX_WIDTH = 1440;

    public ChatView(ChatModel model,
                    ChatController<V, M> controller,
                    Pane chatMessagesComponent,
                    Pane channelInfo) {
        super(model, controller, chatMessagesComponent, channelInfo);
    }

    @Override
    protected void configTitleHBox() {
    }

    @Override
    protected void configCenterVBox() {
        VBox.setVgrow(chatMessagesComponent, Priority.ALWAYS);
        centerVBox.getStyleClass().add("bisq-easy-container");
        centerVBox.setMaxWidth(CHAT_BOX_MAX_WIDTH);
        centerVBox.getChildren().addAll(titleHBox, Layout.hLine(), chatMessagesComponent);
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
