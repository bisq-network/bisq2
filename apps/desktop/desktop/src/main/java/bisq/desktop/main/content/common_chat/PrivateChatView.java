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
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.main.content.chat.ChatView;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrivateChatView extends ChatView {
    private VBox left;
    private final Region twoPartyPrivateChatChannelSelection;
    private final PrivateChatModel privateChatModel;

    public PrivateChatView(PrivateChatModel model,
                           PrivateChatController controller,
                           Region twoPartyPrivateChatChannelSelection,
                           Pane chatMessagesComponent,
                           Pane channelInfo) {
        super(model, controller, chatMessagesComponent, channelInfo);

        privateChatModel = model;
        this.twoPartyPrivateChatChannelSelection = twoPartyPrivateChatChannelSelection;
        left.getChildren().addAll(twoPartyPrivateChatChannelSelection, Spacer.fillVBox());
        left.setPrefWidth(210);
        left.setMinWidth(210);
        left.setFillWidth(true);
        left.getStyleClass().add("bisq-grey-2-bg");
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
        root.setContent(containerHBox);

        left = new VBox();
        HBox.setHgrow(left, Priority.NEVER);
        HBox.setHgrow(centerVBox, Priority.ALWAYS);
        HBox.setHgrow(sideBar, Priority.NEVER);
        containerHBox.getChildren().addAll(left, centerVBox, sideBar);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
        twoPartyPrivateChatChannelSelection.visibleProperty().bind(privateChatModel.getIsTwoPartyPrivateChatChannelSelectionVisible());
        twoPartyPrivateChatChannelSelection.managedProperty().bind(privateChatModel.getIsTwoPartyPrivateChatChannelSelectionVisible());
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        twoPartyPrivateChatChannelSelection.visibleProperty().unbind();
        twoPartyPrivateChatChannelSelection.managedProperty().unbind();
    }
}
