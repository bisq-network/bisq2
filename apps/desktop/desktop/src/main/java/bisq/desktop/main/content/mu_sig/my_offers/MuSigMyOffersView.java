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

package bisq.desktop.main.content.mu_sig.my_offers;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.RichTableView;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.desktop.main.content.mu_sig.MuSigOfferListItem;
import bisq.desktop.main.content.mu_sig.MuSigOfferUtil;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MuSigMyOffersView extends View<VBox, MuSigMyOffersModel, MuSigMyOffersController> {
    private static final double SIDE_PADDING = 40;

    private final RichTableView<MuSigOfferListItem> muSigMyOffersListView;
    private final Button createOfferButton;
    private BisqTableColumn<MuSigOfferListItem> myProfileColumn;

    public MuSigMyOffersView(MuSigMyOffersModel model, MuSigMyOffersController controller) {
        super(new VBox(), model, controller);

        muSigMyOffersListView = new RichTableView<>(
                model.getSortedMuSigMyOffersListItems(),
                Res.get("muSig.myOffers.headline"),
                Res.get("muSig.myOffers.numOffers"),
                controller::applySearchPredicate);
        muSigMyOffersListView.getStyleClass().add("mu-sig-my-offers-table");
        createOfferButton = new Button(Res.get("muSig.myOffers.createOfferButton"));
        createOfferButton.getStyleClass().addAll("create-offer-button", "normal-text");
        HBox.setMargin(createOfferButton, new Insets(0, 0, 0, 15));
        muSigMyOffersListView.getHeaderBox().getChildren().add(createOfferButton);
        configMuSigMyOffersListView();
        VBox.setVgrow(muSigMyOffersListView, Priority.ALWAYS);

        root.setPadding(new Insets(0, SIDE_PADDING, 0, SIDE_PADDING));
        root.getChildren().addAll(muSigMyOffersListView);
    }

    private void configMuSigMyOffersListView() {
        muSigMyOffersListView.getColumns().add(muSigMyOffersListView.getTableView().getSelectionMarkerColumn());

        muSigMyOffersListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.myOffers.table.header.market"))
                .left()
                .fixWidth(81)
                .comparator(Comparator.comparing(MuSigOfferListItem::getMarket))
                .setCellFactory(getMarketCellFactory())
                .includeForCsv(false)
                .build());

        myProfileColumn = new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.myOffers.table.header.myProfile"))
                .left()
                .minWidth(140)
                .comparator(Comparator.comparing(item -> item.getMakerUserProfile().getNickName()))
                .setCellFactory(MuSigOfferUtil.getUserProfileCellFactory())
                .includeForCsv(false)
                .build();
        muSigMyOffersListView.getColumns().add(myProfileColumn);

        muSigMyOffersListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.myOffers.table.header.offerId"))
                .left()
                .minWidth(100)
                .comparator(Comparator.comparing(MuSigOfferListItem::getOfferId))
                .valueSupplier(MuSigOfferListItem::getOfferId)
                .build());

        BisqTableColumn<MuSigOfferListItem> dateColumn = new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.myOffers.table.header.date"))
                .left()
                .minWidth(160)
                .comparator(Comparator.comparing(MuSigOfferListItem::getOfferDate))
                .valueSupplier(MuSigOfferListItem::getOfferDate)
                .sortType(TableColumn.SortType.DESCENDING)
                .build();
        muSigMyOffersListView.getColumns().add(dateColumn);
        muSigMyOffersListView.getSortOrder().add(dateColumn);

        muSigMyOffersListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.myOffers.table.header.offerType"))
                .left()
                .minWidth(100)
                .comparator(Comparator.comparing(MuSigOfferListItem::getOfferIntentText))
                .valueSupplier(MuSigOfferListItem::getOfferIntentText)
                .build());

        muSigMyOffersListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.myOffers.table.header.baseAmount"))
                .left()
                .minWidth(140)
                .comparator(Comparator.comparing(MuSigOfferListItem::getBaseAmountWithSymbol))
                .setCellFactory(MuSigOfferUtil.getBaseAmountCellFactory(true))
                .includeForCsv(false)
                .build());

        muSigMyOffersListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.myOffers.table.header.quoteAmount"))
                .left()
                .minWidth(130)
                .comparator(Comparator.comparing(MuSigOfferListItem::getQuoteAmountWithSymbol))
                .valueSupplier(MuSigOfferListItem::getQuoteAmountWithSymbol)
                .build());

        muSigMyOffersListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.myOffers.table.header.price"))
                .left()
                .fixWidth(180)
                .comparator(Comparator.comparing(MuSigOfferListItem::getPrice))
                .setCellFactory(MuSigOfferUtil.getPriceCellFactory())
                .includeForCsv(false)
                .build());

        muSigMyOffersListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.myOffers.table.header.deposit"))
                .left()
                .minWidth(80)
                .comparator(Comparator.comparing(MuSigOfferListItem::getDeposit))
                .valueSupplier(MuSigOfferListItem::getDeposit)
                .build());

        muSigMyOffersListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .title(Res.get("muSig.myOffers.table.header.paymentMethods"))
                .left()
                .minWidth(140)
                .setCellFactory(MuSigOfferUtil.getPaymentCellFactory())
                .comparator(Comparator.comparing(MuSigOfferListItem::getPaymentMethodsAsString))
                .includeForCsv(false)
                .build());

        muSigMyOffersListView.getColumns().add(new BisqTableColumn.Builder<MuSigOfferListItem>()
                .setCellFactory(getActionButtonsCellFactory())
                .minWidth(150)
                .includeForCsv(false)
                .build());
    }

    @Override
    protected void onViewAttached() {
        muSigMyOffersListView.initialize();
        muSigMyOffersListView.resetSearch();
        muSigMyOffersListView.sort();
        myProfileColumn.visibleProperty().set(model.isShouldShowMyProfileColumn());

        createOfferButton.setOnAction(e -> controller.onCreateOffer());

        List<String> csvHeaders = muSigMyOffersListView.buildCsvHeaders();
        csvHeaders.add(Res.get("muSig.myOffers.table.header.market").toUpperCase());
        csvHeaders.add(Res.get("muSig.myOffers.table.header.myProfile").toUpperCase());
        csvHeaders.add(Res.get("muSig.myOffers.table.header.baseAmount").toUpperCase());
        csvHeaders.add(Res.get("muSig.myOffers.table.header.price").toUpperCase());
        csvHeaders.add(Res.get("muSig.myOffers.table.header.paymentMethods").toUpperCase());
        muSigMyOffersListView.setCsvHeaders(Optional.of(csvHeaders));

        List<List<String>> csvData = muSigMyOffersListView.getItems().stream()
                .map(item -> {
                    List<String> cellDataInRow = muSigMyOffersListView.getBisqTableColumnsForCsv()
                            .map(bisqTableColumn -> bisqTableColumn.resolveValueForCsv(item))
                            .collect(Collectors.toList());

                    // Add market
                    cellDataInRow.add(item.getMarket().getMarketDisplayName());

                    // Add my profile
                    cellDataInRow.add(item.getMakerUserProfile().getUserName());

                    // Add base amount
                    cellDataInRow.add(item.getBaseAmountWithSymbol());

                    // Add price
                    cellDataInRow.add(item.getOfferPriceWithSpec());

                    // Add payment methods
                    cellDataInRow.add(item.getPaymentMethodsAsString());

                    return cellDataInRow;
                })
                .collect(Collectors.toList());
        muSigMyOffersListView.setCsvData(Optional.of(csvData));
    }

    @Override
    protected void onViewDetached() {
        muSigMyOffersListView.dispose();

        createOfferButton.setOnAction(null);
    }

    private Callback<TableColumn<MuSigOfferListItem, MuSigOfferListItem>, TableCell<MuSigOfferListItem, MuSigOfferListItem>> getActionButtonsCellFactory() {
        return column -> new TableCell<>() {
            private static final double PREF_WIDTH = 132;
            private static final double PREF_HEIGHT = 26;

            private final HBox myOfferMainBox = new HBox();
            private final HBox myOfferActionsMenuBox = new HBox(5);
            private final BisqMenuItem removeOfferMenuItem = new BisqMenuItem("delete-t-grey", "delete-t-red");
            private final BisqMenuItem copyOfferMenuItem = new BisqMenuItem("copy-grey", "copy-white");
            private final BisqMenuItem editOfferMenuItem = new BisqMenuItem("edit-grey", "edit-white");
            private final BisqMenuItem goToOfferMenuItem = new BisqMenuItem("open-link-grey", "open-link-white");
            private final ChangeListener<Boolean> selectedListener = (observable, oldValue, newValue) -> {
                boolean shouldShow = newValue || getTableRow().isHover();
                myOfferActionsMenuBox.setVisible(shouldShow);
                myOfferActionsMenuBox.setManaged(shouldShow);
            };

            {
                myOfferMainBox.setMinWidth(PREF_WIDTH);
                myOfferMainBox.setPrefWidth(PREF_WIDTH);
                myOfferMainBox.setMaxWidth(PREF_WIDTH);
                myOfferMainBox.setMinHeight(PREF_HEIGHT);
                myOfferMainBox.setPrefHeight(PREF_HEIGHT);
                myOfferMainBox.setMaxHeight(PREF_HEIGHT);
                myOfferMainBox.getChildren().addAll(myOfferActionsMenuBox);

                myOfferActionsMenuBox.setMinWidth(PREF_WIDTH);
                myOfferActionsMenuBox.setPrefWidth(PREF_WIDTH);
                myOfferActionsMenuBox.setMaxWidth(PREF_WIDTH);
                myOfferActionsMenuBox.setMinHeight(PREF_HEIGHT);
                myOfferActionsMenuBox.setPrefHeight(PREF_HEIGHT);
                myOfferActionsMenuBox.setMaxHeight(PREF_HEIGHT);
                myOfferActionsMenuBox.getChildren().addAll(editOfferMenuItem, copyOfferMenuItem,
                        goToOfferMenuItem, removeOfferMenuItem);
                myOfferActionsMenuBox.setAlignment(Pos.CENTER);

                removeOfferMenuItem.useIconOnly();
                removeOfferMenuItem.setTooltip(Res.get("offer.delete"));

                copyOfferMenuItem.useIconOnly();
                copyOfferMenuItem.setTooltip(Res.get("offer.copy"));

                editOfferMenuItem.useIconOnly();
                editOfferMenuItem.setTooltip(Res.get("offer.edit"));

                goToOfferMenuItem.useIconOnly();
                goToOfferMenuItem.setTooltip(Res.get("muSig.myOffers.table.cell.actionButtons.goToOffer.tooltip"));
            }

            @Override
            protected void updateItem(MuSigOfferListItem item, boolean empty) {
                super.updateItem(item, empty);

                resetRowEventHandlersAndListeners();
                resetVisibilities();

                if (item != null && !empty) {
                    setUpRowEventHandlersAndListeners();
                    setGraphic(myOfferMainBox);
                    goToOfferMenuItem.setOnAction(e -> controller.onGoToOffer(item.getOffer()));
                    removeOfferMenuItem.setOnAction(e -> controller.onRemoveOffer(item.getOffer()));
                } else {
                    resetRowEventHandlersAndListeners();
                    resetVisibilities();
                    goToOfferMenuItem.setOnAction(null);
                    removeOfferMenuItem.setOnAction(null);
                    setGraphic(null);
                }
            }

            private void setUpRowEventHandlersAndListeners() {
                TableRow<?> row = getTableRow();
                if (row != null) {
                    row.setOnMouseEntered(e -> {
                        boolean shouldShow = row.isSelected() || row.isHover();
                        myOfferActionsMenuBox.setVisible(shouldShow);
                        myOfferActionsMenuBox.setManaged(shouldShow);
                    });
                    row.setOnMouseExited(e -> {
                        boolean shouldShow = row.isSelected();
                        myOfferActionsMenuBox.setVisible(shouldShow);
                        myOfferActionsMenuBox.setManaged(shouldShow);
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
                myOfferActionsMenuBox.setVisible(false);
                myOfferActionsMenuBox.setManaged(false);
            }
        };
    }

    public static Callback<TableColumn<MuSigOfferListItem, MuSigOfferListItem>,
            TableCell<MuSigOfferListItem, MuSigOfferListItem>> getMarketCellFactory() {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(MuSigOfferListItem item, boolean empty) {
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
}
