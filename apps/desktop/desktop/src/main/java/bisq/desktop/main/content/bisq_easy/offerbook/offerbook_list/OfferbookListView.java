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

package bisq.desktop.main.content.bisq_easy.offerbook.offerbook_list;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.PaymentMethod;
import bisq.desktop.common.Layout;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.DropdownMenu;
import bisq.desktop.components.controls.DropdownMenuItem;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.desktop.main.content.bisq_easy.offerbook.BisqEasyOfferbookView;
import bisq.desktop.main.content.chat.BaseChatView;
import bisq.desktop.main.content.components.ReputationScoreDisplay;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.i18n.Res;
import com.google.common.base.Joiner;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;

@Slf4j
public class OfferbookListView extends bisq.desktop.common.view.View<VBox, OfferbookListModel, OfferbookListController> {
    private static final double EXPANDED_OFFER_LIST_WIDTH = 545;
    private static final double COLLAPSED_LIST_WIDTH = BisqEasyOfferbookView.COLLAPSED_LIST_WIDTH;
    private static final double HEADER_HEIGHT = BaseChatView.HEADER_HEIGHT;
    private static final double LIST_CELL_HEIGHT = BisqEasyOfferbookView.LIST_CELL_HEIGHT;

    private final Label title, offerListByDirectionFilter;
    private final BisqTableView<OfferbookListItem> tableView;
    private final BisqTooltip titleTooltip;
    private final HBox header;
    private final ImageView offerListWhiteIcon, offerListGreyIcon, offerListGreenIcon;
    private final DropdownMenu filterDropdownMenu;
    private final DropdownMenuItem buyFromOffers, sellToOffers;
    private Subscription showOfferListExpandedPin, showBuyFromOffersPin, offerListTableViewSelectionPin;

    OfferbookListView(OfferbookListModel model, OfferbookListController controller) {
        super(new VBox(), model, controller);

        root.setFillWidth(true);

        offerListGreenIcon = ImageUtil.getImageViewById("list-view-green");
        offerListGreyIcon = ImageUtil.getImageViewById("list-view-grey");
        offerListWhiteIcon = ImageUtil.getImageViewById("list-view-white");

        title = new Label("", offerListGreenIcon);
        title.setCursor(Cursor.HAND);

        titleTooltip = new BisqTooltip();

        header = new HBox(title);
        header.setMinHeight(HEADER_HEIGHT);
        header.setMaxHeight(HEADER_HEIGHT);
        header.getStyleClass().add("chat-header-title");

        filterDropdownMenu = new DropdownMenu("chevron-drop-menu-grey", "chevron-drop-menu-white", false);
        filterDropdownMenu.getStyleClass().add("dropdown-offer-list-direction-filter-menu");
        offerListByDirectionFilter = new Label();
        filterDropdownMenu.setLabel(offerListByDirectionFilter);
        buyFromOffers = new DropdownMenuItem(Res.get("bisqEasy.offerbook.offerList.table.filters.offerDirection.buyFrom"));
        sellToOffers = new DropdownMenuItem(Res.get("bisqEasy.offerbook.offerList.table.filters.offerDirection.sellTo"));
        filterDropdownMenu.addMenuItems(buyFromOffers, sellToOffers);

        HBox subheader = new HBox();
        subheader.setAlignment(Pos.CENTER_LEFT);
        subheader.getStyleClass().add("offer-list-subheader");
        subheader.getChildren().add(filterDropdownMenu);

        tableView = new BisqTableView<>(model.getSortedOfferbookListItems());
        tableView.getStyleClass().add("offers-list");
        tableView.allowVerticalScrollbar();
        tableView.hideHorizontalScrollbar();
        tableView.setFixedCellSize(LIST_CELL_HEIGHT);
        tableView.setPlaceholder(new Label());
        configOffersTableView();
        VBox.setVgrow(tableView, Priority.ALWAYS);

        root.getChildren().addAll(header, Layout.hLine(), subheader, tableView);
    }

    @Override
    protected void onViewAttached() {
        showOfferListExpandedPin = EasyBind.subscribe(model.getShowOfferListExpanded(), showOfferListExpanded -> {
            if (showOfferListExpanded != null) {
                tableView.setVisible(showOfferListExpanded);
                tableView.setManaged(showOfferListExpanded);
                filterDropdownMenu.setVisible(showOfferListExpanded);
                filterDropdownMenu.setManaged(showOfferListExpanded);
                title.setGraphic(offerListGreyIcon);
                if (showOfferListExpanded) {
                    header.setAlignment(Pos.CENTER_LEFT);
                    header.setPadding(new Insets(4, 0, 0, 15));
                    root.setMaxWidth(EXPANDED_OFFER_LIST_WIDTH);
                    root.setPrefWidth(EXPANDED_OFFER_LIST_WIDTH);
                    root.setMinWidth(EXPANDED_OFFER_LIST_WIDTH);
                    HBox.setMargin(root, new Insets(0, 0, 0, 0));
                    root.getStyleClass().remove("collapsed-offer-list-container");
                    root.getStyleClass().add("chat-container");
                    title.setText(Res.get("bisqEasy.offerbook.offerList"));
                    titleTooltip.setText(Res.get("bisqEasy.offerbook.offerList.expandedList.tooltip"));
                    Transitions.expansionAnimation(root, COLLAPSED_LIST_WIDTH + 20, EXPANDED_OFFER_LIST_WIDTH);
                    title.setOnMouseExited(e -> title.setGraphic(offerListGreenIcon));
                } else {
                    Transitions.expansionAnimation(root, EXPANDED_OFFER_LIST_WIDTH, COLLAPSED_LIST_WIDTH + 20, () -> {
                        header.setAlignment(Pos.CENTER);
                        header.setPadding(new Insets(4, 0, 0, 0));
                        root.setMaxWidth(COLLAPSED_LIST_WIDTH);
                        root.setPrefWidth(COLLAPSED_LIST_WIDTH);
                        root.setMinWidth(COLLAPSED_LIST_WIDTH);
                        HBox.setMargin(root, new Insets(0, 0, 0, -9));
                        root.getStyleClass().remove("chat-container");
                        root.getStyleClass().add("collapsed-offer-list-container");
                        title.setText("");
                        titleTooltip.setText(Res.get("bisqEasy.offerbook.offerList.collapsedList.tooltip"));
                        title.setGraphic(offerListGreyIcon);
                        title.setOnMouseExited(e -> title.setGraphic(offerListGreyIcon));
                    });
                }
            }
        });

        offerListTableViewSelectionPin = EasyBind.subscribe(tableView.getSelectionModel().selectedItemProperty(),
                controller::onSelectOfferMessageItem);

        showBuyFromOffersPin = EasyBind.subscribe(model.getShowBuyOffers(), showBuyFromOffers -> {
            if (showBuyFromOffers != null) {
                offerListByDirectionFilter.getStyleClass().clear();
                if (showBuyFromOffers) {
                    offerListByDirectionFilter.setText(sellToOffers.getLabelText());
                    offerListByDirectionFilter.getStyleClass().add("sell-to-offers");
                } else {
                    offerListByDirectionFilter.setText(buyFromOffers.getLabelText());
                    offerListByDirectionFilter.getStyleClass().add("buy-from-offers");
                }
            }
        });

        title.setOnMouseEntered(e -> title.setGraphic(offerListWhiteIcon));
        title.setOnMouseClicked(e -> controller.toggleOfferList());
        buyFromOffers.setOnAction(e -> controller.onSelectBuyFromFilter());
        sellToOffers.setOnAction(e -> controller.onSelectSellToFilter());

        title.setTooltip(titleTooltip);
    }

    @Override
    protected void onViewDetached() {
        showOfferListExpandedPin.unsubscribe();
        offerListTableViewSelectionPin.unsubscribe();
        showBuyFromOffersPin.unsubscribe();

        title.setOnMouseEntered(null);
        title.setOnMouseExited(null);
        title.setOnMouseClicked(null);
        buyFromOffers.setOnAction(null);
        sellToOffers.setOnAction(null);

        title.setTooltip(null);
    }

    private void configOffersTableView() {
        tableView.getColumns().add(tableView.getSelectionMarkerColumn());

        BisqTableColumn<OfferbookListItem> userProfileColumn = new BisqTableColumn.Builder<OfferbookListItem>()
                .title(Res.get("bisqEasy.offerbook.offerList.table.columns.peerProfile"))
                .left()
                .fixWidth(150)
                .setCellFactory(getUserProfileCellFactory())
                .comparator(Comparator.comparingLong(OfferbookListItem::getTotalScore).reversed())
                .build();
        tableView.getColumns().add(userProfileColumn);
        tableView.getSortOrder().add(userProfileColumn);

        BisqTableColumn<OfferbookListItem> priceColumn = new BisqTableColumn.Builder<OfferbookListItem>()
                .title(Res.get("bisqEasy.offerbook.offerList.table.columns.price"))
                .right()
                .fixWidth(75)
                .setCellFactory(getPriceCellFactory())
                .comparator((o1, o2) -> {
                    if (o1.getBisqEasyOffer().getDirection().isSell()) {
                        return Double.compare(o1.getPriceSpecAsPercent(), o2.getPriceSpecAsPercent());
                    } else {
                        return Double.compare(o2.getPriceSpecAsPercent(), o1.getPriceSpecAsPercent());
                    }
                })
                .build();
        tableView.getColumns().add(priceColumn);
        tableView.getSortOrder().add(priceColumn);

        tableView.getColumns().add(new BisqTableColumn.Builder<OfferbookListItem>()
                .titleProperty(model.getFiatAmountTitle())
                .right()
                .fixWidth(120)
                .setCellFactory(getFiatAmountCellFactory())
                .comparator(Comparator.comparing(OfferbookListItem::getQuoteSideMinAmount))
                .build());

        tableView.getColumns().add(new BisqTableColumn.Builder<OfferbookListItem>()
                .title(Res.get("bisqEasy.offerbook.offerList.table.columns.paymentMethod"))
                .right()
                .fixWidth(105)
                .setCellFactory(getPaymentCellFactory())
                .comparator(Comparator.comparing(OfferbookListItem::getFiatPaymentMethodsAsString))
                .build());

        tableView.getColumns().add(new BisqTableColumn.Builder<OfferbookListItem>()
                .title(Res.get("bisqEasy.offerbook.offerList.table.columns.settlementMethod"))
                .left()
                .fixWidth(95)
                .setCellFactory(getSettlementCellFactory())
                .comparator(Comparator.comparing(OfferbookListItem::getBitcoinPaymentMethodsAsString))
                .build());
    }


    private Callback<TableColumn<OfferbookListItem, OfferbookListItem>,
            TableCell<OfferbookListItem, OfferbookListItem>> getUserProfileCellFactory() {
        return column -> new TableCell<>() {
            private final Label userNameLabel = new Label();
            private final ReputationScoreDisplay reputationScoreDisplay = new ReputationScoreDisplay();
            private final VBox nameAndReputationBox = new VBox(userNameLabel, reputationScoreDisplay);
            private final UserProfileIcon userProfileIcon = new UserProfileIcon();
            private final HBox userProfileBox = new HBox(10, userProfileIcon, nameAndReputationBox);

            {
                userNameLabel.setId("chat-user-name");
                HBox.setMargin(userProfileIcon, new Insets(0, 0, 0, -1));
                nameAndReputationBox.setAlignment(Pos.CENTER_LEFT);
                userProfileBox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(OfferbookListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    userNameLabel.setText(item.getUserNickname());
                    reputationScoreDisplay.setReputationScore(item.getReputationScore());
                    userProfileIcon.setUserProfile(item.getUserProfile());
                    setGraphic(userProfileBox);
                } else {
                    userNameLabel.setText("");
                    reputationScoreDisplay.setReputationScore(null);
                    userProfileIcon.dispose();
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<OfferbookListItem, OfferbookListItem>,
            TableCell<OfferbookListItem, OfferbookListItem>> getPriceCellFactory() {
        return column -> new TableCell<>() {
            private final Label percentagePriceLabel = new Label();
            private final BisqTooltip tooltip = new BisqTooltip();

            @Override
            protected void updateItem(OfferbookListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    percentagePriceLabel.setText(item.getFormattedPercentagePrice());
                    percentagePriceLabel.setStyle(item.isFixPrice() ? "-fx-text-fill: -bisq2-green-lit-20" : "");
                    tooltip.setText(item.getPriceTooltipText());
                    percentagePriceLabel.setTooltip(tooltip);
                    setGraphic(percentagePriceLabel);
                } else {
                    percentagePriceLabel.setText("");
                    percentagePriceLabel.setTooltip(null);
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<OfferbookListItem, OfferbookListItem>,
            TableCell<OfferbookListItem, OfferbookListItem>> getFiatAmountCellFactory() {
        return column -> new TableCell<>() {
            private final Label fiatAmountLabel = new Label();
            private final BisqTooltip tooltip = new BisqTooltip();

            @Override
            protected void updateItem(OfferbookListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    fiatAmountLabel.setText(item.getFormattedRangeQuoteAmount());
                    tooltip.setText(item.getFormattedRangeQuoteAmount());
                    fiatAmountLabel.setTooltip(tooltip);
                    setGraphic(fiatAmountLabel);
                } else {
                    fiatAmountLabel.setText("");
                    fiatAmountLabel.setTooltip(null);
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<OfferbookListItem, OfferbookListItem>,
            TableCell<OfferbookListItem, OfferbookListItem>> getPaymentCellFactory() {
        return column -> new TableCell<>() {
            private final HBox hbox = new HBox(5);
            private final BisqTooltip tooltip = new BisqTooltip();

            {
                hbox.setAlignment(Pos.CENTER_RIGHT);
            }

            @Override
            protected void updateItem(OfferbookListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    hbox.getChildren().clear();
                    for (FiatPaymentMethod fiatPaymentMethod : item.getFiatPaymentMethods()) {
                        Node icon = !fiatPaymentMethod.isCustomPaymentMethod()
                                ? ImageUtil.getImageViewById(fiatPaymentMethod.getName())
                                : BisqEasyViewUtils.getCustomPaymentMethodIcon(fiatPaymentMethod.getDisplayString());
                        hbox.getChildren().add(icon);
                    }
                    tooltip.setText(Joiner.on("\n").join(item.getFiatPaymentMethods().stream()
                            .map(PaymentMethod::getDisplayString)
                            .toList()));
                    Tooltip.install(hbox, tooltip);
                    setGraphic(hbox);
                } else {
                    Tooltip.uninstall(hbox, tooltip);
                    hbox.getChildren().clear();
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<OfferbookListItem, OfferbookListItem>,
            TableCell<OfferbookListItem, OfferbookListItem>> getSettlementCellFactory() {
        return column -> new TableCell<>() {
            private final HBox hbox = new HBox(5);
            private final BisqTooltip tooltip = new BisqTooltip();

            {
                hbox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(OfferbookListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    hbox.getChildren().clear();
                    for (BitcoinPaymentMethod bitcoinPaymentMethod : item.getBitcoinPaymentMethods()) {
                        ImageView icon = ImageUtil.getImageViewById(bitcoinPaymentMethod.getName());
                        ColorAdjust colorAdjust = new ColorAdjust();
                        colorAdjust.setBrightness(-0.2);
                        icon.setEffect(colorAdjust);
                        hbox.getChildren().add(icon);
                    }
                    tooltip.setText(Joiner.on("\n").join(item.getBitcoinPaymentMethods().stream()
                            .map(PaymentMethod::getDisplayString)
                            .toList()));
                    Tooltip.install(hbox, tooltip);
                    setGraphic(hbox);
                } else {
                    Tooltip.uninstall(hbox, tooltip);
                    hbox.getChildren().clear();
                    setGraphic(null);
                }
            }
        };
    }
}
