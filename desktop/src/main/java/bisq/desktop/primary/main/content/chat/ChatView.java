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
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
public abstract class ChatView extends NavigationView<SplitPane, ChatModel, ChatController<?, ?>> {
    private final Label headline;
    private final Button searchButton, notificationsButton, channelInfoButton, helpButton;
    private final VBox left;
    private final VBox sideBar;
    protected final Pane chatMessagesComponent;
    private final Pane notificationsSettings;
    private final Pane channelInfo;
    private final ImageView peersRoboIconView;

    protected final HBox centerToolbar;
    private final Button createOfferButton;
    protected final VBox center;
    private final SearchBox searchBox;
    private Pane chatUserOverviewRoot;
    private Subscription sideBarWidthSubscription, sideBarChangedSubscription, rootWidthSubscription, chatUserOverviewRootSubscription;

    public ChatView(ChatModel model,
                    ChatController<?, ?> controller,
                    Pane marketChannelSelection,
                    Pane privateChannelSelection,
                    Pane chatMessagesComponent,
                    Pane notificationsSettings,
                    Pane channelInfo) {
        super(new SplitPane(), model, controller);
        this.chatMessagesComponent = chatMessagesComponent;

        this.notificationsSettings = notificationsSettings;
        this.channelInfo = channelInfo;

        // Undo default padding of ContentView 
        root.setPadding(new Insets(-34, -67, -67, -68));

        createOfferButton = new Button(Res.get("satoshisquareapp.chat.createOffer.button"));
        createOfferButton.setMaxWidth(Double.MAX_VALUE);
        createOfferButton.setMinHeight(37);
        createOfferButton.setDefaultButton(true);

        VBox.setMargin(createOfferButton, new Insets(-2, 24, 17, 24));

        // Left
        left = Layout.vBoxWith(
                marketChannelSelection,
                Layout.separator(),
                privateChannelSelection,
                Spacer.fillVBox(),
                createOfferButton
        );
        left.getStyleClass().add("bisq-dark-bg");
        left.setPrefWidth(210);
        left.setMinWidth(210);

        // Center toolbar
        // peersRoboIconView only visible for private channels
        peersRoboIconView = new ImageView();
        peersRoboIconView.setFitWidth(42);
        peersRoboIconView.setFitHeight(42);
        HBox.setMargin(peersRoboIconView, new Insets(2, 0, 2, 0));

        headline = new Label();
        headline.setId("chat-messages-headline");
        HBox.setMargin(headline, new Insets(0, 0, 0, 0));

        searchBox = new SearchBox();
        //searchBox.setPrefWidth(140);

        searchButton = BisqIconButton.createIconButton("icon-search", Res.get("search"));
        notificationsButton = BisqIconButton.createIconButton("icon-bell", Res.get("notifications"));
        channelInfoButton = BisqIconButton.createIconButton("icon-info", Res.get("chat.channelInfo"));
        helpButton = BisqIconButton.createIconButton("icon-help", Res.get("help"));

        centerToolbar = new HBox(
                10,
                peersRoboIconView,
                headline,
                Spacer.fillHBox(),
                searchBox,
                searchButton,
                notificationsButton,
                channelInfoButton,
                helpButton
        );
        centerToolbar.setAlignment(Pos.CENTER);
        centerToolbar.setMinHeight(64);
        centerToolbar.setPadding(new Insets(0, 20, 0, 24));

        // sideBar
        sideBar = new VBox(notificationsSettings, channelInfo);
        sideBar.getStyleClass().add("bisq-dark-bg");
        sideBar.setAlignment(Pos.TOP_RIGHT);
        sideBar.setFillWidth(true);

        VBox.setVgrow(chatMessagesComponent, Priority.ALWAYS);
        center = new VBox(centerToolbar, chatMessagesComponent);
        chatMessagesComponent.setMinWidth(700);
        root.getItems().addAll(left, center, sideBar);
    }

    @Override
    protected void onViewAttached() {
        peersRoboIconView.managedProperty().bind(model.getPeersRoboIconVisible());
        peersRoboIconView.visibleProperty().bind(model.getPeersRoboIconVisible());
        peersRoboIconView.imageProperty().bind(model.getPeersRoboIconImage());
        headline.textProperty().bind(model.getSelectedChannelAsString());
        searchBox.visibleProperty().bind(model.getSearchFieldVisible());
        searchBox.managedProperty().bind(model.getSearchFieldVisible());
        notificationsSettings.visibleProperty().bind(model.getNotificationsVisible());
        notificationsSettings.managedProperty().bind(model.getNotificationsVisible());
        channelInfo.visibleProperty().bind(model.getChannelInfoVisible());
        channelInfo.managedProperty().bind(model.getChannelInfoVisible());
        sideBar.visibleProperty().bind(model.getSideBarVisible());
        sideBar.managedProperty().bind(model.getSideBarVisible());
        createOfferButton.visibleProperty().bind(model.getCreateOfferButtonVisible());
        createOfferButton.managedProperty().bind(model.getCreateOfferButtonVisible());

        searchButton.setOnAction(e -> controller.onToggleFilterBox());
        notificationsButton.setOnAction(e -> controller.onToggleNotifications());
        channelInfoButton.setOnAction(e -> controller.onToggleChannelInfo());
        helpButton.setOnAction(e -> controller.onToggleHelp());
        createOfferButton.setOnAction(e -> controller.onCreateOffer());
        searchBox.textProperty().bindBidirectional(model.getSearchText());

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

        sideBarWidthSubscription = EasyBind.subscribe(model.getSideBarWidth(), w -> updateDividerPositions());
        sideBarChangedSubscription = EasyBind.subscribe(model.getSideBarChanged(), c -> updateDividerPositions());
        rootWidthSubscription = EasyBind.subscribe(root.widthProperty(), w -> updateDividerPositions());
    }

    @Override
    protected void onViewDetached() {
        peersRoboIconView.managedProperty().unbind();
        peersRoboIconView.visibleProperty().unbind();
        peersRoboIconView.imageProperty().unbind();
        headline.textProperty().unbind();
        searchBox.visibleProperty().unbind();
        searchBox.managedProperty().unbind();
        notificationsSettings.visibleProperty().unbind();
        notificationsSettings.managedProperty().unbind();
        channelInfo.visibleProperty().unbind();
        channelInfo.managedProperty().unbind();
        sideBar.visibleProperty().unbind();
        sideBar.managedProperty().unbind();
        createOfferButton.visibleProperty().unbind();
        createOfferButton.managedProperty().unbind();

        searchButton.setOnAction(null);
        notificationsButton.setOnAction(null);
        channelInfoButton.setOnAction(null);
        helpButton.setOnAction(null);
        createOfferButton.setOnAction(null);
        searchBox.textProperty().unbindBidirectional(model.getSearchText());

        chatUserOverviewRootSubscription.unsubscribe();
        rootWidthSubscription.unsubscribe();
        sideBarWidthSubscription.unsubscribe();
        sideBarChangedSubscription.unsubscribe();
    }

    private void updateDividerPositions() {
        double rootWidth = root.getWidth();
        if (rootWidth > 0) {
            root.setDividerPosition(0, left.getPrefWidth() / rootWidth);
            double sideBarWidth = model.getSideBarWidth().get();
            // rootWidth is left+center
            root.setDividerPosition(1, (rootWidth) / (rootWidth + sideBarWidth));

            // Lock to that initial position
            SplitPane.setResizableWithParent(left, false);
            SplitPane.setResizableWithParent(sideBar, false);

            // Hide right divider is sidebar is invisible
            // Hack as dividers are not accessible ;-(
            for (Node node : root.lookupAll(".split-pane-divider")) {
                ObservableList<Node> childrenUnmodifiable = node.getParent().getChildrenUnmodifiable();
                Node last = childrenUnmodifiable.get(childrenUnmodifiable.size() - 1);
                if (last.equals(node)) {
                    node.setVisible(sideBarWidth > 0);
                    node.setManaged(sideBarWidth > 0);
                }
            }
        }
    }
}
