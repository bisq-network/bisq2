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

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.BitcoinAmountDisplay;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.DateColumnUtil;
import bisq.desktop.components.table.RichTableView;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.util.Comparator;

import static bisq.desktop.components.helpers.LabeledValueRowFactory.getValueLabel;

public class BisqEasyHistoryView extends View<VBox, BisqEasyHistoryModel, BisqEasyHistoryController> {
    private static final double SIDE_PADDING = 40;

    private final RichTableView<BisqEasyTradeHistoryListItem> tableView;

    public BisqEasyHistoryView(BisqEasyHistoryModel model, BisqEasyHistoryController controller) {
        super(new VBox(), model, controller);

        tableView = new RichTableView<>(
                model.getSortedBisqEasyTradeHistoryListItems(),
                Res.get("bisqEasy.history.headline"),
                Res.get("bisqEasy.history.numTrades"),
                controller::applySearchPredicate);
        tableView.getStyleClass().add("bisq-easy-history-table");
        configTableView();

        root.setPadding(new Insets(0, SIDE_PADDING, 0, SIDE_PADDING));
        root.getChildren().addAll(tableView);
    }

    @Override
    protected void onViewAttached() {
        tableView.initialize();
    }

    @Override
    protected void onViewDetached() {
        tableView.dispose();
    }

    private void configTableView() {
        tableView.getColumns().add(tableView.getSelectionMarkerColumn());

        tableView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.openTrades.table.me"))
                .fixWidth(45)
                .left()
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getMyUserName))
                .setCellFactory(getMyUserCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .minWidth(95)
                .left()
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getDirectionalTitle))
                .valueSupplier(BisqEasyTradeHistoryListItem::getDirectionalTitle)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.openTrades.table.tradePeer"))
                .minWidth(110)
                .left()
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getPeersUserName))
                .setCellFactory(getTradePeerCellFactory())
                .build());

        tableView.getColumns().add(DateColumnUtil.getDateColumn(tableView.getSortOrder()));

        tableView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.history.table.tradeId"))
                .minWidth(80)
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getTradeId))
                .valueSupplier(BisqEasyTradeHistoryListItem::getShortTradeId)
                .tooltipSupplier(BisqEasyTradeHistoryListItem::getTradeId)
                .build());

        tableView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.openTrades.table.quoteAmount"))
                .right()
                .fixWidth(120)
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getQuoteAmount))
                .valueSupplier(BisqEasyTradeHistoryListItem::getQuoteAmountString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.openTrades.table.baseAmount"))
                .right()
                .fixWidth(120)
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getBaseAmount))
                .setCellFactory(getBaseAmountCellFactory())
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.history.table.price"))
                .left()
                .fixWidth(220)
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getPrice))
                .setCellFactory(getPriceCellFactory())
                .includeForCsv(false)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.history.table.payment"))
                .fixWidth(100)
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getPaymentAsString))
                .setCellFactory(getPaymentCellFactory())
                .includeForCsv(false)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.history.table.myRole"))
                .right()
                .minWidth(60)
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getMyRole))
                .valueSupplier(BisqEasyTradeHistoryListItem::getMyRole)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .setCellFactory(getActionButtonsCellFactory())
                .minWidth(40)
                .includeForCsv(false)
                .build());
    }

    private Callback<TableColumn<BisqEasyTradeHistoryListItem, BisqEasyTradeHistoryListItem>, TableCell<BisqEasyTradeHistoryListItem, BisqEasyTradeHistoryListItem>> getMyUserCellFactory() {
        return column -> new TableCell<>() {

            private final UserProfileIcon userProfileIcon = new UserProfileIcon();
            private final StackPane stackPane = new StackPane(userProfileIcon);

            @Override
            protected void updateItem(BisqEasyTradeHistoryListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    userProfileIcon.setUserProfile(item.getMyUserProfile(), true);
                    // Tooltip is not working if we add directly to the cell therefor we wrap into a StackPane
                    setGraphic(stackPane);
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
                    userProfileDisplay = new UserProfileDisplay(item.getPeersUserProfile(), true, true);
                    userProfileDisplay.setReputationScore(item.getPeerReputationScore());
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
            TableCell<BisqEasyTradeHistoryListItem, BisqEasyTradeHistoryListItem>> getPriceCellFactory() {
        return column -> new TableCell<>() {
            private final HBox hBox;
            private final Label priceSpec, codes, price, icon;
            private final BisqTooltip tooltip = new BisqTooltip();

            {
                price = getValueLabel();
                codes = new Label();
                icon = new Label();
                codes.getStyleClass().addAll("text-fill-white", "small-text");
                priceSpec = new Label();
                // priceSpecLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
                codes.setStyle("-fx-text-fill: -fx-mid-text-color");
                HBox.setMargin(codes, new Insets(5, 0, 0, 0));
                hBox = new HBox(5, price, codes, icon, priceSpec);
                hBox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(BisqEasyTradeHistoryListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    price.setText(item.getPriceString());
                    codes.setText(item.getPriceCodes());
                    priceSpec.setText(item.getPriceSpecString());
                    setupPriceIconLabel(item.isFixPrice());
                    tooltip.setText(item.getPriceTooltip());
                    Tooltip.install(hBox, tooltip);
                    setGraphic(hBox);
                } else {
                    Tooltip.uninstall(hBox, tooltip);
                    setGraphic(null);
                }
            }

            private void setupPriceIconLabel(boolean hasFixPrice) {
                String priceIconId = hasFixPrice ? "lock-icon-grey" : "chart-icon-grey";
                icon.setGraphic(ImageUtil.getImageViewById(priceIconId));
                if (hasFixPrice) {
                    HBox.setMargin(icon, new Insets(0));
                } else {
                    HBox.setMargin(icon, new Insets(-2, 0, 2, 0));
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
                    hBox.setAlignment(Pos.CENTER_LEFT);
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
            private static final double PREF_WIDTH = 30;
            private static final double PREF_HEIGHT = 26;

            private final HBox tradeMainBox = new HBox();
            private final HBox tradeActionsMenuBox = new HBox(5);
            private final BisqMenuItem showTradeDetailsMenuItem = new BisqMenuItem("icon-info-grey", "icon-info-white");
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
                tradeActionsMenuBox.getChildren().addAll(showTradeDetailsMenuItem);
                tradeActionsMenuBox.setAlignment(Pos.CENTER);

                showTradeDetailsMenuItem.useIconOnly();
                showTradeDetailsMenuItem.setTooltip(Res.get("bisqEasy.history.table.actionButtons.showTradeDetails.tooltip"));
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
                } else {
                    resetRowEventHandlersAndListeners();
                    resetVisibilities();
                    showTradeDetailsMenuItem.setOnAction(null);
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
