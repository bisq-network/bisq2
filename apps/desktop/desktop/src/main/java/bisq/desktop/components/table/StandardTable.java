package bisq.desktop.components.table;

import bisq.common.encoding.Csv;
import bisq.common.file.FileUtils;
import bisq.desktop.common.utils.FileChooserUtil;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.DropdownMenu;
import bisq.desktop.components.controls.DropdownMenuItem;
import bisq.desktop.components.controls.SearchBox;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.user.reputation.list.ReputationListView;
import bisq.i18n.Res;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Convenience class for a standardized table view with a headline, num entries and support for filters.
 */
@Slf4j
@Getter
public class StandardTable<T> extends VBox {
    private final Optional<String> headline;
    private final Optional<List<FilterMenuItem<T>>> filterItems;
    private final Optional<ToggleGroup> toggleGroup;
    private final Optional<Consumer<String>> searchTextHandler;
    private final Label headlineLabel, numEntriesLabel;
    private final BisqTableView<T> tableView;
    private final DropdownMenu filterMenu;
    private final BisqTooltip tooltip;
    private final SearchBox searchBox;
    private final Hyperlink exportHyperlink;
    private final ChangeListener<Toggle> toggleChangeListener;
    private final ListChangeListener<T> listChangeListener;
    private Subscription searchTextPin;
    @Setter
    private Optional<List<String>> csvHeaders = Optional.empty();
    @Setter
    private Optional<List<List<String>>> csvData = Optional.empty();

    public StandardTable(SortedList<T> sortedList) {
        this(sortedList, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public StandardTable(SortedList<T> sortedList, String headline) {
        this(sortedList, Optional.of(headline), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public StandardTable(SortedList<T> sortedList,
                         String headline,
                         List<FilterMenuItem<T>> filterItems,
                         ToggleGroup toggleGroup) {
        this(sortedList, Optional.of(headline), Optional.of(filterItems), Optional.of(toggleGroup), Optional.empty());
    }

    public StandardTable(SortedList<T> sortedList,
                         String headline,
                         List<FilterMenuItem<T>> filterItems,
                         ToggleGroup toggleGroup,
                         Consumer<String> searchTextHandler) {
        this(sortedList, Optional.of(headline), Optional.of(filterItems), Optional.of(toggleGroup), Optional.of(searchTextHandler));
    }

    private StandardTable(SortedList<T> sortedList,
                          Optional<String> headline,
                          Optional<List<FilterMenuItem<T>>> filterItems,
                          Optional<ToggleGroup> toggleGroup,
                          Optional<Consumer<String>> searchTextHandler) {
        this.headline = headline;
        this.filterItems = filterItems;
        this.toggleGroup = toggleGroup;
        this.searchTextHandler = searchTextHandler;
        if (filterItems.isPresent()) {
            checkArgument(toggleGroup.isPresent(), "filterItems and toggleGroup must be both present or empty");
        }
        if (toggleGroup.isPresent()) {
            checkArgument(filterItems.isPresent(), "filterItems and toggleGroup must be both present or empty");
        }

        headlineLabel = new Label(headline.orElse(""));
        headlineLabel.setManaged(headline.isPresent());
        headlineLabel.setVisible(headlineLabel.isManaged());
        headlineLabel.getStyleClass().add("standard-table-headline");
        headlineLabel.setAlignment(Pos.BASELINE_LEFT);

        tableView = new BisqTableView<>(sortedList);
        tableView.getStyleClass().add("standard-table-view");
        tableView.setMinHeight(200);

        filterMenu = new DropdownMenu("chevron-drop-menu-grey", "chevron-drop-menu-white", false);
        filterMenu.setManaged(filterItems.isPresent());
        filterMenu.setVisible(filterMenu.isManaged());
        filterItems.ifPresent(filterMenu::addMenuItems);
        tooltip = new BisqTooltip();
        filterMenu.setTooltip(tooltip);
        filterMenu.setAlignment(Pos.BASELINE_LEFT);

        searchBox = new SearchBox();
        searchBox.setManaged(searchTextHandler.isPresent());
        searchBox.setVisible(searchBox.isManaged());
        searchBox.setPrefWidth(90);
        searchBox.setAlignment(Pos.BASELINE_LEFT);
        HBox.setMargin(filterMenu, new Insets(0, 20, 0, 0));
        HBox filterBox = new HBox(20, searchBox, filterMenu);
        filterBox.setAlignment(Pos.BASELINE_LEFT);

        HBox headerBox = new HBox(headlineLabel, Spacer.fillHBox(), filterBox);
        headerBox.setAlignment(Pos.BASELINE_LEFT);

        numEntriesLabel = new Label();
        numEntriesLabel.getStyleClass().add("standard-table-num-entries");
        numEntriesLabel.setAlignment(Pos.BASELINE_LEFT);

        exportHyperlink = new Hyperlink(Res.get("action.exportAsCsv"));
        exportHyperlink.getStyleClass().add("standard-table-num-entries");
        exportHyperlink.setAlignment(Pos.BASELINE_LEFT);

        HBox.setMargin(exportHyperlink, new Insets(8, 10, 0, 0));
        HBox footerHBox = new HBox(numEntriesLabel, Spacer.fillHBox(), exportHyperlink);
        footerHBox.setAlignment(Pos.BASELINE_LEFT);

        VBox.setMargin(headerBox, new Insets(0, 0, 5, 10));
        VBox.setVgrow(tableView, Priority.ALWAYS);
        VBox.setMargin(footerHBox, new Insets(0, 0, 0, 10));
        getChildren().addAll(headerBox, tableView, footerHBox);

        listChangeListener = c -> listItemsChanged();
        toggleChangeListener = (observable, oldValue, newValue) -> selectedFilterMenuItemChanged();
    }

    public void initialize() {
        tableView.initialize();
        tableView.getItems().addListener(listChangeListener);
        listItemsChanged();
        toggleGroup.ifPresent(toggleGroup -> toggleGroup.selectedToggleProperty().addListener(toggleChangeListener));
        selectedFilterMenuItemChanged();
        filterItems.ifPresent(filterItems -> filterItems.forEach(StandardTable.FilterMenuItem::initialize));
        searchTextHandler.ifPresent(stringConsumer -> searchTextPin = EasyBind.subscribe(searchBox.textProperty(), stringConsumer));
        exportHyperlink.setOnAction(ev -> {
            List<String> headers = csvHeaders.orElse(buildCsvHeaders());
            List<List<String>> data = csvData.orElse(buildCsvData());
            String csv = Csv.toCsv(headers, data);
            String initialFileName = headline.orElse("Bisq-table-data") + ".csv";
            FileChooserUtil.saveFile(tableView.getScene(), initialFileName)
                    .ifPresent(file -> {
                        try {
                            FileUtils.writeToFile(csv, file);
                        } catch (IOException e) {
                            new Popup().error(e).show();
                        }
                    });
        });
    }

    public void dispose() {
        tableView.dispose();
        tableView.getItems().removeListener(listChangeListener);
        toggleGroup.ifPresent(toggleGroup -> toggleGroup.selectedToggleProperty().removeListener(toggleChangeListener));
        filterItems.ifPresent(filterItems -> filterItems.forEach(StandardTable.FilterMenuItem::dispose));
        if (searchTextPin != null) {
            searchTextPin.unsubscribe();
        }
        exportHyperlink.setOnAction(null);
    }

    public void resetSearch() {
        searchBox.clear();
    }

    public Stream<BisqTableColumn<T>> getBisqTableColumnsForCsv() {
        return tableView.getColumns().stream()
                .filter(column -> column instanceof BisqTableColumn)
                .map(column -> {
                    @SuppressWarnings("unchecked")
                    BisqTableColumn<T> bisqTableColumn = (BisqTableColumn<T>) column;
                    return bisqTableColumn;
                })
                .filter(TableColumnBase::isVisible)
                .filter(BisqTableColumn::isIncludeForCsv);
    }

    public List<String> buildCsvHeaders() {
        return getBisqTableColumnsForCsv()
                .map(BisqTableColumn::getHeaderForCsv)
                .collect(Collectors.toList());
    }

    public List<List<String>> buildCsvData() {
        return tableView.getItems().stream()
                .map(item -> getBisqTableColumnsForCsv()
                        .map(bisqTableColumn -> bisqTableColumn.resolveValueForCsv(item))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // TableView delegates
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void refresh() {
        tableView.refresh();
    }

    public ObservableList<TableColumn<T, ?>> getSortOrder() {
        return tableView.getSortOrder();
    }

    public ObservableList<T> getItems() {
        return tableView.getItems();
    }

    public ObservableList<TableColumn<T, ?>> getColumns() {
        return tableView.getColumns();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void selectedFilterMenuItemChanged() {
        toggleGroup.flatMap(toggleGroup -> FilterMenuItem.fromToggle(toggleGroup.getSelectedToggle()))
                .ifPresent(filterMenuItem -> {
                    tooltip.setText(Res.get("component.standardTable.filter.tooltip", filterMenuItem.getTitle()));
                    filterMenu.setLabel(filterMenuItem.getTitle());
                });
    }

    private void listItemsChanged() {
        numEntriesLabel.setText(Res.get("component.standardTable.numEntries", tableView.getItems().size()));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // FilterMenuItem
    ///////////////////////////////////////////////////////////////////////////////////////////

    @ToString
    @EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
    public static final class FilterMenuItem<T> extends DropdownMenuItem implements Toggle {
        private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");

        public static FilterMenuItem<ReputationListView.ListItem> getShowAllFilterMenuItem(ToggleGroup toggleGroup) {
            return new StandardTable.FilterMenuItem<>(toggleGroup, Res.get("component.standardTable.filter.showAll"), Optional.empty(), e -> true);
        }

        public static Optional<FilterMenuItem<ReputationListView.ListItem>> fromToggle(Toggle selectedToggle) {
            if (selectedToggle == null) {
                return Optional.empty();
            }
            if (selectedToggle instanceof StandardTable.FilterMenuItem) {
                try {
                    //noinspection unchecked
                    return Optional.of((StandardTable.FilterMenuItem<ReputationListView.ListItem>) selectedToggle);
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
        private final Predicate<T> filter;
        private final ObjectProperty<ToggleGroup> toggleGroupProperty = new SimpleObjectProperty<>();
        private final BooleanProperty selectedProperty = new SimpleBooleanProperty();
        private final ChangeListener<Toggle> toggleChangeListener;

        public FilterMenuItem(ToggleGroup toggleGroup, String title, Optional<Object> data, Predicate<T> filter) {
            this(toggleGroup, "check-white", "check-white", title, data, filter);
        }

        public FilterMenuItem(ToggleGroup toggleGroup,
                              String defaultIconId,
                              String activeIconId,
                              String title,
                              Optional<Object> data,
                              Predicate<T> filter) {
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
    }
}
