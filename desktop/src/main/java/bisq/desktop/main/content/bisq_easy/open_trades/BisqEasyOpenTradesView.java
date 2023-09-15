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
import bisq.common.data.Triple;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.desktop.common.Layout;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.utils.SceneUtil;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.desktop.main.content.chat.ChatView;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.i18n.Res;
import bisq.presentation.formatters.DateFormatter;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeFormatter;
import bisq.trade.bisq_easy.BisqEasyTradeUtils;
import bisq.user.profile.UserProfile;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;

@Slf4j
public class BisqEasyOpenTradesView extends ChatView {
    private final VBox tradeStateViewRoot, tradeWelcomeViewRoot;
    private VBox tableViewVBox, chatVBox;
    private HBox chatHeaderHBox;
    private Label chatHeadline;
    private BisqTableView<ListItem> tableView;
    private Subscription noOpenTradesPin, selectedTradePin, selectedModelItemPin, peerUserProfileDisplayPin, chatWindowPin;
    private Button toggleChatWindowButton;

    public BisqEasyOpenTradesView(BisqEasyOpenTradesModel model,
                                  BisqEasyOpenTradesController controller,
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

        root.setPadding(new Insets(0, 0, -67, 0));

        VBox.setMargin(tradeWelcomeViewRoot, new Insets(0, 0, 10, 0));
        centerVBox.getChildren().add(0, tradeWelcomeViewRoot);

        VBox.setMargin(tradeStateViewRoot, new Insets(0, 0, 10, 0));
        VBox.setVgrow(tradeStateViewRoot, Priority.ALWAYS);
        centerVBox.getChildren().add(2, tradeStateViewRoot);
    }


    @Override
    protected void configTitleHBox() {
    }

    @Override
    protected void configCenterVBox() {
        addTableBox();
        addChatBox();
    }

    private void addTableBox() {
        tableView = new BisqTableView<>(getModel().getSortedList());
        tableView.getStyleClass().addAll("bisq-easy-open-trades", "hide-horizontal-scrollbar");

        configTableView();

        VBox.setMargin(tableView, new Insets(10, 0, 0, 0));
        Triple<Label, HBox, VBox> triple = BisqEasyViewUtils.getContainer(Res.get("bisqEasy.openTrades.table.headline"), tableView);
        tableViewVBox = triple.getThird();
        VBox.setMargin(tableViewVBox, new Insets(0, 0, 10, 0));
        centerVBox.getChildren().add(tableViewVBox);
    }

    private void addChatBox() {
        chatMessagesComponent.setMinHeight(200);
        chatMessagesComponent.getStyleClass().add("bisq-easy-chat-messages-bg");
        VBox.setVgrow(chatMessagesComponent, Priority.ALWAYS);
        chatMessagesComponent.setPadding(new Insets(0, -30, -15, -30));
        Triple<Label, HBox, VBox> triple = BisqEasyViewUtils.getContainer("", chatMessagesComponent);
        chatHeadline = triple.getFirst();
        chatHeadline.setContentDisplay(ContentDisplay.RIGHT);
        chatHeadline.setGraphicTextGap(10);

        toggleChatWindowButton = new Button();
        toggleChatWindowButton.setGraphicTextGap(10);
        toggleChatWindowButton.getStyleClass().add("outlined-button");
        chatHeaderHBox = triple.getSecond();
        chatHeaderHBox.getChildren().addAll(Spacer.fillHBox(), toggleChatWindowButton);

        chatVBox = triple.getThird();
        VBox.setVgrow(chatVBox, Priority.ALWAYS);
        centerVBox.getChildren().add(chatVBox);
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

        Layout.pinToAnchorPane(containerHBox, 30, 0, 0, 0);
        root.getChildren().add(containerHBox);
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
        chatHeadline.textProperty().bind(model.getChatHeadline());

        selectedModelItemPin = EasyBind.subscribe(model.getSelectedItem(), selected ->
                tableView.getSelectionModel().select(selected));
        selectedTradePin = EasyBind.subscribe(tableView.getSelectionModel().selectedItemProperty(),
                item -> {
                    if (item != null) {
                        getController().onSelectItem(item);
                    }
                });

        noOpenTradesPin = EasyBind.subscribe(model.getNoOpenTrades(),
                noOpenTrades -> {
                    if (noOpenTrades) {
                        tableView.removeListeners();
                        tableView.setPlaceholderText(Res.get("bisqEasy.openTrades.noTrades"));
                        tableView.allowVerticalScrollbar();
                        tableView.setFixHeight(150);
                    } else {
                        tableView.setPlaceholder(null);
                        tableView.adjustHeightToNumRows();
                        tableView.hideVerticalScrollbar();
                    }
                });
        peerUserProfileDisplayPin = EasyBind.subscribe(model.getPeerUserProfileDisplay(),
                peerUserProfileDisplay -> {
                    if (peerUserProfileDisplay != null) {
                        chatHeadline.setGraphic(peerUserProfileDisplay);
                    }
                });

        chatWindowPin = EasyBind.subscribe(model.getChatWindow(),
                chatWindow -> {
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
                        chatWindow.getIcons().add(ImageUtil.getWindowTitleIcon());
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
                        chatWindow.setWidth(800);
                        chatWindow.setHeight(600);

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
                });

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
        chatHeadline.textProperty().unbind();

        selectedModelItemPin.unsubscribe();
        selectedTradePin.unsubscribe();
        noOpenTradesPin.unsubscribe();
        peerUserProfileDisplayPin.unsubscribe();
        chatWindowPin.unsubscribe();

        toggleChatWindowButton.setOnAction(null);
    }

    private void configTableView() {
        tableView.getColumns().add(tableView.getSelectionMarkerColumn());

        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.tradePeer"))
                .minWidth(100)
                .left()
                .comparator(Comparator.comparing(ListItem::getPeersUserName))
                .setCellFactory(getTradePeerCellFactory())
                .build());

        BisqTableColumn<ListItem> column = new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("temporal.date"))
                .minWidth(170)
                .comparator(Comparator.comparing(ListItem::getDate).reversed())
                .valueSupplier(ListItem::getDateString)
                .build();
        tableView.getColumns().add(column);
        tableView.getSortOrder().add(column);

        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.tradeId"))
                .minWidth(80)
                .comparator(Comparator.comparing(ListItem::getTradeId))
                .valueSupplier(ListItem::getShortTradeId)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.quoteAmount"))
                .minWidth(90)
                .comparator(Comparator.comparing(ListItem::getQuoteAmount))
                .valueSupplier(ListItem::getQuoteAmountString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.baseAmount"))
                .minWidth(120)
                .comparator(Comparator.comparing(ListItem::getBaseAmount))
                .valueSupplier(ListItem::getBaseAmountString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.price"))
                .minWidth(130)
                .comparator(Comparator.comparing(ListItem::getPrice))
                .valueSupplier(ListItem::getPriceString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.myRole"))
                .minWidth(110)
                .right()
                .comparator(Comparator.comparing(ListItem::getMyRole))
                .valueSupplier(ListItem::getMyRole)
                .build());
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getTradePeerCellFactory() {
        return column -> new TableCell<>() {

            @Override
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    setGraphic(new UserProfileDisplay(item.getChannel().getPeer()));
                } else {
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
        private final String offerId;
        private final String tradeId;
        private final String shortTradeId;
        private final String peersUserName;
        private final String dateString, market, priceString,
                baseAmountString, quoteAmountString, paymentMethod, myRole;
        private final long date, price, baseAmount, quoteAmount;
        private final UserProfile peersUserProfile;

        public ListItem(BisqEasyOpenTradeChannel channel, BisqEasyTrade trade) {
            this.channel = channel;
            this.trade = trade;

            peersUserProfile = channel.getPeer();
            peersUserName = peersUserProfile.getUserName();
            offerId = channel.getBisqEasyOffer().getId();
            this.tradeId = trade.getId();
            shortTradeId = tradeId.substring(0, 8);

            BisqEasyContract contract = trade.getContract();
            date = trade.getDate();
            dateString = DateFormatter.formatDateTime(trade.getDate());
            market = trade.getOffer().getMarket().toString();
            price = BisqEasyTradeUtils.getPriceQuote(trade).getValue();
            priceString = BisqEasyTradeFormatter.formatPriceWithCode(trade);
            baseAmount = contract.getBaseSideAmount();
            baseAmountString = BisqEasyTradeFormatter.formatBaseSideAmount(trade);
            quoteAmount = contract.getQuoteSideAmount();
            quoteAmountString = BisqEasyTradeFormatter.formatQuoteSideAmountWithCode(trade);
            paymentMethod = contract.getQuoteSidePaymentMethodSpec().getPaymentMethodName();
            myRole = BisqEasyTradeFormatter.getMyRole(trade);
        }
    }
}
