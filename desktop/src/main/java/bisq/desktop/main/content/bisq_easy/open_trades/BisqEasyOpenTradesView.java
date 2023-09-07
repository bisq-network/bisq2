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

import bisq.chat.bisqeasy.open_trade.BisqEasyOpenTradeChannel;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.desktop.common.Layout;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.desktop.main.content.chat.ChatView;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.presentation.formatters.DateFormatter;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeFormatter;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.trade.bisq_easy.BisqEasyTradeUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;

@Slf4j
public class BisqEasyOpenTradesView extends ChatView {
    private BisqTableView<ListItem> tableView;
    private Subscription selectedTradePin;

    public BisqEasyOpenTradesView(BisqEasyOpenTradesModel model,
                                  BisqEasyOpenTradesController controller,
                                  VBox chatMessagesComponent,
                                  Pane channelSidebar,
                                  VBox tradeStateViewRoot) {
        super(model,
                controller,
                chatMessagesComponent,
                channelSidebar);


        root.setPadding(new Insets(0, 0, -67, 0));
    }

    @Override
    protected void configTitleHBox() {
        titleHBox.setAlignment(Pos.CENTER);
        titleHBox.setPadding(new Insets(12.5, 25, 12.5, 25));
        titleHBox.getStyleClass().add("bisq-easy-chat-title-bg");
        titleHBox.setCursor(Cursor.HAND);

        channelTitle.setId("chat-messages-headline");

        double scale = 1.15;
        helpButton = BisqIconButton.createIconButton("icon-help");
        helpButton.setScaleX(scale);
        helpButton.setScaleY(scale);
        infoButton = BisqIconButton.createIconButton("icon-info");
        infoButton.setScaleX(scale);
        infoButton.setScaleY(scale);

        HBox.setMargin(channelTitle, new Insets(0, 0, 0, 4));
        HBox.setMargin(helpButton, new Insets(-2, 0, 0, 0));
        HBox.setMargin(infoButton, new Insets(-2, 0, 0, 0));
        titleHBox.getChildren().addAll(
                channelTitle,
                Spacer.fillHBox(),
                helpButton, infoButton
        );
    }

    @Override
    protected void configCenterVBox() {
        tableView = new BisqTableView<>(getBisqEasyOpenTradesModel().getSortedList());
        tableView.setMinHeight(150);
        tableView.setPrefHeight(250);
        tableView.getStyleClass().add("bisq-easy-open-trades-table-view");
        configTableView();

        centerVBox.setSpacing(0);
        centerVBox.setFillWidth(true);

        VBox topPanelVBox = new VBox(titleHBox); //todo
        topPanelVBox.getStyleClass().add("bisq-easy-chat-tools-bg");

        chatMessagesComponent.setMinWidth(700);
        chatMessagesComponent.getStyleClass().add("bisq-easy-chat-messages-bg");

        VBox.setMargin(tableView, new Insets(0, 0, 10, 0));
        VBox.setVgrow(chatMessagesComponent, Priority.ALWAYS);
        centerVBox.getChildren().addAll(
                tableView,
                titleHBox,
                Layout.hLine(),
                chatMessagesComponent);
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

        selectedTradePin = EasyBind.subscribe(tableView.getSelectionModel().selectedItemProperty(),
                item -> {
                    if (item != null) {
                        getBisqEasyOpenTradesController().onSelectTrade(item.getOfferId());
                    }
                });
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        selectedTradePin.unsubscribe();
    }

    private void configTableView() {
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.tradePeer"))
                .minWidth(100)
                .left()
                .comparator(Comparator.comparing(ListItem::getPeersUserName))
                .setCellFactory(getTradePeerCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.tradeId"))
                .minWidth(75)
                .comparator(Comparator.comparing(ListItem::getOfferId))
                .valueSupplier(ListItem::getShortOfferId)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("temporal.date"))
                .minWidth(160)
                .comparator(Comparator.comparing(ListItem::getDate))
                .valueSupplier(ListItem::getDateString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.market"))
                .minWidth(120)
                .comparator(Comparator.comparing(ListItem::getMarket))
                .valueSupplier(ListItem::getMarket)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.price"))
                .minWidth(80)
                .comparator(Comparator.comparing(ListItem::getPrice))
                .valueSupplier(ListItem::getPriceString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.baseAmount"))
                .minWidth(120)
                .comparator(Comparator.comparing(ListItem::getBaseAmount))
                .valueSupplier(ListItem::getBaseAmountString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.quoteAmount"))
                .minWidth(100)
                .comparator(Comparator.comparing(ListItem::getQuoteAmount))
                .valueSupplier(ListItem::getQuoteAmountString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.paymentMethod"))
                .minWidth(130)
                .comparator(Comparator.comparing(ListItem::getPaymentMethod))
                .valueSupplier(ListItem::getPaymentMethod)
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
                    BisqEasyOpenTradeChannel channel = item.getChannel();
                    Label label = new Label(channel.getPeer().getUserName());
                    Image image = RoboHash.getImage(channel.getPeer().getPubKeyHash());
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(35);
                    imageView.setFitHeight(35);
                    label.setGraphic(imageView);
                    label.setGraphicTextGap(10);
                    setGraphic(label);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private BisqEasyOpenTradesModel getBisqEasyOpenTradesModel() {
        return (BisqEasyOpenTradesModel) model;
    }

    private BisqEasyOpenTradesController getBisqEasyOpenTradesController() {
        return (BisqEasyOpenTradesController) controller;
    }

    @Getter
    @EqualsAndHashCode
    static class ListItem implements TableItem {
        private final BisqEasyOpenTradeChannel channel;
        private final String offerId, shortOfferId;
        private final String peersUserName;

        private String dateString, market, priceString,
                baseAmountString, quoteAmountString, paymentMethod, myRole;
        private long date, price, baseAmount, quoteAmount;
        private Pin pin;

        public ListItem(BisqEasyOpenTradeChannel channel, BisqEasyTradeService bisqEasyTradeService) {
            this.channel = channel;

            BisqEasyOffer offer = channel.getBisqEasyOffer();
            offerId = offer.getId();
            shortOfferId = offer.getId().substring(0, 8);
            peersUserName = channel.getPeer().getUserName();

            bisqEasyTradeService.findTrade(offerId)
                    .ifPresentOrElse(this::applyTrade,
                            () -> {
                                pin = bisqEasyTradeService.getTrades().addListener(new CollectionObserver<>() {
                                    @Override
                                    public void add(BisqEasyTrade trade) {
                                        UIThread.runOnNextRenderFrame(() -> {
                                            applyTrade(trade);
                                            pin.unbind();
                                        });
                                    }

                                    @Override
                                    public void remove(Object element) {
                                    }

                                    @Override
                                    public void clear() {
                                    }
                                });
                            });

        }

        private void applyTrade(BisqEasyTrade trade) {
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
