package bisq.desktop.main.content.authorized_role.mediator;

import bisq.common.encoding.Csv;
import bisq.common.file.FileUtils;
import bisq.common.util.StringUtils;
import bisq.desktop.common.Layout;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.FileChooserUtil;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.*;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.DateColumnUtil;
import bisq.desktop.components.table.RichTableView;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
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
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Convenience class for a feature rich table view with a headline, search, num entries and support for filters.
 */
@Slf4j
@Getter
class MediationTableView extends VBox {
    private final Optional<List<RichTableView.FilterMenuItem<MediationCaseListItem>>> filterItems;
    private final Optional<ToggleGroup> toggleGroup;
    private final Consumer<String> searchTextHandler;
    private final Label numEntriesLabel;
    @Getter
    private final BisqTableView<MediationCaseListItem> tableView;
    private final DropdownMenu filterMenu;
    private final BisqTooltip tooltip;
    private final SearchBox searchBox;
    private final Hyperlink exportHyperlink;
    private final ChangeListener<Toggle> toggleChangeListener;
    private final ListChangeListener<MediationCaseListItem> listChangeListener;
    private final Switch showClosedCasesSwitch;
    private Subscription searchTextPin;
    private BisqTableColumn<MediationCaseListItem> closeCaseDateColumn;
    private MediatorModel model;
    private MediatorController controller;
    private Subscription showClosedCasesPin, selectedModelItemPin, tableViewSelectionPin, noOpenCasesPin, chatWindowPin;

    MediationTableView(MediatorModel model, MediatorController controller) {
        this(model.getListItems().getSortedList(),
                Optional.empty(),
                Optional.empty());
        this.model = model;
        this.controller = controller;
    }

    MediationTableView(SortedList<MediationCaseListItem> sortedList,
                       Optional<List<RichTableView.FilterMenuItem<MediationCaseListItem>>> filterItems,
                       Optional<ToggleGroup> toggleGroup) {
        this.filterItems = filterItems;
        this.toggleGroup = toggleGroup;
        this.searchTextHandler = this::applySearchPredicate;
        if (filterItems.isPresent()) {
            checkArgument(toggleGroup.isPresent(), "filterItems and toggleGroup must be both present or empty");
        }
        if (toggleGroup.isPresent()) {
            checkArgument(filterItems.isPresent(), "filterItems and toggleGroup must be both present or empty");
        }

        Label headlineLabel = new Label(Res.get("authorizedRole.mediator.table.headline"));
        headlineLabel.getStyleClass().add("bisq-easy-container-headline");

        filterMenu = new DropdownMenu("chevron-drop-menu-grey", "chevron-drop-menu-white", false);
        filterMenu.setManaged(filterItems.isPresent());
        filterMenu.setVisible(filterMenu.isManaged());
        filterItems.ifPresent(filterMenu::addMenuItems);
        tooltip = new BisqTooltip();
        filterMenu.setTooltip(tooltip);

        searchBox = new SearchBox();
        searchBox.setPrefWidth(90);
        HBox.setMargin(searchBox, new Insets(0, 4, 0, 0));
        HBox filterBox = new HBox(10, searchBox, filterMenu);

        showClosedCasesSwitch = new Switch(Res.get("authorizedRole.mediator.showClosedCases"));

        HBox headerBox = new HBox(10, headlineLabel, Spacer.fillHBox(), showClosedCasesSwitch, filterBox);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(15, 30, 15, 30));
        // headerBox.getStyleClass().add("chat-container-header");

        tableView = new BisqTableView<>(sortedList);
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
        toggleChangeListener = (observable, oldValue, newValue) -> selectedFilterMenuItemChanged();
    }

    void initialize() {
        tableView.initialize();
        tableView.getItems().addListener(listChangeListener);
        listItemsChanged();
        toggleGroup.ifPresent(toggleGroup -> toggleGroup.selectedToggleProperty().addListener(toggleChangeListener));
        selectedFilterMenuItemChanged();
        filterItems.ifPresent(filterItems -> filterItems.forEach(RichTableView.FilterMenuItem::initialize));
        searchTextPin = EasyBind.subscribe(searchBox.textProperty(), searchTextHandler);
        exportHyperlink.setOnAction(ev -> {
            List<String> headers = buildCsvHeaders();
            List<List<String>> data = buildCsvData();
            String csv = Csv.toCsv(headers, data);
            String initialFileName = Res.get("authorizedRole.mediator.table.headline") + ".csv";
            FileChooserUtil.saveFile(tableView.getScene(), initialFileName)
                    .ifPresent(file -> {
                        try {
                            FileUtils.writeToFile(csv, file);
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
                tableView.removeListeners();
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
        toggleGroup.ifPresent(toggleGroup -> toggleGroup.selectedToggleProperty().removeListener(toggleChangeListener));
        filterItems.ifPresent(filterItems -> filterItems.forEach(RichTableView.FilterMenuItem::dispose));

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
        model.getListItems().setPredicate(item ->
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

    ///////////////////////////////////////////////////////////////////////////////////////////
    // TableView delegates

    ///////////////////////////////////////////////////////////////////////////////////////////

    public void refresh() {
        tableView.refresh();
    }

    public ObservableList<TableColumn<MediationCaseListItem, ?>> getSortOrder() {
        return tableView.getSortOrder();
    }

    public ObservableList<MediationCaseListItem> getItems() {
        return tableView.getItems();
    }

    public ObservableList<TableColumn<MediationCaseListItem, ?>> getColumns() {
        return tableView.getColumns();
    }

    public void setFixHeight(double value) {
        tableView.setFixHeight(value);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Private

    ///////////////////////////////////////////////////////////////////////////////////////////

    private void selectedFilterMenuItemChanged() {
        toggleGroup.flatMap(toggleGroup -> RichTableView.FilterMenuItem.fromToggle(toggleGroup.getSelectedToggle()))
                .ifPresent(filterMenuItem -> {
                    tooltip.setText(Res.get("component.standardTable.filter.tooltip", filterMenuItem.getTitle()));
                    filterMenu.setLabelAsContent(filterMenuItem.getTitle());
                });
    }

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
        UserProfileDisplay userProfileDisplay = new UserProfileDisplay();
        userProfileDisplay.setUserProfile(trader.getUserProfile());
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


    ///////////////////////////////////////////////////////////////////////////////////////////
    // FilterMenuItem

    ///////////////////////////////////////////////////////////////////////////////////////////

   /* @ToString
    @EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
    public static final class FilterMenuItem<MediationCaseListItem> extends DropdownBisqMenuItem implements Toggle {
        private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");

        public static FilterMenuItem<MediationCaseListItem> getShowAllFilterMenuItem(ToggleGroup toggleGroup) {
            return new MediationTableView.FilterMenuItem<>(toggleGroup, Res.get("component.standardTable.filter.showAll"), Optional.empty(), e -> true);
        }

        public static Optional<FilterMenuItem> fromToggle(Toggle selectedToggle) {
            if (selectedToggle == null) {
                return Optional.empty();
            }
            if (selectedToggle instanceof MediationTableView.FilterMenuItem) {
                try {
                    //noinspection unchecked
                    return Optional.of((MediationTableView.FilterMenuItem<MediationCaseListItem>) selectedToggle);
                } catch (ClassCastException e) {
                    log.error("Cast failed", e);
                    return Optional.empty();
                }
            }
            log.warn("Unexpected type: selectedToggle={}", selectedToggle);
            return Optional.empty();
        }

        @Getter
        private final String title;
        @EqualsAndHashCode.Include
        @Getter
        private final Optional<Object> data;
        @Getter
        private final Predicate<MediationCaseListItem> filter;
        private final ObjectProperty<ToggleGroup> toggleGroupProperty = new SimpleObjectProperty<>();
        private final BooleanProperty selectedProperty = new SimpleBooleanProperty();
        private final ChangeListener<Toggle> toggleChangeListener;

        public FilterMenuItem(ToggleGroup toggleGroup,
                              String title,
                              Optional<Object> data,
                              Predicate<MediationCaseListItem> filter) {
            this(toggleGroup, "check-white", "check-white", title, data, filter);
        }

        public FilterMenuItem(ToggleGroup toggleGroup,
                              String defaultIconId,
                              String activeIconId,
                              String title,
                              Optional<Object> data,
                              Predicate<MediationCaseListItem> filter) {
            super(defaultIconId, activeIconId, title);

            this.title = title;
            this.data = data;
            this.filter = filter;

            setToggleGroup(toggleGroup);
            getStyleClass().add("dropdown-menu-item");

            toggleChangeListener = (observable, oldValue, newValue) -> toggleChanged();
        }

        public void initialize() {
            getToggleGroup().selectedToggleProperty().addListener(toggleChangeListener);
            toggleChanged();
            setOnAction(e -> getToggleGroup().selectToggle(this));
            applyStyle();
        }

        public void dispose() {
            getToggleGroup().selectedToggleProperty().removeListener(toggleChangeListener);
            setOnAction(null);
        }


        ///////////////////////////////////////////////////////////////////////////////////////////
        // Toggle implementation

        ///////////////////////////////////////////////////////////////////////////////////////////

        @Override
        public ToggleGroup getToggleGroup() {
            return toggleGroupProperty.get();
        }

        @Override
        public void setToggleGroup(ToggleGroup toggleGroup) {
            toggleGroupProperty.set(toggleGroup);
        }

        @Override
        public ObjectProperty<ToggleGroup> toggleGroupProperty() {
            return toggleGroupProperty;
        }

        @Override
        public boolean isSelected() {
            return selectedProperty.get();
        }

        @Override
        public BooleanProperty selectedProperty() {
            return selectedProperty;
        }

        @Override
        public void setSelected(boolean selected) {
            selectedProperty.set(selected);
            applyStyle();
        }


        ///////////////////////////////////////////////////////////////////////////////////////////
        // Private

        ///////////////////////////////////////////////////////////////////////////////////////////

        private void toggleChanged() {
            setSelected(this.equals(getToggleGroup().getSelectedToggle()));
        }

        private void applyStyle() {
            getContent().pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, isSelected());
        }
    }*/
}
