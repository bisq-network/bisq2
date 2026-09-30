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

package bisq.desktop.main.content.authorized_role.arbitrator.mu_sig;

import bisq.common.encoding.Csv;
import bisq.common.file.FileMutatorUtils;
import bisq.common.util.StringUtils;
import bisq.desktop.common.Layout;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.FileChooserUtil;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Badge;
import bisq.desktop.components.controls.SearchBox;
import bisq.desktop.components.controls.Switch;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.DateColumnUtil;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.i18n.Res;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Convenience class for a feature rich table view with a headline, search, num entries and support for filters.
 */
@Slf4j
@Getter
class MuSigArbitrationTableView extends VBox {
    private final MuSigArbitratorModel model;
    private final MuSigArbitratorController controller;

    private final Switch showClosedCasesSwitch;
    private final SearchBox searchBox;
    private final BisqTableView<MuSigArbitrationCaseListItem> tableView;
    private BisqTableColumn<MuSigArbitrationCaseListItem> closeCaseDateColumn;
    private final Hyperlink exportHyperlink;
    private final Label numEntriesLabel;
    private final ListChangeListener<MuSigArbitrationCaseListItem> listChangeListener;
    private Subscription searchTextPin;
    private Subscription showClosedCasesPin, selectedModelItemPin, tableViewSelectionPin, noOpenCasesPin, chatWindowPin;

    MuSigArbitrationTableView(MuSigArbitratorModel model, MuSigArbitratorController controller) {
        this.model = model;
        this.controller = controller;

        Label headlineLabel = new Label(Res.get("authorizedRole.arbitrator.table.headline"));
        headlineLabel.getStyleClass().add("bisq-easy-container-headline");

        showClosedCasesSwitch = new Switch(Res.get("authorizedRole.disputeActor.showClosedCases"));

        searchBox = new SearchBox();
        searchBox.setPrefWidth(90);
        HBox.setMargin(searchBox, new Insets(0, 4, 0, 0));

        HBox headerBox = new HBox(10, headlineLabel, Spacer.fillHBox(), showClosedCasesSwitch, searchBox);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(15, 30, 15, 30));

        tableView = new BisqTableView<>(model.getListItems().getSortedList());
        tableView.getStyleClass().addAll("bisq-easy-open-trades", "hide-horizontal-scrollbar");
        configTableView();

        numEntriesLabel = new Label();
        numEntriesLabel.getStyleClass().add("rich-table-num-entries");
        numEntriesLabel.setAlignment(Pos.BASELINE_LEFT);

        exportHyperlink = new Hyperlink(Res.get("action.exportAsCsv"));
        exportHyperlink.getStyleClass().add("rich-table-num-entries");
        exportHyperlink.setAlignment(Pos.BASELINE_LEFT);

        HBox footerVBox = new HBox(numEntriesLabel, Spacer.fillHBox(), exportHyperlink);
        footerVBox.setAlignment(Pos.BASELINE_LEFT);

        VBox.setMargin(headerBox, new Insets(0, 0, 5, 0));
        VBox.setVgrow(tableView, Priority.ALWAYS);
        VBox headerAndTable = new VBox(headerBox, Layout.hLine(), tableView);
        headerAndTable.setFillWidth(true);
        headerAndTable.getStyleClass().add("bisq-easy-container");

        VBox.setMargin(footerVBox, new Insets(2.5, 10, 5, 10));
        getChildren().addAll(headerAndTable, footerVBox);
        setFillWidth(true);

        listChangeListener = c -> listItemsChanged();
    }

    void initialize() {
        tableView.initialize();
        tableView.getItems().addListener(listChangeListener);
        listItemsChanged();
        searchTextPin = EasyBind.subscribe(searchBox.textProperty(), this::applySearchPredicate);
        exportHyperlink.setOnAction(ev -> {
            List<String> headers = buildCsvHeaders();
            List<List<String>> data = buildCsvData();
            String csv = Csv.toCsv(headers, data);
            String initialFileName = Res.get("authorizedRole.arbitrator.table.headline") + ".csv";
            FileChooserUtil.saveFile(tableView.getScene(), initialFileName)
                    .ifPresent(file -> {
                        try {
                            FileMutatorUtils.writeToPath(csv, file);
                        } catch (IOException e) {
                            new Popup().error(e).show();
                        }
                    });
        });

        showClosedCasesPin = EasyBind.subscribe(model.getShowClosedCases(), showClosedCases -> {
            showClosedCasesSwitch.setSelected(showClosedCases);
            tableView.setPlaceholderText(showClosedCases ?
                    Res.get("authorizedRole.arbitrator.noClosedCases") :
                    Res.get("authorizedRole.arbitrator.noOpenCases"));
            closeCaseDateColumn.setVisible(showClosedCases);
        });
        showClosedCasesSwitch.setOnAction(e -> controller.onToggleClosedCases());

        selectedModelItemPin = EasyBind.subscribe(model.getSelectedItem(),
                selected -> tableView.getSelectionModel().select(selected));

        tableViewSelectionPin = EasyBind.subscribe(tableView.getSelectionModel().selectedItemProperty(),
                item -> {
                    if (item != null) {
                        controller.onSelectItem(item);
                    }
                });
        noOpenCasesPin = EasyBind.subscribe(model.getNoOpenCases(), noOpenCases -> {
            if (noOpenCases) {
                tableView.getStyleClass().add("empty-table");
                tableView.setPlaceholderText(model.getShowClosedCases().get() ?
                        Res.get("authorizedRole.arbitrator.noClosedCases") :
                        Res.get("authorizedRole.arbitrator.noOpenCases"));

                tableView.setMinHeight(150);
                tableView.setMaxHeight(150);
            } else {
                tableView.setPlaceholder(null);
                tableView.getStyleClass().remove("empty-table");
            }
        });

        chatWindowPin = EasyBind.subscribe(model.getChatWindow(), e -> updateHeight());
    }

    void dispose() {
        tableView.dispose();
        tableView.getItems().removeListener(listChangeListener);

        searchTextPin.unsubscribe();
        showClosedCasesPin.unsubscribe();
        selectedModelItemPin.unsubscribe();
        tableViewSelectionPin.unsubscribe();
        noOpenCasesPin.unsubscribe();
        chatWindowPin.unsubscribe();

        exportHyperlink.setOnAction(null);
        showClosedCasesSwitch.setOnAction(null);
    }

    private void resetSearch() {
        searchBox.clear();
    }

    private Stream<BisqTableColumn<MuSigArbitrationCaseListItem>> getBisqTableColumnsForCsv() {
        return tableView.getColumns().stream()
                .filter(column -> column instanceof BisqTableColumn)
                .map(column -> {
                    @SuppressWarnings("unchecked")
                    BisqTableColumn<MuSigArbitrationCaseListItem> bisqTableColumn = (BisqTableColumn) column;
                    return bisqTableColumn;
                })
                .filter(TableColumnBase::isVisible)
                .filter(BisqTableColumn::isIncludeForCsv);
    }

    private List<String> buildCsvHeaders() {
        return getBisqTableColumnsForCsv()
                .map(BisqTableColumn::getHeaderForCsv)
                .collect(Collectors.toList());
    }

    private List<List<String>> buildCsvData() {
        return tableView.getItems().stream()
                .map(item -> getBisqTableColumnsForCsv()
                        .map(bisqTableColumn -> bisqTableColumn.resolveValueForCsv(item))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    private void applySearchPredicate(String searchText) {
        String string = searchText.toLowerCase();
        model.getSearchPredicate().set(item ->
                StringUtils.isEmpty(string) ||
                        item.getMaker().getUserName().toLowerCase().contains(string) ||
                        item.getTaker().getUserName().toLowerCase().contains(string) ||
                        item.getTradeId().toLowerCase().contains(string) ||
                        item.getMarket().toLowerCase().contains(string) ||
                        item.getPaymentMethod().toLowerCase().contains(string) ||
                        item.getPriceString().toLowerCase().contains(string) ||
                        item.getNonBtcAmountString().toLowerCase().contains(string) ||
                        item.getBtcAmountString().toLowerCase().contains(string) ||
                        item.getDateString().toLowerCase().contains(string) ||
                        item.getTimeString().toLowerCase().contains(string) ||
                        item.getCloseCaseDateString().toLowerCase().contains(string) ||
                        item.getCloseCaseTimeString().toLowerCase().contains(string));
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void listItemsChanged() {
        numEntriesLabel.setText(Res.get("component.standardTable.numEntries", tableView.getItems().size()));
        updateHeight();
    }

    private void updateHeight() {
        if (tableView.getItems().isEmpty()) {
            return;
        }
        // Allow table to use full height if chat is detached
        int maxNumItems = model.getChatWindow().get() == null ? 3 : Integer.MAX_VALUE;
        double height = tableView.calculateTableHeight(maxNumItems);
        tableView.setMinHeight(height + 1);
        tableView.setMaxHeight(height + 1);
        UIThread.runOnNextRenderFrame(() -> {
            tableView.setMinHeight(height);
            tableView.setMaxHeight(height);
            // Delay call as otherwise the width does not take the scrollbar width correctly into account
            UIThread.runOnNextRenderFrame(tableView::adjustMinWidth);
        });
    }

    private void configTableView() {
        tableView.getColumns().add(tableView.getSelectionMarkerColumn());

        tableView.getColumns().add(new BisqTableColumn.Builder<MuSigArbitrationCaseListItem>()
                .title(Res.get("authorizedRole.disputeActor.table.maker"))
                .minWidth(120)
                .left()
                .comparator(Comparator.comparing(item -> item.getMaker().getUserName()))
                .setCellFactory(getMakerCellFactory())
                .valueSupplier(item -> item.getMaker().getUserName())// For csv export
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MuSigArbitrationCaseListItem>()
                .minWidth(95)
                .comparator(Comparator.comparing(MuSigArbitrationCaseListItem::getDirectionalTitle))
                .setCellFactory(getDirectionCellFactory())
                .includeForCsv(false)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MuSigArbitrationCaseListItem>()
                .title(Res.get("authorizedRole.disputeActor.table.taker"))
                .minWidth(120)
                .left()
                .comparator(Comparator.comparing(item -> item.getTaker().getUserName()))
                .setCellFactory(getTakerCellFactory())
                .valueSupplier(item -> item.getTaker().getUserName())// For csv export
                .build());

        tableView.getColumns().add(DateColumnUtil.getDateColumn(tableView.getSortOrder()));

        tableView.getColumns().add(new BisqTableColumn.Builder<MuSigArbitrationCaseListItem>()
                .title(Res.get("bisqEasy.openTrades.table.tradeId"))
                .minWidth(85)
                .comparator(Comparator.comparing(MuSigArbitrationCaseListItem::getTradeId))
                .valueSupplier(MuSigArbitrationCaseListItem::getShortTradeId)
                .tooltipSupplier(MuSigArbitrationCaseListItem::getTradeId)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MuSigArbitrationCaseListItem>()
                .title(Res.get("bisqEasy.openTrades.table.quoteAmount"))
                .fixWidth(120)
                .comparator(Comparator.comparing(MuSigArbitrationCaseListItem::getNonBtcAmount))
                .valueSupplier(MuSigArbitrationCaseListItem::getNonBtcAmountString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MuSigArbitrationCaseListItem>()
                .title(Res.get("bisqEasy.openTrades.table.baseAmount"))
                .fixWidth(120)
                .comparator(Comparator.comparing(MuSigArbitrationCaseListItem::getBtcAmount))
                .valueSupplier(MuSigArbitrationCaseListItem::getBtcAmountString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MuSigArbitrationCaseListItem>()
                .title(Res.get("bisqEasy.openTrades.table.price"))
                .fixWidth(170)
                .comparator(Comparator.comparing(MuSigArbitrationCaseListItem::getPrice))
                .valueSupplier(MuSigArbitrationCaseListItem::getPriceString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MuSigArbitrationCaseListItem>()
                .title(Res.get("bisqEasy.openTrades.table.paymentMethod"))
                .minWidth(130)
                .right()
                .comparator(Comparator.comparing(MuSigArbitrationCaseListItem::getPaymentMethod))
                .valueSupplier(MuSigArbitrationCaseListItem::getPaymentMethod)
                .tooltipSupplier(MuSigArbitrationCaseListItem::getPaymentMethod)
                .build());
        closeCaseDateColumn = new BisqTableColumn.Builder<MuSigArbitrationCaseListItem>()
                .title(Res.get("authorizedRole.disputeActor.table.header.closeCaseDate"))
                .minWidth(130)
                .right()
                .comparator(Comparator.comparing(MuSigArbitrationCaseListItem::getCloseCaseDate))
                .sortType(TableColumn.SortType.DESCENDING)
                .setCellFactory(getCloseDateCellFactory())
                .valueSupplier(item -> item.getCloseCaseDateString() + " " + item.getCloseCaseTimeString())
                .build();
        tableView.getColumns().add(closeCaseDateColumn);
    }

    private Callback<TableColumn<MuSigArbitrationCaseListItem, MuSigArbitrationCaseListItem>,
            TableCell<MuSigArbitrationCaseListItem, MuSigArbitrationCaseListItem>> getCloseDateCellFactory() {
        return column -> new TableCell<>() {
            private final Label date = new Label();
            private final Label time = new Label();
            private final VBox vBox = new VBox(3, date, time);

            {
                date.getStyleClass().add("table-view-date-column-date");
                time.getStyleClass().add("table-view-date-column-time");
                vBox.setAlignment(Pos.CENTER);
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(MuSigArbitrationCaseListItem item, boolean empty) {
                super.updateItem(item, empty);

                date.textProperty().unbind();
                time.textProperty().unbind();

                if (item != null && !empty) {
                    date.textProperty().bind(item.getCloseCaseDateStringProperty());
                    time.textProperty().bind(item.getCloseCaseTimeStringProperty());
                    setGraphic(vBox);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<MuSigArbitrationCaseListItem, MuSigArbitrationCaseListItem>,
            TableCell<MuSigArbitrationCaseListItem, MuSigArbitrationCaseListItem>> getDirectionCellFactory() {
        return column -> new TableCell<>() {

            private final Label label = new Label();

            @Override
            protected void updateItem(MuSigArbitrationCaseListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    label.setText(item.getDirectionalTitle());
                    label.setPadding(new Insets(-9, -20, 0, -20));
                    setGraphic(label);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    private Callback<TableColumn<MuSigArbitrationCaseListItem, MuSigArbitrationCaseListItem>,
            TableCell<MuSigArbitrationCaseListItem, MuSigArbitrationCaseListItem>> getMakerCellFactory() {
        return column -> new TableCell<>() {

            private UserProfileDisplay userProfileDisplay;

            @Override
            protected void updateItem(MuSigArbitrationCaseListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    userProfileDisplay = applyTraderToTableCell(this, item, item.isMakerRequester(), item.getMaker());
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

    private Callback<TableColumn<MuSigArbitrationCaseListItem, MuSigArbitrationCaseListItem>,
            TableCell<MuSigArbitrationCaseListItem, MuSigArbitrationCaseListItem>> getTakerCellFactory() {
        return column -> new TableCell<>() {

            private UserProfileDisplay userProfileDisplay;

            @Override
            protected void updateItem(MuSigArbitrationCaseListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    userProfileDisplay = applyTraderToTableCell(this, item, !item.isMakerRequester(), item.getTaker());
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

    private static UserProfileDisplay applyTraderToTableCell(TableCell<MuSigArbitrationCaseListItem, MuSigArbitrationCaseListItem> tableCell,
                                                             MuSigArbitrationCaseListItem item,
                                                             boolean isRequester,
                                                             MuSigArbitrationCaseListItem.Trader trader) {
        UserProfileDisplay userProfileDisplay = new UserProfileDisplay(trader.getUserProfile(), false);
        userProfileDisplay.setReputationScore(trader.getReputationScore());
        if (isRequester) {
            userProfileDisplay.getStyleClass().add("mediator-table-requester");
        }
        userProfileDisplay.getTooltip().setText(Res.get("authorizedRole.arbitrator.hasRequested",
                userProfileDisplay.getTooltipText(),
                isRequester ? Res.get("confirmation.yes") : Res.get("confirmation.no")
        ));
        Badge badge = trader.equals(item.getMaker()) ? item.getMakersBadge() : item.getTakersBadge();
        badge.setControl(userProfileDisplay);
        badge.getStyleClass().add("open-trades-badge");
        badge.setPosition(Pos.BOTTOM_LEFT);
        badge.setBadgeInsets(new Insets(0, 0, 7.5, 20));
        // Label color does not get applied from badge style when in a list cell even we use '!important' in the css.
        badge.getLabel().setStyle("-fx-text-fill: black !important;");
        tableCell.setGraphic(badge);
        return userProfileDisplay;
    }
}
