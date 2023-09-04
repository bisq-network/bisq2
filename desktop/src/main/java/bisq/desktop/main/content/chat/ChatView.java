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
import bisq.desktop.common.view.NavigationView;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.SearchBox;
import bisq.desktop.main.MainView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public abstract class ChatView extends NavigationView<AnchorPane, ChatModel, ChatController<?, ?>> {
    protected final Label channelTitle = new Label();
    protected Button helpButton, infoButton, closeFilterButton;
    protected final VBox left = new VBox();
    protected final VBox sideBar = new VBox();
    protected final VBox centerVBox = new VBox();
    protected final HBox titleHBox = new HBox(10);
    protected final HBox containerHBox = new HBox();
    protected final Region twoPartyPrivateChatChannelSelection;
    protected final Pane channelSidebar, chatMessagesComponent;
    protected final SearchBox searchBox = new SearchBox();
    protected final Region topSeparator = Layout.hLine();
    protected Pane chatUserOverviewRoot;
    protected Subscription channelIconPin, chatUserOverviewRootSubscription;

    public ChatView(ChatModel model,
                    ChatController<?, ?> controller,
                    Region publicChannelSelection,
                    Region twoPartyPrivateChatChannelSelection,
                    Pane chatMessagesComponent,
                    Pane channelSidebar) {
        super(new AnchorPane(), model, controller);
        this.twoPartyPrivateChatChannelSelection = twoPartyPrivateChatChannelSelection;
        this.chatMessagesComponent = chatMessagesComponent;
        this.channelSidebar = channelSidebar;

        configLeftVBox(publicChannelSelection, twoPartyPrivateChatChannelSelection);
        configTitleHBox();
        configCenterVBox();
        configSideBarVBox();
        configContainerHBox();
    }

    protected void configLeftVBox(Region publicChannelSelection, Region twoPartyPrivateChatChannelSelection) {
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
        searchBox.setPrefWidth(200);
        helpButton = BisqIconButton.createIconButton("icon-help", model.getHelpTitle());
        infoButton = BisqIconButton.createIconButton("icon-info", Res.get("chat.topMenu.channelInfoIcon.tooltip"));
        HBox.setMargin(infoButton, new Insets(0, 0, 0, -5));

        titleHBox.getChildren().addAll(
                channelTitle,
                Spacer.fillHBox(),
                searchBox,
                helpButton,
                infoButton
        );
        titleHBox.setAlignment(Pos.CENTER);
        titleHBox.setMinHeight(64);
        titleHBox.setPadding(new Insets(0, 20, 0, 25));
    }

    protected void configCenterVBox() {
        VBox.setVgrow(chatMessagesComponent, Priority.ALWAYS);
        chatMessagesComponent.setMinWidth(700);
        centerVBox.getChildren().addAll(titleHBox, topSeparator, chatMessagesComponent);
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

        HBox.setHgrow(left, Priority.NEVER);
        HBox.setHgrow(centerVBox, Priority.ALWAYS);
        HBox.setHgrow(sideBar, Priority.NEVER);
        containerHBox.getChildren().addAll(left, centerVBox, sideBar);
    }

    @Override
    protected void onViewAttached() {
        MainView.setFitToHeight(true);
        channelTitle.textProperty().bind(model.getChannelTitle());
        channelSidebar.visibleProperty().bind(model.getChannelSidebarVisible());
        channelSidebar.managedProperty().bind(model.getChannelSidebarVisible());
        sideBar.visibleProperty().bind(model.getSideBarVisible());
        sideBar.managedProperty().bind(model.getSideBarVisible());

        helpButton.setOnAction(e -> controller.onOpenHelp());
        infoButton.setOnAction(e -> controller.onToggleChannelInfo());
        searchBox.textProperty().bindBidirectional(model.getSearchText());

        twoPartyPrivateChatChannelSelection.visibleProperty().bind(model.getIsTwoPartyPrivateChatChannelSelectionVisible());
        twoPartyPrivateChatChannelSelection.managedProperty().bind(model.getIsTwoPartyPrivateChatChannelSelectionVisible());

        chatUserOverviewRootSubscription = EasyBind.subscribe(model.getChatUserDetailsRoot(),
                pane -> {
                    if (chatUserOverviewRoot != null) {
                        sideBar.getChildren().remove(chatUserOverviewRoot);
                        chatUserOverviewRoot = null;
                    }

                    if (pane != null) {
                        sideBar.getChildren().add(pane);
                        chatUserOverviewRoot = pane;
                    }
                });

        channelIconPin = EasyBind.subscribe(model.getChannelIconNode(), node -> {
            if (node != null) {
                channelTitle.setGraphic(node);
                channelTitle.setGraphicTextGap(10);
                node.setStyle("-fx-cursor: hand;");
                node.setOnMouseClicked(e -> controller.onToggleChannelInfo());
            } else {
                channelTitle.setGraphic(null);
            }
        });
    }

    @Override
    protected void onViewDetached() {
        channelTitle.textProperty().unbind();
        channelSidebar.visibleProperty().unbind();
        channelSidebar.managedProperty().unbind();
        sideBar.visibleProperty().unbind();
        sideBar.managedProperty().unbind();

        twoPartyPrivateChatChannelSelection.visibleProperty().unbind();
        twoPartyPrivateChatChannelSelection.managedProperty().unbind();

        helpButton.setOnAction(null);
        infoButton.setOnAction(null);

        searchBox.textProperty().unbindBidirectional(model.getSearchText());

        chatUserOverviewRootSubscription.unsubscribe();
        channelIconPin.unsubscribe();
    }
}
