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

package bisq.desktop.primary.main.content.chat;

import bisq.desktop.common.utils.Layout;
import bisq.desktop.common.view.NavigationView;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.SearchBox;
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
public abstract class ChatView extends NavigationView<HBox, ChatModel, ChatController<?, ?>> {
    private final Label channelTitle;
    private final Button helpButton, channelInfoButton;
    protected final VBox left;
    private final VBox sideBar;
    private final Region twoPartyPrivateChannelSelection;
    protected final Pane chatMessagesComponent;
    private final Pane channelSidebar;
    protected final HBox centerToolbar;

    protected final VBox centerVBox;
    private final SearchBox searchBox;
    private Pane chatUserOverviewRoot;
    private Subscription chatUserOverviewRootSubscription;
    private Subscription channelIconPin;

    public ChatView(ChatModel model,
                    ChatController<?, ?> controller,
                    Region publicChannelSelection,
                    Region twoPartyPrivateChatChannelSelection,
                    Pane chatMessagesComponent,
                    Pane channelSidebar) {
        super(new HBox(), model, controller);

        this.twoPartyPrivateChannelSelection = twoPartyPrivateChatChannelSelection;
        this.chatMessagesComponent = chatMessagesComponent;

        this.channelSidebar = channelSidebar;

        // Undo default padding of ContentView 
        root.setPadding(new Insets(-34, -67, -67, -68));

        // Left
        left = new VBox(
                publicChannelSelection,
                Layout.separator(),
                twoPartyPrivateChatChannelSelection,
                Spacer.fillVBox());
        left.getStyleClass().add("bisq-grey-2-bg");
        left.setPrefWidth(210);
        left.setMinWidth(210);

        // Center toolbar
        channelTitle = new Label();
        channelTitle.setId("chat-messages-headline");
        HBox.setMargin(channelTitle, new Insets(0, 0, 0, 0));

        searchBox = new SearchBox();
        searchBox.setPrefWidth(200);
        helpButton = BisqIconButton.createIconButton("icon-help", Res.get("help"));
        channelInfoButton = BisqIconButton.createIconButton("icon-info", Res.get("chat.channelInfo"));
        HBox.setMargin(channelInfoButton, new Insets(0, 0, 0, -5));

        centerToolbar = new HBox(
                10,
                channelTitle,
                Spacer.fillHBox(),
                searchBox,
                helpButton,
                channelInfoButton
        );
        centerToolbar.setAlignment(Pos.CENTER);
        centerToolbar.setMinHeight(64);
        centerToolbar.setPadding(new Insets(0, 20, 0, 25));

        // sideBar
        sideBar = new VBox(channelSidebar);
        sideBar.getStyleClass().add("bisq-grey-2-bg");
        sideBar.setAlignment(Pos.TOP_RIGHT);
        sideBar.setFillWidth(true);

        VBox.setVgrow(chatMessagesComponent, Priority.ALWAYS);
        centerVBox = new VBox(centerToolbar, Layout.separator(), chatMessagesComponent);
        chatMessagesComponent.setMinWidth(700);
        HBox.setHgrow(left, Priority.NEVER);
        HBox.setHgrow(centerVBox, Priority.ALWAYS);
        HBox.setHgrow(sideBar, Priority.NEVER);
        root.getChildren().addAll(left, centerVBox, sideBar);
    }

    @Override
    protected void onViewAttached() {
        channelTitle.textProperty().bind(model.getChannelTitle());
        channelSidebar.visibleProperty().bind(model.getChannelSidebarVisible());
        channelSidebar.managedProperty().bind(model.getChannelSidebarVisible());
        sideBar.visibleProperty().bind(model.getSideBarVisible());
        sideBar.managedProperty().bind(model.getSideBarVisible());

        helpButton.setOnAction(e -> controller.onToggleHelp());
        channelInfoButton.setOnAction(e -> controller.onToggleChannelInfo());
        searchBox.textProperty().bindBidirectional(model.getSearchText());

        twoPartyPrivateChannelSelection.visibleProperty().bind(model.getIsTwoPartyPrivateChatChannelSelectionVisible());
        twoPartyPrivateChannelSelection.managedProperty().bind(model.getIsTwoPartyPrivateChatChannelSelectionVisible());

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

        channelIconPin = EasyBind.subscribe(model.getChannelIcon(), icon -> {
            if (icon != null) {
                channelTitle.setGraphic(icon);
                channelTitle.setGraphicTextGap(10);
                icon.setStyle("-fx-cursor: hand;");
                icon.setOnMouseClicked(e -> controller.onToggleChannelInfo());
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

        twoPartyPrivateChannelSelection.visibleProperty().unbind();
        twoPartyPrivateChannelSelection.managedProperty().unbind();

        helpButton.setOnAction(null);
        channelInfoButton.setOnAction(null);

        searchBox.textProperty().unbindBidirectional(model.getSearchText());

        chatUserOverviewRootSubscription.unsubscribe();
        channelIconPin.unsubscribe();
    }
}
