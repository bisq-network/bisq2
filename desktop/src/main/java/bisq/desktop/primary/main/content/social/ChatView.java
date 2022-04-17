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

package bisq.desktop.primary.main.content.social;

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.jfx.BisqInputTextField;
import bisq.desktop.components.table.FilterBox;
import bisq.desktop.layout.Layout;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class ChatView extends View<SplitPane, ChatModel, ChatController> {
    private final Label selectedChannelLabel;
    private final Button searchButton, notificationsButton, infoButton, closeButton;
    private final Pane userProfileSelection;
    private final VBox left, sideBar;
    private final BisqInputTextField filterBoxRoot;
    private final Pane notificationsSettings;
    private final Pane channelInfo;

    private final HBox messagesListAndSideBar;
    private final ImageView peersRoboIconView;
    private Subscription chatUserOverviewRootSubscription;
    private Pane chatUserOverviewRoot;
    private Subscription widthSubscription;

    public ChatView(ChatModel model,
                    ChatController controller,
                    Pane userProfileSelection,
                    Pane marketChannelSelection,
                    Pane privateChannelSelection,
                    Pane chatMessagesComponent,
                    Pane notificationsSettings,
                    Pane channelInfo,
                    FilterBox filterBox) {
        super(new SplitPane(), model, controller);

        this.notificationsSettings = notificationsSettings;
        this.channelInfo = channelInfo;
        this.userProfileSelection = userProfileSelection;

        // Left 
        left = Layout.vBoxWith(userProfileSelection,
                marketChannelSelection,
                privateChannelSelection,
                Spacer.fillVBox()
        );
        left.setPadding(new Insets(0, 10, 0, 0));
        left.setPrefWidth(300);
        left.setMinWidth(180);

        // Center toolbar
        // peersRoboIconView only visible for private channels
        peersRoboIconView = new ImageView();
        peersRoboIconView.setFitWidth(40);
        peersRoboIconView.setFitHeight(40);
        HBox.setMargin(peersRoboIconView, new Insets(5, -20, 5, 12));

        selectedChannelLabel = new Label();
        selectedChannelLabel.setId("chat-messages-headline");
        HBox.setMargin(selectedChannelLabel, new Insets(1, 0, 0, 20));

        searchButton = BisqIconButton.createIconButton(AwesomeIcon.SEARCH);
        searchButton.setOpacity(0.4);
        notificationsButton = BisqIconButton.createIconButton(AwesomeIcon.BELL);
        notificationsButton.setOpacity(0.4);
        infoButton = BisqIconButton.createIconButton(AwesomeIcon.INFO_SIGN);
        infoButton.setOpacity(0.4);

        HBox centerToolbar = Layout.hBoxWith(peersRoboIconView, selectedChannelLabel, Spacer.fillHBox(), searchButton, notificationsButton, infoButton);
        centerToolbar.setStyle("-fx-background-color: -bisq-grey-left-nav-bg");
        centerToolbar.setAlignment(Pos.CENTER);
        // centerToolbar.setPadding(new Insets(10,10,0,20));
        centerToolbar.setMinHeight(50);
        VBox.setMargin(centerToolbar, new Insets(0, 0, 0, 10));


        // sideBar
        closeButton = BisqIconButton.createIconButton(AwesomeIcon.REMOVE_SIGN);
        closeButton.setOpacity(0.4);
        VBox.setMargin(closeButton, new Insets(-10, -45, 0, 0));
        channelInfo.setMinWidth(200);
        sideBar = Layout.vBoxWith(closeButton, notificationsSettings, channelInfo);
        sideBar.setAlignment(Pos.TOP_RIGHT);
        sideBar.setMinWidth(340);
        sideBar.setPadding(new Insets(10, 20, 20, 20));
        sideBar.setFillWidth(true);
        sideBar.setStyle("-fx-background-color: -bisq-grey-left-nav-bg");

        filterBoxRoot = filterBox.getRoot();
        filterBoxRoot.setStyle("-fx-background-color: -bisq-grey-left-nav-bg;");
        VBox.setMargin(filterBoxRoot, new Insets(0, 0, 0, 10));

        HBox.setMargin(chatMessagesComponent, new Insets(0, 0, 10, 10));
        HBox.setHgrow(chatMessagesComponent, Priority.ALWAYS);
        VBox.setMargin(chatMessagesComponent, new Insets(0, 0, 15, 0));
        chatMessagesComponent.setMinWidth(650);
        messagesListAndSideBar = Layout.hBoxWith(chatMessagesComponent, sideBar);

        VBox.setVgrow(messagesListAndSideBar, Priority.ALWAYS);

        VBox center = Layout.vBoxWith(centerToolbar, filterBoxRoot, messagesListAndSideBar);
        center.setStyle("-fx-background-color: transparent");
        messagesListAndSideBar.setStyle("-fx-background-color: transparent");

        root.getItems().addAll(left, center);
    }

    @Override
    protected void onViewAttached() {
        peersRoboIconView.managedProperty().bind(model.getPeersRoboIconVisible());
        peersRoboIconView.visibleProperty().bind(model.getPeersRoboIconVisible());
        peersRoboIconView.imageProperty().bind(model.getPeersRoboIconImage());
        selectedChannelLabel.textProperty().bind(model.getSelectedChannelAsString());
        filterBoxRoot.visibleProperty().bind(model.getFilterBoxVisible());
        filterBoxRoot.managedProperty().bind(model.getFilterBoxVisible());
        notificationsSettings.visibleProperty().bind(model.getNotificationsVisible());
        notificationsSettings.managedProperty().bind(model.getNotificationsVisible());
        channelInfo.visibleProperty().bind(model.getChannelInfoVisible());
        channelInfo.managedProperty().bind(model.getChannelInfoVisible());
        sideBar.visibleProperty().bind(model.getSideBarVisible());
        sideBar.managedProperty().bind(model.getSideBarVisible());

        searchButton.setOnAction(e -> controller.onToggleFilterBox());
        notificationsButton.setOnAction(e -> controller.onToggleNotifications());
        infoButton.setOnAction(e -> controller.onToggleChannelInfo());
        closeButton.setOnAction(e -> controller.onCloseSideBar());

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
        widthSubscription = EasyBind.subscribe(root.widthProperty(), w -> {
            double width = w.doubleValue();
            if (width > 0) {
                root.setDividerPosition(0, left.getPrefWidth() / width);
                // Lock to that initial position
                SplitPane.setResizableWithParent(left, false);
                UIThread.runOnNextRenderFrame(() -> {
                    widthSubscription.unsubscribe();
                    widthSubscription = null;
                });
            }
        });
    }

    @Override
    protected void onViewDetached() {
        peersRoboIconView.managedProperty().unbind();
        peersRoboIconView.visibleProperty().unbind();
        peersRoboIconView.imageProperty().unbind();
        selectedChannelLabel.textProperty().unbind();
        filterBoxRoot.visibleProperty().unbind();
        filterBoxRoot.managedProperty().unbind();
        notificationsSettings.visibleProperty().unbind();
        notificationsSettings.managedProperty().unbind();
        channelInfo.visibleProperty().unbind();
        channelInfo.managedProperty().unbind();
        sideBar.visibleProperty().unbind();
        sideBar.managedProperty().unbind();

        searchButton.setOnAction(null);
        notificationsButton.setOnAction(null);
        infoButton.setOnAction(null);
        closeButton.setOnAction(null);

        chatUserOverviewRootSubscription.unsubscribe();
        if (widthSubscription != null) {
            widthSubscription.unsubscribe();
            widthSubscription = null;
        }
    }
}
