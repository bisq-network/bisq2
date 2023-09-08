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
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.desktop.common.Layout;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.desktop.main.content.chat.ChatView;
import bisq.i18n.Res;
import bisq.presentation.formatters.DateFormatter;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeFormatter;
import bisq.trade.bisq_easy.BisqEasyTradeUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
    private final VBox tradeStateViewRoot;
    private final VBox tradeWelcomeViewRoot;
    private BisqTableView<ListItem> tableView;
    private Subscription noOpenTradesPin, selectedTradePin, selectedItemPin;

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

        VBox.setMargin(tradeWelcomeViewRoot, new Insets(0, 0, 10, 0));
        centerVBox.getChildren().add(0, tradeWelcomeViewRoot);

        VBox.setMargin(tradeStateViewRoot, new Insets(0, 0, 10, 0));
        centerVBox.getChildren().add(2, tradeStateViewRoot);

        root.setPadding(new Insets(0, 0, -67, 0));
    }

    @Override
    protected void configTitleHBox() {
    }

    @Override
    protected void configCenterVBox() {
        tableView = new BisqTableView<>(getOpenTradesModel().getSortedList());
        tableView.getStyleClass().add("bisq-easy-open-trades-table-view");
        configTableView();

        centerVBox.setSpacing(0);
        centerVBox.setFillWidth(true);

        chatMessagesComponent.setMinHeight(200);
        chatMessagesComponent.getStyleClass().add("bisq-easy-chat-messages-bg");

        VBox.setMargin(tableView, new Insets(0, 0, 10, 0));
        VBox.setVgrow(chatMessagesComponent, Priority.ALWAYS);
        centerVBox.getChildren().addAll(tableView, chatMessagesComponent);
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

        BisqEasyOpenTradesModel openTradesModel = getOpenTradesModel();

        tradeWelcomeViewRoot.visibleProperty().bind(openTradesModel.getTradeWelcomeVisible());
        tradeWelcomeViewRoot.managedProperty().bind(openTradesModel.getTradeWelcomeVisible());
        tradeStateViewRoot.visibleProperty().bind(openTradesModel.getTradeStateVisible());
        tradeStateViewRoot.managedProperty().bind(openTradesModel.getTradeStateVisible());
        chatMessagesComponent.visibleProperty().bind(openTradesModel.getChatVisible());
        chatMessagesComponent.managedProperty().bind(openTradesModel.getChatVisible());
        tableView.mouseTransparentProperty().bind(openTradesModel.getTableViewDisabled());

        selectedItemPin = EasyBind.subscribe(openTradesModel.getSelectedItem(), selected ->
                tableView.getSelectionModel().select(selected));
        selectedTradePin = EasyBind.subscribe(tableView.getSelectionModel().selectedItemProperty(),
                item -> {
                    if (item != null) {
                        getBisqEasyOpenTradesController().onSelectTrade(item.getOfferId());
                    }
                });

        noOpenTradesPin = EasyBind.subscribe(openTradesModel.getNoOpenTrades(), noOpenTrades -> {
            log.error("noOpenTrades " + noOpenTrades);
            if (noOpenTrades) {
                tableView.removeListeners();
                tableView.setPlaceholderText(Res.get("bisqEasy.openTrades.noData"));
                tableView.allowVerticalScrollbar();
                tableView.setFixHeight(150);
            } else {
                tableView.setPlaceholder(null);
                tableView.adjustHeightToNumRows();
                tableView.hideVerticalScrollbar();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        tradeWelcomeViewRoot.visibleProperty().unbind();
        tradeWelcomeViewRoot.managedProperty().unbind();
        tradeStateViewRoot.visibleProperty().unbind();
        tradeStateViewRoot.managedProperty().unbind();
        chatMessagesComponent.visibleProperty().unbind();
        chatMessagesComponent.managedProperty().unbind();

        selectedItemPin.unsubscribe();
        selectedTradePin.unsubscribe();

        tableView.removeListeners();
    }

    private void configTableView() {
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
      
       /* tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.market"))
                .minWidth(120)
                .comparator(Comparator.comparing(ListItem::getMarket))
                .valueSupplier(ListItem::getMarket)
                .build());*/
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.price"))
                .minWidth(130)
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
                .minWidth(90)
                .comparator(Comparator.comparing(ListItem::getQuoteAmount))
                .valueSupplier(ListItem::getQuoteAmountString)
                .build());
       /* tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.paymentMethod"))
                .minWidth(130)
                .comparator(Comparator.comparing(ListItem::getPaymentMethod))
                .valueSupplier(ListItem::getPaymentMethod)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.openTrades.table.myRole"))
                .minWidth(110)
                .comparator(Comparator.comparing(ListItem::getMyRole))
                .valueSupplier(ListItem::getMyRole)
                .build());*/
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getTradePeerCellFactory() {
        return column -> new TableCell<>() {

            @Override
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    BisqEasyOpenTradeChannel channel = item.getChannel();
                    Image image = RoboHash.getImage(channel.getPeer().getPubKeyHash());
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(35);
                    imageView.setFitHeight(35);
                    Label label = new Label(channel.getPeer().getUserName(), imageView);
                    label.setGraphicTextGap(10);
                    setGraphic(label);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private BisqEasyOpenTradesModel getOpenTradesModel() {
        return (BisqEasyOpenTradesModel) model;
    }

    private BisqEasyOpenTradesController getBisqEasyOpenTradesController() {
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

        public ListItem(BisqEasyOpenTradeChannel channel, BisqEasyTrade trade) {
            this.channel = channel;
            this.trade = trade;

            peersUserName = channel.getPeer().getUserName();
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
