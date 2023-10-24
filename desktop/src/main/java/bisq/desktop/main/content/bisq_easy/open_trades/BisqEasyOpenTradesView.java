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

package bisq.desktop.main.content.bisq_easy.open_trades;

import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.data.Triple;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.desktop.common.Layout;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.utils.SceneUtil;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Badge;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.desktop.main.content.chat.ChatView;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.i18n.Res;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.notifications.NotificationsService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeFormatter;
import bisq.trade.bisq_easy.BisqEasyTradeUtils;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;

@Slf4j
public class BisqEasyOpenTradesView extends ChatView {
    private final VBox tradeWelcomeViewRoot, tradeStateViewRoot, chatVBox;
    private final BisqTableView<ListItem> tableView;
    private final Button toggleChatWindowButton;
    private Subscription noOpenTradesPin, tradeRulesAcceptedPin, tableViewSelectionPin,
            selectedModelItemPin, chatWindowPin;

    public BisqEasyOpenTradesView(BisqEasyOpenTradesModel model,
                                  BisqEasyOpenTradesController controller,
                                  HBox tradeDataHeader,
                                  VBox chatMessagesComponent,
                                  Pane channelSidebar,
                                  VBox tradeStateViewRoot,
                                  VBox tradeWelcomeViewRoot) {
        super(model,
                controller,
                chatMessagesComponent,
                channelSidebar);
        this.tradeStateViewRoot = tradeStateViewRoot;
        this.tradeWelcomeViewRoot = tradeWelcomeViewRoot;

        // Table view
        tableView = new BisqTableView<>(getModel().getSortedList());
        tableView.getStyleClass().addAll("bisq-easy-open-trades", "hide-horizontal-scrollbar");
        configTableView();

        VBox.setMargin(tableView, new Insets(10, 0, 0, 0));
        Triple<Label, HBox, VBox> triple = BisqEasyViewUtils.getContainer(Res.get("bisqEasy.openTrades.table.headline"), tableView);
        VBox tableViewVBox = triple.getThird();

        // ChatBox
        toggleChatWindowButton = new Button();
        toggleChatWindowButton.setGraphicTextGap(10);
        toggleChatWindowButton.getStyleClass().add("outlined-button");
        toggleChatWindowButton.setMinWidth(140);

        tradeDataHeader.getChildren().addAll(Spacer.fillHBox(), toggleChatWindowButton);

        chatMessagesComponent.setMinHeight(200);
        chatMessagesComponent.setPadding(new Insets(0, -30, -15, -30));

        VBox.setMargin(chatMessagesComponent, new Insets(0, 30, 15, 30));
        VBox.setVgrow(chatMessagesComponent, Priority.ALWAYS);
        chatVBox = new VBox(tradeDataHeader, Layout.hLine(), chatMessagesComponent);
        chatVBox.getStyleClass().add("bisq-easy-container");

        VBox.setMargin(tradeWelcomeViewRoot, new Insets(0, 0, 10, 0));
        VBox.setMargin(tableViewVBox, new Insets(0, 0, 10, 0));
        VBox.setMargin(tradeStateViewRoot, new Insets(0, 0, 10, 0));
        VBox.setVgrow(tradeStateViewRoot, Priority.ALWAYS);
        VBox.setVgrow(chatVBox, Priority.ALWAYS);
        centerVBox.getChildren().addAll(tradeWelcomeViewRoot, tableViewVBox, tradeStateViewRoot, chatVBox);
    }

    @Override
    protected void configTitleHBox() {
    }

    @Override
    protected void configCenterVBox() {
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
        HBox.setHgrow(centerVBox, Priority.ALWAYS);
        HBox.setHgrow(sideBar, Priority.NEVER);
        containerHBox.getChildren().addAll(centerVBox, sideBar);
        containerHBox.setPadding(new Insets(0, 40, 0, 40));

        Layout.pinToAnchorPane(containerHBox, 30, 0, 0, 0);
        root.setContent(containerHBox);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

        BisqEasyOpenTradesModel model = getModel();

        tradeWelcomeViewRoot.visibleProperty().bind(model.getTradeWelcomeVisible());
        tradeWelcomeViewRoot.managedProperty().bind(model.getTradeWelcomeVisible());
        tradeStateViewRoot.visibleProperty().bind(model.getTradeStateVisible());
        tradeStateViewRoot.managedProperty().bind(model.getTradeStateVisible());
        chatVBox.visibleProperty().bind(model.getChatVisible());
        chatVBox.managedProperty().bind(model.getChatVisible());

        selectedModelItemPin = EasyBind.subscribe(model.getSelectedItem(), selected ->
                tableView.getSelectionModel().select(selected));

        tradeRulesAcceptedPin = EasyBind.subscribe(model.getTradeRulesAccepted(),
                tradeRulesAccepted -> {
                    if (tradeRulesAccepted) {
                        tableView.setOnMouseClicked(null);
                        tableViewSelectionPin = EasyBind.subscribe(tableView.getSelectionModel().selectedItemProperty(),
                                item -> {
                                    if (item != null) {
                                        getController().onSelectItem(item);
                                    }
                                });
                        UIThread.runOnNextRenderFrame(() -> tradeRulesAcceptedPin.unsubscribe());
                    } else {
                        tableView.setOnMouseClicked(e -> getController().onShowTradeRulesAcceptedWarning());
                    }
                });
        noOpenTradesPin = EasyBind.subscribe(model.getNoOpenTrades(),
                noOpenTrades -> {
                    if (noOpenTrades) {
                        tableView.removeListeners();
                        tableView.setPlaceholderText(Res.get("bisqEasy.openTrades.noTrades"));
                        tableView.allowVerticalScrollbar();
                        tableView.setFixHeight(150);
                        tableView.getStyleClass().add("empty-table");
                    } else {
                        tableView.setPlaceholder(null);
                        tableView.adjustHeightToNumRows();
                        tableView.hideVerticalScrollbar();
                        tableView.getStyleClass().remove("empty-table");
                    }
                });

        chatWindowPin = EasyBind.subscribe(model.getChatWindow(), this::chatWindowChanged);

        toggleChatWindowButton.setOnAction(e -> getController().onToggleChatWindow());
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        // TODO would be nice to keep it open or allow multiple windows... but for now keep it simple...
        getController().onCloseChatWindow();

        tableView.removeListeners();

        tradeWelcomeViewRoot.visibleProperty().unbind();
        tradeWelcomeViewRoot.managedProperty().unbind();
        tradeStateViewRoot.visibleProperty().unbind();
        tradeStateViewRoot.managedProperty().unbind();
        chatVBox.visibleProperty().unbind();
        chatVBox.managedProperty().unbind();

        selectedModelItemPin.unsubscribe();
        if (tableViewSelectionPin != null) {
            tableViewSelectionPin.unsubscribe();
        }
        noOpenTradesPin.unsubscribe();
        tradeRulesAcceptedPin.unsubscribe();
        chatWindowPin.unsubscribe();

        toggleChatWindowButton.setOnAction(null);
        tableView.setOnMouseClicked(null);
    }


    private void chatWindowChanged(Stage chatWindow) {
        if (chatWindow == null) {
            ImageView icon = ImageUtil.getImageViewById("detach");
            toggleChatWindowButton.setText(Res.get("bisqEasy.openTrades.chat.detach"));
            toggleChatWindowButton.setTooltip(new BisqTooltip(Res.get("bisqEasy.openTrades.chat.detach.tooltip")));
            toggleChatWindowButton.setGraphic(icon);

            if (!centerVBox.getChildren().contains(chatVBox)) {
                centerVBox.getChildren().add(3, chatVBox);
            }
        } else {
            ImageView icon = ImageUtil.getImageViewById("attach");
            toggleChatWindowButton.setText(Res.get("bisqEasy.openTrades.chat.attach"));
            toggleChatWindowButton.setTooltip(new BisqTooltip(Res.get("bisqEasy.openTrades.chat.attach.tooltip")));
            toggleChatWindowButton.setGraphic(icon);

            chatWindow.titleProperty().bind(getModel().getChatWindowTitle());
            ImageUtil.addAppIcons(chatWindow);
            chatWindow.initModality(Modality.NONE);

            // We open the window at the button position (need to be done before we remove the chatVBox
            // TODO we could persist the position and size of the window and use it for next time opening...
            Point2D windowPoint = new Point2D(root.getScene().getWindow().getX(), root.getScene().getWindow().getY());
            Point2D scenePoint = new Point2D(root.getScene().getX(), root.getScene().getY());
            Point2D buttonPoint = toggleChatWindowButton.localToScene(0.0, 0.0);
            double x = Math.round(windowPoint.getX() + scenePoint.getX() + buttonPoint.getX());
            double y = Math.round(windowPoint.getY() + scenePoint.getY() + buttonPoint.getY());
            chatWindow.setX(x);
            chatWindow.setY(y);
            chatWindow.setMinWidth(600);
            chatWindow.setMinHeight(400);
            chatWindow.setWidth(1000);
            chatWindow.setHeight(700);

            chatWindow.setOnCloseRequest(event -> {
                event.consume();
                chatWindow.titleProperty().unbind();
                getController().onCloseChatWindow();
                chatWindow.hide();
            });

            chatWindow.show();

            centerVBox.getChildren().remove(chatVBox);

            Layout.pinToAnchorPane(chatVBox, 0, 0, 0, 0);
            AnchorPane windowRoot = new AnchorPane(chatVBox);
            windowRoot.getStyleClass().add("bisq-popup");

            Scene scene = new Scene(windowRoot);
            SceneUtil.configCss(scene);
            chatWindow.setScene(scene);

            // Avoid flicker
            chatWindow.setOpacity(0);
            UIThread.runOnNextRenderFrame(() -> chatWindow.setOpacity(1));
        }
    }


    private void configTableView() {
        tableView.getColumns().add(tableView.getSelectionMarkerColumn());

        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.me"))
                .fixWidth(45)
                .left()
                .comparator(Comparator.comparing(ListItem::getMyUserName))
                .setCellFactory(getMyUserCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .minWidth(95)
                .left()
                .comparator(Comparator.comparing(ListItem::getDirection))
                .valueSupplier(ListItem::getDirection)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.tradePeer"))
                .minWidth(110)
                .left()
                .comparator(Comparator.comparing(ListItem::getPeersUserName))
                .setCellFactory(getTradePeerCellFactory())
                .build());
        BisqTableColumn<ListItem> dateColumn = new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("temporal.date"))
                .fixWidth(85)
                .comparator(Comparator.comparing(ListItem::getDate).reversed())
                .setCellFactory(getDateCellFactory())
                .build();
        tableView.getColumns().add(dateColumn);
        tableView.getSortOrder().add(dateColumn);

        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.tradeId"))
                .minWidth(85)
                .comparator(Comparator.comparing(ListItem::getTradeId))
                .valueSupplier(ListItem::getShortTradeId)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.quoteAmount"))
                .fixWidth(95)
                .comparator(Comparator.comparing(ListItem::getQuoteAmount))
                .valueSupplier(ListItem::getQuoteAmountString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.baseAmount"))
                .fixWidth(120)
                .comparator(Comparator.comparing(ListItem::getBaseAmount))
                .valueSupplier(ListItem::getBaseAmountString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.price"))
                .fixWidth(135)
                .comparator(Comparator.comparing(ListItem::getPrice))
                .valueSupplier(ListItem::getPriceString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.paymentMethod"))
                .minWidth(130)
                .comparator(Comparator.comparing(ListItem::getPaymentMethod))
                .valueSupplier(ListItem::getPaymentMethod)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.makerTakerRole"))
                .minWidth(80)
                .right()
                .comparator(Comparator.comparing(ListItem::getMyRole))
                .valueSupplier(ListItem::getMyRole)
                .build());
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getDateCellFactory() {
        return column -> new TableCell<>() {

            @Override
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    Label date = new Label(item.getDateString());
                    date.getStyleClass().add("table-view-date-column-date");
                    Label time = new Label(item.getTimeString());
                    time.getStyleClass().add("table-view-date-column-time");
                    VBox vBox = new VBox(3, date, time);
                    vBox.setAlignment(Pos.CENTER);
                    setAlignment(Pos.CENTER);
                    setGraphic(vBox);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getMyUserCellFactory() {
        return column -> new TableCell<>() {

            @Override
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    UserProfileIcon userProfileIcon = new UserProfileIcon();
                    UserProfile userProfile = item.getChannel().getMyUserIdentity().getUserProfile();
                    userProfileIcon.setUserProfile(userProfile);
                    // Tooltip is not working if we add directly to the cell therefor we wrap into a StackPane
                    setGraphic(new StackPane(userProfileIcon));
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getTradePeerCellFactory() {
        return column -> new TableCell<>() {
            private Badge badge;

            @Override
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    UserProfileDisplay userProfileDisplay = new UserProfileDisplay(item.getChannel().getPeer());
                    userProfileDisplay.setReputationScore(item.getReputationScore());
                    setGraphic(userProfileDisplay);

                    badge = new Badge(userProfileDisplay);
                    badge.getStyleClass().add("open-trades-badge");
                    badge.textProperty().bind(item.getNumTradeNotification());
                    badge.setPosition(Pos.BOTTOM_LEFT);
                    badge.setBadgeInsets(new Insets(0, 0, 7.5, 20));
                    // Label color does not get applied from badge style when in a list cell even we use '!important' in the css.
                    badge.getLabel().setStyle("-fx-text-fill: black !important;");
                    setGraphic(badge);
                } else {
                    if (badge != null) {
                        badge.textProperty().unbind();
                        badge.dispose();
                    }
                    setGraphic(null);
                }
            }
        };
    }

    private BisqEasyOpenTradesModel getModel() {
        return (BisqEasyOpenTradesModel) model;
    }

    private BisqEasyOpenTradesController getController() {
        return (BisqEasyOpenTradesController) controller;
    }

    @Getter
    @EqualsAndHashCode
    static class ListItem implements TableItem {
        private final BisqEasyOpenTradeChannel channel;
        private final BisqEasyTrade trade;
        private final String offerId, tradeId, shortTradeId, myUserName, direction, peersUserName, dateString, timeString,
                market, priceString, baseAmountString, quoteAmountString, paymentMethod, myRole;
        private final long date, price, baseAmount, quoteAmount;
        private final UserProfile peersUserProfile;
        private final NotificationsService notificationsService;
        private final ChatNotificationService chatNotificationService;
        private final ReputationScore reputationScore;
        private final StringProperty numTradeNotification = new SimpleStringProperty();

        public ListItem(BisqEasyOpenTradeChannel channel,
                        BisqEasyTrade trade,
                        ReputationService reputationService,
                        NotificationsService notificationsService,
                        ChatNotificationService chatNotificationService) {
            this.channel = channel;
            this.trade = trade;

            peersUserProfile = channel.getPeer();
            this.notificationsService = notificationsService;
            this.chatNotificationService = chatNotificationService;
            peersUserName = peersUserProfile.getUserName();
            myUserName = channel.getMyUserIdentity().getUserName();
            direction = BisqEasyTradeFormatter.getDirection(trade);
            offerId = channel.getBisqEasyOffer().getId();
            this.tradeId = trade.getId();
            shortTradeId = trade.getShortId();

            BisqEasyContract contract = trade.getContract();
            date = trade.getDate();
            dateString = DateFormatter.formatDate(trade.getDate());
            timeString = DateFormatter.formatTime(trade.getDate());
            market = trade.getOffer().getMarket().toString();
            price = BisqEasyTradeUtils.getPriceQuote(trade).getValue();
            priceString = BisqEasyTradeFormatter.formatPriceWithCode(trade);
            baseAmount = contract.getBaseSideAmount();
            baseAmountString = BisqEasyTradeFormatter.formatBaseSideAmount(trade);
            quoteAmount = contract.getQuoteSideAmount();
            quoteAmountString = BisqEasyTradeFormatter.formatQuoteSideAmountWithCode(trade);
            paymentMethod = contract.getQuoteSidePaymentMethodSpec().getShortDisplayString();
            myRole = BisqEasyTradeFormatter.getMakerTakerRole(trade);
            reputationScore = reputationService.getReputationScore(peersUserProfile);

            notificationsService.subscribe(this::updateNumNotifications);
        }

        public void dispose() {
            notificationsService.unsubscribe(this::updateNumNotifications);
        }

        private void updateNumNotifications(String notificationId) {
            UIThread.run(() -> {
                int numNotifications = chatNotificationService.getNumNotificationsByChannel(channel);
                numTradeNotification.set(numNotifications > 0 ?
                        String.valueOf(numNotifications) :
                        "");
            });
        }

    }
}
