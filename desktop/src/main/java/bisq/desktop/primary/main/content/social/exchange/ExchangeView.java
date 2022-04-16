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

package bisq.desktop.primary.main.content.social.exchange;

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqInputTextField;
import bisq.desktop.components.table.FilterBox;
import bisq.desktop.layout.Layout;
import bisq.desktop.primary.main.content.social.components.MarketChannelSelection;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class ExchangeView extends View<SplitPane, ExchangeModel, ExchangeController> {
    private final Label selectedChannelLabel;
    private final Button searchButton, notificationsButton, infoButton, closeButton;
    private final Pane userProfileSelection;
    private final VBox left, sideBar;
    private final BisqInputTextField filterBoxRoot;
    private final MarketChannelSelection marketChannelSelection;
    private final Pane notificationsSettings;
    private final Pane channelInfo;

    private final HBox messagesListAndSideBar;
    private Subscription chatUserOverviewRootSubscription;
    private Pane chatUserOverviewRoot;
    private Subscription widthSubscription;

    public ExchangeView(ExchangeModel model,
                        ExchangeController controller,
                        Pane userProfileSelection,
                        MarketChannelSelection marketChannelSelection,
                        Pane privateChannelSelection,
                        Pane chatMessagesComponent,
                        Pane notificationsSettings,
                        Pane channelInfo,
                        FilterBox filterBox) {
        super(new SplitPane(), model, controller);
        this.marketChannelSelection = marketChannelSelection;

        this.notificationsSettings = notificationsSettings;
        this.channelInfo = channelInfo;
        this.userProfileSelection = userProfileSelection;
        //  userProfileSelection.setPrefWidth(300);

        // Left 
        marketChannelSelection.setCellFactory(getMarketChannelCellFactory());

        left = Layout.vBoxWith(userProfileSelection,
                marketChannelSelection.getRoot(),
                Spacer.fillVBox()
        );
        left.setPadding(new Insets(0, 20, 0, 0));
        left.setPrefWidth(300);


        // Center toolbar
        selectedChannelLabel = new Label();
        selectedChannelLabel.getStyleClass().add("headline-label");

        filterBoxRoot = filterBox.getRoot();
        filterBoxRoot.setStyle("-fx-background-color: -bisq-grey-left-nav-bg");
        HBox.setHgrow(filterBoxRoot, Priority.ALWAYS);
        HBox.setMargin(filterBoxRoot, new Insets(0, 0, 0, 10));

        searchButton = BisqIconButton.createIconButton(AwesomeIcon.SEARCH);
        notificationsButton = BisqIconButton.createIconButton(AwesomeIcon.BELL);
        infoButton = BisqIconButton.createIconButton(AwesomeIcon.INFO_SIGN);
        HBox centerToolbar = Layout.hBoxWith(selectedChannelLabel, filterBoxRoot, searchButton, notificationsButton, infoButton);
        centerToolbar.setStyle("-fx-background-color: -fx-base");
        centerToolbar.setPadding(new Insets(10, 10, 10, 10));

        // sideBar
        closeButton = BisqIconButton.createIconButton(AwesomeIcon.REMOVE_SIGN);
        VBox.setMargin(closeButton, new Insets(-10, -20, 0, 0));
        channelInfo.setMinWidth(200);
        sideBar = Layout.vBoxWith(closeButton, notificationsSettings, channelInfo);
        sideBar.setAlignment(Pos.TOP_RIGHT);
        sideBar.setPadding(new Insets(10, 20, 20, 20));
        sideBar.setFillWidth(true);
        sideBar.setStyle("-fx-background-color: -bisq-grey-left-nav-bg");

        // messagesListAndSideBar
        messagesListAndSideBar = Layout.hBoxWith(chatMessagesComponent, sideBar);
        HBox.setHgrow(chatMessagesComponent, Priority.ALWAYS);
        VBox.setVgrow(messagesListAndSideBar, Priority.ALWAYS);
        VBox center = Layout.vBoxWith(centerToolbar, messagesListAndSideBar);
        center.setStyle("-fx-background-color: -fx-base");
        // center.setSpacing(0);
        messagesListAndSideBar.setPadding(new Insets(10, 10, 10, 10));
        messagesListAndSideBar.setStyle("-fx-background-color: -fx-base");

        root.getItems().addAll(left, center);


    }

    @NonNull
    private Callback<ListView<MarketChannelSelection.MarketChannelItem>, ListCell<MarketChannelSelection.MarketChannelItem>> getMarketChannelCellFactory() {
        return new Callback<>() {
            @Override
            public ListCell<MarketChannelSelection.MarketChannelItem> call(ListView<MarketChannelSelection.MarketChannelItem> list) {
                return new ListCell<>() {
                    @Override
                    public void updateItem(final MarketChannelSelection.MarketChannelItem item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null && !empty) {
                            Label market = new Label(item.toString());
                            HBox hBox = new HBox();
                            hBox.setAlignment(Pos.CENTER_LEFT);
                            hBox.getChildren().addAll(market, Spacer.fillHBox());

                            Badge badge = new Badge(hBox);
                            badge.setTooltip(Res.get("social.marketChannels.numMessages"));
                            badge.setPosition(Pos.CENTER_RIGHT);
                            int numMessages = item.getNumMessages();
                            if (numMessages > 0) {
                                badge.setText(String.valueOf(numMessages));
                            }
                            setGraphic(badge);
                        } else {
                            setGraphic(null);
                        }
                    }
                };
            }
        };
    }

    @Override
    protected void onViewAttached() {
        selectedChannelLabel.textProperty().bind(model.getSelectedChannelAsString());
        filterBoxRoot.visibleProperty().bind(model.getFilterBoxVisible());
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
                // lock to that initial position
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
        selectedChannelLabel.textProperty().unbind();
        filterBoxRoot.visibleProperty().unbind();
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
