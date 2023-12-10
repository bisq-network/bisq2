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

package bisq.desktop.main.content.common_chat;

import bisq.desktop.common.Layout;
import bisq.desktop.main.content.chat.ChatView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonChatView extends ChatView {
    public static final double SIDE_PADDING = 40;

    public CommonChatView(CommonChatModel model,
                          CommonChatController controller,
                          Pane chatMessagesComponent,
                          Pane channelInfo) {
        super(model, controller, chatMessagesComponent, channelInfo);
    }

    protected void configTitleHBox() {
    }

    protected void configCenterVBox() {
        VBox.setVgrow(chatMessagesComponent, Priority.ALWAYS);
        chatMessagesComponent.setMinWidth(700);
        centerVBox.getChildren().addAll(chatMessagesComponent);
        centerVBox.setFillWidth(true);
    }

    protected void configSideBarVBox() {
        sideBar.getChildren().add(channelSidebar);
        sideBar.getStyleClass().add("bisq-grey-2-bg");
        sideBar.setAlignment(Pos.TOP_RIGHT);
        sideBar.setFillWidth(true);
    }

    protected void configContainerHBox() {
        containerHBox.setFillHeight(true);
        Layout.pinToAnchorPane(containerHBox, 0, 0, 0, 0);

        AnchorPane wrapper = new AnchorPane();
        wrapper.setPadding(new Insets(0, SIDE_PADDING, 0, SIDE_PADDING));
        wrapper.getChildren().add(containerHBox);

        root.setContent(wrapper);

        HBox.setHgrow(centerVBox, Priority.ALWAYS);
        HBox.setHgrow(sideBar, Priority.NEVER);
        containerHBox.getChildren().addAll(centerVBox, sideBar);
    }
}
