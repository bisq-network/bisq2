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
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.RichTableView;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.i18n.Res;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.util.Comparator;

public class BisqEasyHistoryView extends View<VBox, BisqEasyHistoryModel, BisqEasyHistoryController> {
    private static final double SIDE_PADDING = 40;

    private final RichTableView<BisqEasyTradeHistoryListItem> bisqEasyTradeHistoryListView;

    public BisqEasyHistoryView(BisqEasyHistoryModel model, BisqEasyHistoryController controller) {
        super(new VBox(), model, controller);

        bisqEasyTradeHistoryListView = new RichTableView<>(
                model.getSortedBisqEasyTradeHistoryListItems(),
                Res.get("bisqEasy.history.headline"),
                Res.get("bisqEasy.history.numTrades"),
                controller::applySearchPredicate);
        bisqEasyTradeHistoryListView.getStyleClass().add("bisq-easy-history-table");
        configTableView();

        root.setPadding(new Insets(0, SIDE_PADDING, 0, SIDE_PADDING));
        root.getChildren().addAll(bisqEasyTradeHistoryListView);
    }

    @Override
    protected void onViewAttached() {
        bisqEasyTradeHistoryListView.initialize();
    }

    @Override
    protected void onViewDetached() {
        bisqEasyTradeHistoryListView.dispose();
    }

    private void configTableView() {
        bisqEasyTradeHistoryListView.getColumns().add(bisqEasyTradeHistoryListView.getSelectionMarkerColumn());

        bisqEasyTradeHistoryListView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.history.table.market"))
                .left()
                .fixWidth(81)
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getMarket))
                .setCellFactory(getMarketCellFactory())
                .includeForCsv(false)
                .build());

        bisqEasyTradeHistoryListView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.history.table.tradeId"))
                .minWidth(80)
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getTradeId))
                .valueSupplier(BisqEasyTradeHistoryListItem::getShortTradeId)
                .tooltipSupplier(BisqEasyTradeHistoryListItem::getTradeId)
                .build());

        bisqEasyTradeHistoryListView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.history.table.myProfile"))
                .left()
                .minWidth(140)
                .comparator(Comparator.comparing(item -> item.getMyUserProfile().getNickName()))
                .setCellFactory(getUserProfileCellFactory(true))
                .includeForCsv(false)
                .build());

        bisqEasyTradeHistoryListView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.history.table.peerProfile"))
                .left()
                .minWidth(140)
                .comparator(Comparator.comparing(item -> item.getPeerProfile().getNickName()))
                .setCellFactory(getUserProfileCellFactory(false))
                .includeForCsv(false)
                .build());

        BisqTableColumn<BisqEasyTradeHistoryListItem> dateColumn = new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.history.table.date"))
                .left()
                .minWidth(160)
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getDate))
                .valueSupplier(BisqEasyTradeHistoryListItem::getDateString)
                .sortType(TableColumn.SortType.DESCENDING)
                .build();
        bisqEasyTradeHistoryListView.getColumns().add(dateColumn);
        bisqEasyTradeHistoryListView.getSortOrder().add(dateColumn);

        bisqEasyTradeHistoryListView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.history.table.baseAmount"))
                .left()
                .minWidth(100)
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getBaseAmount))
                .setCellFactory(getBaseAmountCellFactory(true))
                .includeForCsv(false)
                .build());

        bisqEasyTradeHistoryListView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.history.table.quoteAmount"))
                .left()
                .minWidth(100)
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getQuoteAmount))
                .valueSupplier(BisqEasyTradeHistoryListItem::getQuoteAmountWithSymbol)
                .build());

        bisqEasyTradeHistoryListView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.history.table.price"))
                .left()
                .fixWidth(220)
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getPrice))
                .setCellFactory(getPriceCellFactory())
                .includeForCsv(false)
                .build());

        bisqEasyTradeHistoryListView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.history.table.payment"))
                .left()
                .fixWidth(100)
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getPaymentAsString))
                .setCellFactory(getPaymentCellFactory())
                .includeForCsv(false)
                .build());

        bisqEasyTradeHistoryListView.getColumns().add(new BisqTableColumn.Builder<BisqEasyTradeHistoryListItem>()
                .title(Res.get("bisqEasy.history.table.myRole"))
                .left()
                .minWidth(140)
                .comparator(Comparator.comparing(BisqEasyTradeHistoryListItem::getMyRole))
                .valueSupplier(BisqEasyTradeHistoryListItem::getMyRole)
                .tooltipSupplier(BisqEasyTradeHistoryListItem::getMyRole)
                .build());
    }

    public static Callback<TableColumn<BisqEasyTradeHistoryListItem, BisqEasyTradeHistoryListItem>,
            TableCell<BisqEasyTradeHistoryListItem, BisqEasyTradeHistoryListItem>> getMarketCellFactory() {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(BisqEasyTradeHistoryListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    StackPane tradePairImage = MarketImageComposition.getMarketPairIcons(item.getMarket().getBaseCurrencyCode(),
                            item.getMarket().getQuoteCurrencyCode());
                    setGraphic(tradePairImage);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    public static Callback<TableColumn<BisqEasyTradeHistoryListItem, BisqEasyTradeHistoryListItem>,
            TableCell<BisqEasyTradeHistoryListItem, BisqEasyTradeHistoryListItem>> getUserProfileCellFactory(boolean isMyUserProfile) {
        return column -> new TableCell<>() {
            private UserProfileDisplay userProfileDisplay;

            @Override
            protected void updateItem(BisqEasyTradeHistoryListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    UserProfile userProfile = isMyUserProfile ? item.getMyUserProfile() : item.getPeerProfile();
                    userProfileDisplay = new UserProfileDisplay(userProfile, true, true);
                    ReputationScore reputationScore = isMyUserProfile ? item.getMyReputationScore() : item.getPeerReputationScore();
                    userProfileDisplay.setReputationScore(reputationScore);
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
                hbox.setAlignment(Pos.CENTER_LEFT);
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
                priceIconLabel.setGraphic(ImageUtil.getImageViewById(priceIconId));
                if (hasFixPrice) {
                    HBox.setMargin(priceIconLabel, new Insets(0));
                } else {
                    HBox.setMargin(priceIconLabel, new Insets(-2, 0, 2, 0));
                }
            }
        };
    }

    public static Callback<TableColumn<BisqEasyTradeHistoryListItem, BisqEasyTradeHistoryListItem>,
            TableCell<BisqEasyTradeHistoryListItem, BisqEasyTradeHistoryListItem>> getBaseAmountCellFactory(boolean showSymbol) {
        return column -> new TableCell<>() {
            @SuppressWarnings("UnnecessaryUnicodeEscape")
            private static final String DASH_SYMBOL = "\u2013"; // Unicode for "–"

            private final HBox hbox = new HBox(5);
            private final Label dashLabel = new Label(DASH_SYMBOL);

            {
                hbox.setAlignment(Pos.CENTER_LEFT);
                dashLabel.setAlignment(Pos.CENTER);
                dashLabel.setStyle("-fx-text-fill: -fx-mid-text-color;");
            }

            @Override
            protected void updateItem(BisqEasyTradeHistoryListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    hbox.getChildren().clear();
                    setGraphic(new Label(showSymbol
                            ? item.getBaseAmountWithSymbol()
                            : item.getBaseAmountAsString()));
                } else {
                    hbox.getChildren().clear();
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
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(hBox);
                } else {
                    setGraphic(null);
                }
            }
        };
    }
}
