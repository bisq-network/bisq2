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
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.SearchBox;
import bisq.desktop.main.content.chat.ChatView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonChatView extends ChatView {
    private VBox left;
    private final Region twoPartyPrivateChatChannelSelection;
    private SearchBox searchBox;
    private final CommonChatModel commonChatModel;

    public CommonChatView(CommonChatModel model,
                          CommonChatController controller,
                          Region publicChannelSelection,
                          Region twoPartyPrivateChatChannelSelection,
                          Pane chatMessagesComponent,
                          Pane channelInfo) {
        super(model,
                controller,
                chatMessagesComponent,
                channelInfo);

        commonChatModel = model;
        this.twoPartyPrivateChatChannelSelection = twoPartyPrivateChatChannelSelection;

        left.getChildren().addAll(
                publicChannelSelection,
                Layout.hLine(),
                twoPartyPrivateChatChannelSelection,
                Spacer.fillVBox());
        left.setPrefWidth(210);
        left.setMinWidth(210);
        left.setFillWidth(true);
        left.getStyleClass().add("bisq-grey-2-bg");
    }


    protected void configTitleHBox() {
        channelTitle.setId("chat-messages-headline");
        HBox.setMargin(channelTitle, new Insets(0, 0, 0, 0));

        searchBox = new SearchBox();
        searchBox.setPrefWidth(200);

        helpButton = BisqIconButton.createIconButton("icon-help", model.getHelpTitle());
        infoButton = BisqIconButton.createIconButton("icon-info", Res.get("chat.topMenu.channelInfoIcon.tooltip"));

        HBox.setMargin(searchBox, new Insets(0, 0, 0, 0));
        HBox.setMargin(infoButton, new Insets(0, 0, 0, -5));
        titleHBox.getChildren().addAll(
                channelTitle,
                Spacer.fillHBox(),
                searchBox,
                helpButton,
                infoButton
        );
        titleHBox.setAlignment(Pos.CENTER);
        titleHBox.setMinHeight(58);
        titleHBox.setPadding(new Insets(0, 20, 0, 25));
    }

    protected void configCenterVBox() {
        VBox.setVgrow(chatMessagesComponent, Priority.ALWAYS);
        chatMessagesComponent.setMinWidth(700);
        centerVBox.getChildren().addAll(titleHBox, Layout.hLine(), chatMessagesComponent);
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
        root.getChildren().add(containerHBox);

        left = new VBox();
        HBox.setHgrow(left, Priority.NEVER);
        HBox.setHgrow(centerVBox, Priority.ALWAYS);
        HBox.setHgrow(sideBar, Priority.NEVER);
        containerHBox.getChildren().addAll(left, centerVBox, sideBar);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
        searchBox.textProperty().bindBidirectional(commonChatModel.getSearchText());
        twoPartyPrivateChatChannelSelection.visibleProperty().bind(commonChatModel.getIsTwoPartyPrivateChatChannelSelectionVisible());
        twoPartyPrivateChatChannelSelection.managedProperty().bind(commonChatModel.getIsTwoPartyPrivateChatChannelSelectionVisible());
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        searchBox.textProperty().unbindBidirectional(commonChatModel.getSearchText());
        twoPartyPrivateChatChannelSelection.visibleProperty().unbind();
        twoPartyPrivateChatChannelSelection.managedProperty().unbind();
    }
}
