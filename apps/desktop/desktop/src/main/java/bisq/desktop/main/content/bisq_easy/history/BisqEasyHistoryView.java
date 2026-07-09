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

package bisq.desktop.main.content.bisq_easy.history;

import bisq.common.data.Pair;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.BitcoinAmountDisplay;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.DateColumnUtil;
import bisq.desktop.components.table.RichTableView;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BisqEasyHistoryView extends View<VBox, BisqEasyHistoryModel, BisqEasyHistoryController> {
    private static final double SIDE_PADDING = 40;

    private final RichTableView<BisqEasyTradeHistoryListItem> tableView;
    private final Label placeholderLabel = new Label();

    public BisqEasyHistoryView(BisqEasyHistoryModel model, BisqEasyHistoryController controller) {
        super(new VBox(), model, controller);

        tableView = new RichTableView<>(
                model.getSortedBisqEasyTradeHistoryListItems(),
                Res.get("bisqEasy.history.headline"),
                Res.get("bisqEasy.history.numTrades"),
                controller::applySearchPredicate);
        tableView.getStyleClass().add("bisq-easy-history-table");
        tableView.getTableView().setPlaceholder(placeholderLabel);
        configTableView();

        root.setPadding(new Insets(0, SIDE_PADDING, 0, SIDE_PADDING));
        root.getChildren().addAll(tableView);
    }

    @Override
    protected void onViewAttached() {
        placeholderLabel.textProperty().bind(model.getPlaceholderText());
        tableView.initialize();

        List<String> csvHeaders = tableView.buildCsvHeaders();
        csvHeaders.add(Res.get("bisqEasy.history.table.tradeId").toUpperCase());
        csvHeaders.add(Res.get("bisqEasy.history.table.csv.date").toUpperCase());
        csvHeaders.add(Res.get("bisqEasy.history.table.myRole").toUpperCase());
        csvHeaders.add(Res.get("bisqEasy.history.table.csv.offerType").toUpperCase());
        csvHeaders.add(Res.get("bisqEasy.history.table.csv.quoteAmount").toUpperCase());
        csvHeaders.add(Res.get("bisqEasy.history.table.csv.quoteAmountCurrencyCode").toUpperCase());
        csvHeaders.add(Res.get("bisqEasy.history.table.baseAmount").toUpperCase());
        csvHeaders.add(Res.get("bisqEasy.history.table.csv.tradePrice").toUpperCase());
        csvHeaders.add(Res.get("bisqEasy.history.table.market").toUpperCase());
        csvHeaders.add(Res.get("bisqEasy.history.table.csv.pricePercentage").toUpperCase());
        csvHeaders.add(Res.get("bisqEasy.history.table.csv.priceModality").toUpperCase());
        csvHeaders.add(Res.get("bisqEasy.history.table.csv.payment").toUpperCase());
        csvHeaders.add(Res.get("bisqEasy.history.table.csv.receiverAddressOrInvoice").toUpperCase());
        csvHeaders.add(Res.get("bisqEasy.history.table.csv.txIdOrPreimage").toUpperCase());
        tableView.setCsvHeaders(Optional.of(csvHeaders));

        List<List<String>> csvData = tableView.getItems().stream()
                .map(item -> {
                    List<String> cellDataInRow = tableView.getBisqTableColumnsForCsv()
                            .map(bisqTableColumn -> bisqTableColumn.resolveValueForCsv(item))
                            .collect(Collectors.toList());

                    // Add trade id
                    cellDataInRow.add(item.getTradeId());

                    // Add date
                    cellDataInRow.add(item.getDateTimeString());

                    // Add my role
                    cellDataInRow.add(item.getMyRole());

                    // Add offer type
                    cellDataInRow.add(item.getOfferType());

                    // Add quote amount
                    cellDataInRow.add(item.getQuoteAmountString());

                    // Add quote currency code
                    cellDataInRow.add(item.getMarket().getQuoteCurrencyCode());

                    // Add base amount
                    cellDataInRow.add(item.getBaseAmountString());

                    // Add price
                    cellDataInRow.add(item.getPriceString());

                    // Add market
                    cellDataInRow.add(item.getMarket().getMarketCodes());

                    // Add price percentage
                    cellDataInRow.add(item.getPricePercentage());

                    // Add price modality
                    cellDataInRow.add(item.getPriceModality());

                    // Add payment methods
                    cellDataInRow.add(item.getPaymentMethodAsString());

                    // Add receiver address or invoice
                    cellDataInRow.add(item.getReceiverAddressOrInvoice());

                    // Add transaction ID or preimage
                    cellDataInRow.add(item.getTxIdOrPreimage());

                    return cellDataInRow;
                })
                .collect(Collectors.toList());
        tableView.setCsvData(Optional.of(csvData));
    }

    @Override
    protected void onViewDetached() {
        placeholderLabel.textProperty().unbind();
        tableView.dispose();
    }

    private void configTableView() {
        tableView.getColumns().add(tableView.getSelectionMarkerColumn());

        tableView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.history.table.market"))
                .fixWidth(81)
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getMarket))
                .setCellFactory(getMarketCellFactory())
                .includeForCsv(false)
                .build());

        tableView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.openTrades.table.me"))
                .fixWidth(45)
                .left()
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getMyUserName))
                .setCellFactory(getMyUserCellFactory())
                .includeForCsv(false)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .minWidth(95)
                .left()
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getDirectionalTitle))
                .valueSupplier(BisqEasyTradeHistoryListItem::getDirectionalTitle)
                .includeForCsv(false)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.openTrades.table.tradePeer"))
                .minWidth(120)
                .left()
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getPeersUserName))
                .setCellFactory(getTradePeerCellFactory())
                .includeForCsv(false)
                .build());

        BisqTableColumn<BisqEasyTradeHistoryListItem> dateColumn = new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.history.table.date"))
                .fixWidth(85)
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getDate))
                .sortType(TableColumn.SortType.DESCENDING)
                .setCellFactory(DateColumnUtil.getCellFactory())
                .includeForCsv(false)
                .build();
        tableView.getColumns().add(dateColumn);
        tableView.getSortOrder().add(dateColumn);

        tableView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.history.table.tradeId"))
                .minWidth(85)
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getTradeId))
                .valueSupplier(BisqEasyTradeHistoryListItem::getShortTradeId)
                .tooltipSupplier(BisqEasyTradeHistoryListItem::getTradeId)
                .includeForCsv(false)
                .build());

        tableView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.history.table.quoteAmount"))
                .fixWidth(120)
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getQuoteAmount))
                .valueSupplier(BisqEasyTradeHistoryListItem::getQuoteAmountWithCodeString)
                .includeForCsv(false)
                .build());

        tableView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.history.table.baseAmount"))
                .fixWidth(120)
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getBaseAmount))
                .setCellFactory(getBaseAmountCellFactory())
                .includeForCsv(false)
                .build());

        tableView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.history.table.price"))
                .minWidth(260)
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getPrice))
                .setCellFactory(getPriceCellFactory())
                .includeForCsv(false)
                .build());

        tableView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.history.table.payment"))
                .minWidth(100)
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getPaymentMethodAsString))
                .setCellFactory(getPaymentCellFactory())
                .includeForCsv(false)
                .build());

        tableView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.history.table.myRole"))
                .minWidth(85)
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getMyRole))
                .valueSupplier(BisqEasyTradeHistoryListItem::getMyRole)
                .includeForCsv(false)
                .build());

        tableView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .setCellFactory(getActionButtonsCellFactory())
                .left()
                .minWidth(150)
                .includeForCsv(false)
                .build());
    }

    private Callback<TableColumn<BisqEasyTradeHistoryListItem, BisqEasyTradeHistoryListItem>,
            TableCell<BisqEasyTradeHistoryListItem, BisqEasyTradeHistoryListItem>> getMarketCellFactory() {
        return column -> new TableCell<>() {
            private final Label marketPairIcons = new Label();
            private final Tooltip tooltip = new BisqTooltip();

            @Override
            protected void updateItem(BisqEasyTradeHistoryListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    marketPairIcons.setGraphic(MarketImageComposition.getMarketMenuPairIcons(item.getMarket().getBaseCurrencyCode(),
                            item.getMarket().getQuoteCurrencyCode()));
                    tooltip.setText(item.getMarket().getMarketCodes());
                    Tooltip.install(marketPairIcons, tooltip);
                    setGraphic(marketPairIcons);
                } else {
                    Tooltip.uninstall(marketPairIcons, tooltip);
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<BisqEasyTradeHistoryListItem, BisqEasyTradeHistoryListItem>,
            TableCell<BisqEasyTradeHistoryListItem, BisqEasyTradeHistoryListItem>> getMyUserCellFactory() {
        return column -> new TableCell<>() {
            private final UserProfileIcon userProfileIcon = new UserProfileIcon();

            @Override
            protected void updateItem(BisqEasyTradeHistoryListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    userProfileIcon.setUserProfile(item.getMyUserProfile(), true, false);
                    setGraphic(userProfileIcon);
                } else {
                    userProfileIcon.dispose();
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<BisqEasyTradeHistoryListItem, BisqEasyTradeHistoryListItem>, TableCell<BisqEasyTradeHistoryListItem, BisqEasyTradeHistoryListItem>> getTradePeerCellFactory() {
        return column -> new TableCell<>() {
            private UserProfileDisplay userProfileDisplay;

            @Override
            protected void updateItem(BisqEasyTradeHistoryListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    if (userProfileDisplay != null) {
                        userProfileDisplay.dispose();
                    }
                    userProfileDisplay = new UserProfileDisplay(item.getPeersUserProfile(), true);
                    userProfileDisplay.setReputationScore(item.getPeersReputationScore());
                    userProfileDisplay.hideIconTooltip();

                    setGraphic(userProfileDisplay);
                } else {
                    if (userProfileDisplay != null) {
                        userProfileDisplay.dispose();
                        userProfileDisplay = null;
                    }
                    setGraphic(null);
                }
            }
        };
    }

    public static Callback<TableColumn<BisqEasyTradeHistoryListItem, BisqEasyTradeHistoryListItem>,
            TableCell<BisqEasyTradeHistoryListItem, BisqEasyTradeHistoryListItem>> getPriceCellFactory() {
        return column -> new TableCell<>() {
            private final HBox hbox = new HBox(7);
            private final BisqTooltip tooltip = new BisqTooltip();
            private final Label priceIconLabel = new Label();

            {
                hbox.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(BisqEasyTradeHistoryListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    hbox.getChildren().clear();

                    Pair<String, String> pricePair = item.getPricePair();
                    Label price = new Label(pricePair.getFirst());
                    setupPriceIconLabel(item.isHasFixPrice());
                    Label pricePercentage = new Label(pricePair.getSecond());
                    hbox.getChildren().addAll(price, priceIconLabel, pricePercentage);

                    tooltip.setText(item.getPriceTooltip());
                    Tooltip.install(hbox, tooltip);
                    setGraphic(hbox);
                } else {
                    Tooltip.uninstall(hbox, tooltip);
                    hbox.getChildren().clear();
                    setGraphic(null);
                }
            }

            private void setupPriceIconLabel(boolean hasFixPrice) {
                String priceIconId = hasFixPrice ? "lock-icon-grey" : "chart-icon-grey";
                ImageView icon = ImageUtil.getImageViewById(priceIconId);
                icon.setScaleX(0.75);
                icon.setScaleY(0.75);
                priceIconLabel.setGraphic(icon);
            }
        };
    }

    private Callback<TableColumn<BisqEasyTradeHistoryListItem, BisqEasyTradeHistoryListItem>, TableCell<BisqEasyTradeHistoryListItem, BisqEasyTradeHistoryListItem>> getBaseAmountCellFactory() {
        return column -> new TableCell<>() {
            private final BitcoinAmountDisplay bitcoinAmountDisplay = new BitcoinAmountDisplay("0", false);

            {
                bitcoinAmountDisplay.getSignificantDigits().getStyleClass().add("bisq-easy-open-trades-bitcoin-amount-display");
                bitcoinAmountDisplay.getLeadingZeros().getStyleClass().add("bisq-easy-open-trades-bitcoin-amount-display");
                bitcoinAmountDisplay.getIntegerPart().getStyleClass().add("bisq-easy-open-trades-bitcoin-amount-display");
                bitcoinAmountDisplay.setTranslateY(5);
            }

            @Override
            protected void updateItem(BisqEasyTradeHistoryListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    bitcoinAmountDisplay.applySmallCompactConfig();
                    bitcoinAmountDisplay.setBtcAmount(item.getBaseAmountString());
                    setGraphic(bitcoinAmountDisplay);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    public static Callback<TableColumn<BisqEasyTradeHistoryListItem, BisqEasyTradeHistoryListItem>,
            TableCell<BisqEasyTradeHistoryListItem, BisqEasyTradeHistoryListItem>> getPaymentCellFactory() {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(BisqEasyTradeHistoryListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    HBox hBox = BisqEasyViewUtils.getPaymentAndSettlementMethodsBox(item.getPaymentMethod(), item.getSettlementMethod());
                    hBox.setAlignment(Pos.CENTER);
                    setGraphic(hBox);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<BisqEasyTradeHistoryListItem, BisqEasyTradeHistoryListItem>,
            TableCell<BisqEasyTradeHistoryListItem, BisqEasyTradeHistoryListItem>> getActionButtonsCellFactory() {
        return column -> new TableCell<>() {
            private static final double PREF_WIDTH = 120;
            private static final double PREF_HEIGHT = 26;

            private final HBox tradeMainBox = new HBox();
            private final HBox tradeActionsMenuBox = new HBox(5);
            private final BisqMenuItem showTradeDetailsMenuItem = new BisqMenuItem("icon-info-grey", "icon-info-white");
            private final BisqMenuItem contactPeerMenuItem = new BisqMenuItem("private-chat-grey", "private-chat-white");
            private final BisqMenuItem exportTradeDataMenuItem = new BisqMenuItem("download-grey", "download-white");
            private final BisqMenuItem deleteTradeMenuItem = new BisqMenuItem("delete-t-grey", "delete-t-red");
            private final ChangeListener<Boolean> selectedListener = (observable, oldValue, newValue) -> {
                boolean shouldShow = newValue || getTableRow().isHover();
                tradeActionsMenuBox.setVisible(shouldShow);
                tradeActionsMenuBox.setManaged(shouldShow);
            };

            {
                tradeMainBox.setMinWidth(PREF_WIDTH);
                tradeMainBox.setPrefWidth(PREF_WIDTH);
                tradeMainBox.setMaxWidth(PREF_WIDTH);
                tradeMainBox.setMinHeight(PREF_HEIGHT);
                tradeMainBox.setPrefHeight(PREF_HEIGHT);
                tradeMainBox.setMaxHeight(PREF_HEIGHT);
                tradeMainBox.getChildren().addAll(tradeActionsMenuBox);

                tradeActionsMenuBox.setMinWidth(PREF_WIDTH);
                tradeActionsMenuBox.setPrefWidth(PREF_WIDTH);
                tradeActionsMenuBox.setMaxWidth(PREF_WIDTH);
                tradeActionsMenuBox.setMinHeight(PREF_HEIGHT);
                tradeActionsMenuBox.setPrefHeight(PREF_HEIGHT);
                tradeActionsMenuBox.setMaxHeight(PREF_HEIGHT);
                tradeActionsMenuBox.getChildren().addAll(contactPeerMenuItem, exportTradeDataMenuItem,
                        showTradeDetailsMenuItem, deleteTradeMenuItem);
                tradeActionsMenuBox.setAlignment(Pos.CENTER);

                showTradeDetailsMenuItem.useIconOnly();
                showTradeDetailsMenuItem.setTooltip(Res.get("bisqEasy.history.table.actionButtons.showTradeDetails.tooltip"));

                contactPeerMenuItem.useIconOnly();
                contactPeerMenuItem.setTooltip(Res.get("bisqEasy.history.table.actionButtons.contactPeer.tooltip"));

                exportTradeDataMenuItem.useIconOnly();
                exportTradeDataMenuItem.setTooltip(Res.get("bisqEasy.history.table.actionButtons.exportTradeData.tooltip"));

                deleteTradeMenuItem.useIconOnly();
                deleteTradeMenuItem.setTooltip(Res.get("bisqEasy.history.table.actionButtons.deleteArchivedTrade.tooltip"));
            }

            @Override
            protected void updateItem(BisqEasyTradeHistoryListItem item, boolean empty) {
                super.updateItem(item, empty);

                resetRowEventHandlersAndListeners();
                resetVisibilities();

                if (item != null && !empty) {
                    setUpRowEventHandlersAndListeners();
                    setGraphic(tradeMainBox);
                    showTradeDetailsMenuItem.setOnAction(e -> controller.onShowTradeDetails(item));
                    contactPeerMenuItem.setOnAction(e -> controller.onContactPeer(item.getPeersUserProfile()));
                    exportTradeDataMenuItem.setOnAction(e -> controller.onExportTradeData(item.getTrade()));
                    deleteTradeMenuItem.setOnAction(e -> controller.onDeleteTrade(item.getTrade()));
                } else {
                    resetRowEventHandlersAndListeners();
                    resetVisibilities();
                    showTradeDetailsMenuItem.setOnAction(null);
                    contactPeerMenuItem.setOnAction(null);
                    exportTradeDataMenuItem.setOnAction(null);
                    deleteTradeMenuItem.setOnAction(null);
                    setGraphic(null);
                }
            }

            private void setUpRowEventHandlersAndListeners() {
                TableRow<?> row = getTableRow();
                if (row != null) {
                    row.setOnMouseEntered(e -> {
                        boolean shouldShow = row.isSelected() || row.isHover();
                        tradeActionsMenuBox.setVisible(shouldShow);
                        tradeActionsMenuBox.setManaged(shouldShow);
                    });
                    row.setOnMouseExited(e -> {
                        boolean shouldShow = row.isSelected();
                        tradeActionsMenuBox.setVisible(shouldShow);
                        tradeActionsMenuBox.setManaged(shouldShow);
                    });
                    row.selectedProperty().addListener(selectedListener);
                }
            }

            private void resetRowEventHandlersAndListeners() {
                TableRow<?> row = getTableRow();
                if (row != null) {
                    row.setOnMouseEntered(null);
                    row.setOnMouseExited(null);
                    row.selectedProperty().removeListener(selectedListener);
                }
            }

            private void resetVisibilities() {
                tradeActionsMenuBox.setVisible(false);
                tradeActionsMenuBox.setManaged(false);
            }
        };
    }
}
