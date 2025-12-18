package bisq.desktop.main.content.authorized_role.mediator;

import bisq.common.encoding.Csv;
import bisq.common.facades.FacadeProvider;
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
import javafx.scene.control.*;
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
class MediationTableView extends VBox {
    private final MediatorModel model;
    private final MediatorController controller;

    private final Switch showClosedCasesSwitch;
    private final SearchBox searchBox;
    private final BisqTableView<MediationCaseListItem> tableView;
    private BisqTableColumn<MediationCaseListItem> closeCaseDateColumn;
    private final Hyperlink exportHyperlink;
    private final Label numEntriesLabel;
    private final ListChangeListener<MediationCaseListItem> listChangeListener;
    private Subscription searchTextPin;
    private Subscription showClosedCasesPin, selectedModelItemPin, tableViewSelectionPin, noOpenCasesPin, chatWindowPin;

    MediationTableView(MediatorModel model, MediatorController controller) {
        this.model = model;
        this.controller = controller;

        Label headlineLabel = new Label(Res.get("authorizedRole.mediator.table.headline"));
        headlineLabel.getStyleClass().add("bisq-easy-container-headline");

        showClosedCasesSwitch = new Switch(Res.get("authorizedRole.mediator.showClosedCases"));

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
            String initialFileName = Res.get("authorizedRole.mediator.table.headline") + ".csv";
            FileChooserUtil.saveFile(tableView.getScene(), initialFileName)
                    .ifPresent(file -> {
                        try {
                            FacadeProvider.getJdkFacade().writeString(csv, file);
                        } catch (IOException e) {
                            new Popup().error(e).show();
                        }
                    });
        });

        showClosedCasesPin = EasyBind.subscribe(model.getShowClosedCases(), showClosedCases -> {
            showClosedCasesSwitch.setSelected(showClosedCases);
            tableView.setPlaceholderText(showClosedCases ?
                    Res.get("authorizedRole.mediator.noClosedCases") :
                    Res.get("authorizedRole.mediator.noOpenCases"));
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
                        Res.get("authorizedRole.mediator.noClosedCases") :
                        Res.get("authorizedRole.mediator.noOpenCases"));

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

    private Stream<BisqTableColumn<MediationCaseListItem>> getBisqTableColumnsForCsv() {
        return tableView.getColumns().stream()
                .filter(column -> column instanceof BisqTableColumn)
                .map(column -> {
                    @SuppressWarnings("unchecked")
                    BisqTableColumn<MediationCaseListItem> bisqTableColumn = (BisqTableColumn) column;
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
                        item.getQuoteAmountString().toLowerCase().contains(string) ||
                        item.getBaseAmountString().toLowerCase().contains(string) ||
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

        tableView.getColumns().add(new BisqTableColumn.Builder<MediationCaseListItem>()
                .title(Res.get("authorizedRole.mediator.table.maker"))
                .minWidth(120)
                .left()
                .comparator(Comparator.comparing(item -> item.getMaker().getUserName()))
                .setCellFactory(getMakerCellFactory())
                .valueSupplier(item -> item.getMaker().getUserName())// For csv export
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MediationCaseListItem>()
                .minWidth(95)
                .comparator(Comparator.comparing(MediationCaseListItem::getDirectionalTitle))
                .setCellFactory(getDirectionCellFactory())
                .includeForCsv(false)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MediationCaseListItem>()
                .title(Res.get("authorizedRole.mediator.table.taker"))
                .minWidth(120)
                .left()
                .comparator(Comparator.comparing(item -> item.getTaker().getUserName()))
                .setCellFactory(getTakerCellFactory())
                .valueSupplier(item -> item.getTaker().getUserName())// For csv export
                .build());

        tableView.getColumns().add(DateColumnUtil.getDateColumn(tableView.getSortOrder()));

        tableView.getColumns().add(new BisqTableColumn.Builder<MediationCaseListItem>()
                .title(Res.get("bisqEasy.openTrades.table.tradeId"))
                .minWidth(85)
                .comparator(Comparator.comparing(MediationCaseListItem::getTradeId))
                .valueSupplier(MediationCaseListItem::getShortTradeId)
                .tooltipSupplier(MediationCaseListItem::getTradeId)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MediationCaseListItem>()
                .title(Res.get("bisqEasy.openTrades.table.quoteAmount"))
                .fixWidth(120)
                .comparator(Comparator.comparing(MediationCaseListItem::getQuoteAmount))
                .valueSupplier(MediationCaseListItem::getQuoteAmountString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MediationCaseListItem>()
                .title(Res.get("bisqEasy.openTrades.table.baseAmount"))
                .fixWidth(120)
                .comparator(Comparator.comparing(MediationCaseListItem::getBaseAmount))
                .valueSupplier(MediationCaseListItem::getBaseAmountString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MediationCaseListItem>()
                .title(Res.get("bisqEasy.openTrades.table.price"))
                .fixWidth(170)
                .comparator(Comparator.comparing(MediationCaseListItem::getPrice))
                .valueSupplier(MediationCaseListItem::getPriceString)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<MediationCaseListItem>()
                .title(Res.get("bisqEasy.openTrades.table.paymentMethod"))
                .minWidth(130)
                .right()
                .comparator(Comparator.comparing(MediationCaseListItem::getPaymentMethod))
                .valueSupplier(MediationCaseListItem::getPaymentMethod)
                .tooltipSupplier(MediationCaseListItem::getPaymentMethod)
                .build());
        closeCaseDateColumn = new BisqTableColumn.Builder<MediationCaseListItem>()
                .title(Res.get("authorizedRole.mediator.table.header.closeCaseDate"))
                .minWidth(130)
                .right()
                .comparator(Comparator.comparing(MediationCaseListItem::getCloseCaseDate))
                .sortType(TableColumn.SortType.DESCENDING)
                .setCellFactory(getCloseDateCellFactory())
                .valueSupplier(item -> item.getCloseCaseDateString() + " " + item.getCloseCaseTimeString())
                .build();
        tableView.getColumns().add(closeCaseDateColumn);
    }

    private Callback<TableColumn<MediationCaseListItem, MediationCaseListItem>,
            TableCell<MediationCaseListItem, MediationCaseListItem>> getCloseDateCellFactory() {
        return column -> new TableCell<>() {

            private final Label label = new Label();

            @Override
            protected void updateItem(MediationCaseListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    Label date = new Label(item.getCloseCaseDateString());
                    date.getStyleClass().add("table-view-date-column-date");
                    Label time = new Label(item.getCloseCaseTimeString());
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

    private Callback<TableColumn<MediationCaseListItem, MediationCaseListItem>,
            TableCell<MediationCaseListItem, MediationCaseListItem>> getDirectionCellFactory() {
        return column -> new TableCell<>() {

            private final Label label = new Label();

            @Override
            protected void updateItem(MediationCaseListItem item, boolean empty) {
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

    private Callback<TableColumn<MediationCaseListItem, MediationCaseListItem>,
            TableCell<MediationCaseListItem, MediationCaseListItem>> getMakerCellFactory() {
        return column -> new TableCell<>() {

            private UserProfileDisplay userProfileDisplay;

            @Override
            protected void updateItem(MediationCaseListItem item, boolean empty) {
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

    private Callback<TableColumn<MediationCaseListItem, MediationCaseListItem>,
            TableCell<MediationCaseListItem, MediationCaseListItem>> getTakerCellFactory() {
        return column -> new TableCell<>() {

            private UserProfileDisplay userProfileDisplay;

            @Override
            protected void updateItem(MediationCaseListItem item, boolean empty) {
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

    private static UserProfileDisplay applyTraderToTableCell(TableCell<MediationCaseListItem, MediationCaseListItem> tableCell,
                                                             MediationCaseListItem item,
                                                             boolean isRequester,
                                                             MediationCaseListItem.Trader trader) {
        UserProfileDisplay userProfileDisplay = new UserProfileDisplay(trader.getUserProfile(), false);
        if (isRequester) {
            userProfileDisplay.getStyleClass().add("mediator-table-requester");
        }
        userProfileDisplay.getTooltip().setText(Res.get("authorizedRole.mediator.hasRequested",
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
