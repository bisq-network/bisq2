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

import bisq.desktop.common.view.NavigationView;
import bisq.desktop.components.controls.SearchBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public abstract class BaseChatView extends NavigationView<ScrollPane, BaseChatModel, BaseChatController<?, ?>> {
    protected final static double HEADER_HEIGHT = 61;

    protected final Label channelTitle = new Label();
    protected final Label channelDescription = new Label();
    protected Button helpButton, infoButton;
    protected final VBox sideBar = new VBox();
    protected final VBox centerVBox = new VBox();
    protected final HBox titleHBox = new HBox(10);
    protected final HBox containerHBox = new HBox();
    protected final Pane channelSidebar, chatMessagesComponent;
    protected Pane chatUserOverviewRoot;
    protected Subscription /*channelIconPin, */chatUserOverviewRootSubscription;
    protected final SearchBox searchBox = new SearchBox();

    public BaseChatView(BaseChatModel model,
                        BaseChatController<?, ?> controller,
                        Pane chatMessagesComponent,
                        Pane channelSidebar) {
        super(new ScrollPane(), model, controller);

        this.chatMessagesComponent = chatMessagesComponent;
        this.channelSidebar = channelSidebar;

        configTitleHBox();
        configCenterVBox();
        configSideBarVBox();
        configContainerHBox();

        root.setFitToWidth(true);
        root.setFitToHeight(true);
    }

    protected abstract void configTitleHBox();

    protected abstract void configCenterVBox();

    protected abstract void configSideBarVBox();

    protected abstract void configContainerHBox();

    @Override
    protected void onViewAttached() {
        channelTitle.textProperty().bind(model.getChannelTitle());
        channelDescription.textProperty().bind(model.getChannelDescription());
        channelSidebar.visibleProperty().bind(model.getChannelSidebarVisible());
        channelSidebar.managedProperty().bind(model.getChannelSidebarVisible());
        sideBar.visibleProperty().bind(model.getSideBarVisible());
        sideBar.managedProperty().bind(model.getSideBarVisible());
        searchBox.textProperty().bindBidirectional(model.getSearchText());

        if (helpButton != null) {
            helpButton.setOnAction(e -> controller.onOpenHelp());
        }
        if (infoButton != null) {
            infoButton.setOnAction(e -> controller.onToggleChannelInfo());
        }

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

//        channelIconPin = EasyBind.subscribe(model.getChannelIconNode(), node -> {
//            if (node != null) {
//                channelTitle.setGraphic(node);
//                channelTitle.setGraphicTextGap(10);
//                node.setStyle("-fx-cursor: hand;");
//                node.setOnMouseClicked(e -> controller.onToggleChannelInfo());
//            } else {
//                channelTitle.setGraphic(null);
//            }
//        });
    }

    @Override
    protected void onViewDetached() {
        channelTitle.textProperty().unbind();
        channelDescription.textProperty().unbind();
        channelSidebar.visibleProperty().unbind();
        channelSidebar.managedProperty().unbind();
        sideBar.visibleProperty().unbind();
        sideBar.managedProperty().unbind();
        searchBox.textProperty().unbindBidirectional(model.getSearchText());

        if (helpButton != null) {
            helpButton.setOnAction(null);
        }
        if (infoButton != null) {
            infoButton.setOnAction(null);
        }

        chatUserOverviewRootSubscription.unsubscribe();
//        channelIconPin.unsubscribe();
    }
}
