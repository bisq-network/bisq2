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

package bisq.desktop.components.table;

import bisq.common.encoding.Csv;
import bisq.common.facades.FacadeProvider;
import bisq.common.file.FileMutatorUtils;
import bisq.desktop.common.Layout;
import bisq.desktop.common.utils.FileChooserUtil;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.DropdownBisqMenuItem;
import bisq.desktop.components.controls.DropdownMenu;
import bisq.desktop.components.controls.SearchBox;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.navigation.NavigationTarget;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
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
 * Convenience class for a feature rich table view with a headline, search, num entries and support for filters.
 */
@Slf4j
@Getter
public class RichTableView<T> extends VBox {
    private final Optional<String> headline;
    private final Optional<List<FilterMenuItem<T>>> filterItems;
    private final Optional<ToggleGroup> toggleGroup;
    private final Optional<Consumer<String>> searchTextHandler;
    private final Label headlineLabel, numEntriesLabel;
    private final BisqTableView<T> tableView;
    private final DropdownMenu filterMenu;
    private final BisqTooltip tooltip;
    private final SearchBox searchBox;
    private final Button exportButton;
    private final ChangeListener<Toggle> toggleChangeListener;
    private final ListChangeListener<T> listChangeListener;
    private final String entriesUnit;
    private final HBox headerBox, subheaderBox;
    private final BisqMenuItem tableInfoMenuItem;
    private Subscription searchTextPin;
    @Setter
    private Optional<List<String>> csvHeaders = Optional.empty();
    @Setter
    private Optional<List<List<String>>> csvData = Optional.empty();
    private Optional<String> learnMoreTitle = Optional.empty();
    private Optional<String> learnMoreContent = Optional.empty();

    public RichTableView(ObservableList<T> observableList) {
        this(new SortedList<>(observableList));
    }

    public RichTableView(ObservableList<T> observableList, String headline) {
        this(new SortedList<>(observableList), Optional.of(headline), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public RichTableView(SortedList<T> sortedList) {
        this(sortedList, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public RichTableView(SortedList<T> sortedList, String headline, String entriesUnit, Consumer<String> searchTextHandler) {
        this(sortedList, Optional.of(headline), Optional.empty(), Optional.empty(), Optional.of(searchTextHandler), Optional.of(entriesUnit));
    }

    public RichTableView(SortedList<T> sortedList,
                         String headline,
                         List<FilterMenuItem<T>> filterItems,
                         ToggleGroup toggleGroup) {
        this(sortedList, Optional.of(headline), Optional.of(filterItems), Optional.of(toggleGroup), Optional.empty(), Optional.empty());
    }

    public RichTableView(SortedList<T> sortedList,
                         Consumer<String> searchTextHandler) {
        this(sortedList, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(searchTextHandler), Optional.empty());
    }

    public RichTableView(SortedList<T> sortedList,
                         String headline,
                         Consumer<String> searchTextHandler) {
        this(sortedList, Optional.of(headline), Optional.empty(), Optional.empty(), Optional.of(searchTextHandler), Optional.empty());
    }

    public RichTableView(SortedList<T> sortedList,
                         String headline,
                         List<FilterMenuItem<T>> filterItems,
                         ToggleGroup toggleGroup,
                         Consumer<String> searchTextHandler,
                         String entriesUnit) {
        this(sortedList, Optional.of(headline), Optional.of(filterItems), Optional.of(toggleGroup), Optional.of(searchTextHandler), Optional.of(entriesUnit));
    }

    public RichTableView(SortedList<T> sortedList,
                         List<FilterMenuItem<T>> filterItems,
                         ToggleGroup toggleGroup,
                         Consumer<String> searchTextHandler,
                         String entriesUnit) {
        this(sortedList, Optional.empty(), Optional.of(filterItems), Optional.of(toggleGroup), Optional.of(searchTextHandler), Optional.of(entriesUnit));
    }

    private RichTableView(SortedList<T> sortedList,
                          Optional<String> headline,
                          Optional<List<FilterMenuItem<T>>> filterItems,
                          Optional<ToggleGroup> toggleGroup,
                          Optional<Consumer<String>> searchTextHandler,
                          Optional<String> entriesUnit) {
        this.headline = headline;
        this.filterItems = filterItems;
        this.toggleGroup = toggleGroup;
        this.searchTextHandler = searchTextHandler;
        this.entriesUnit = entriesUnit.orElseGet(() -> Res.get("component.standardTable.entriesUnit.generic"));
        if (filterItems.isPresent()) {
            checkArgument(toggleGroup.isPresent(), "filterItems and toggleGroup must be both present or empty");
        }
        if (toggleGroup.isPresent()) {
            checkArgument(filterItems.isPresent(), "filterItems and toggleGroup must be both present or empty");
        }

        // Header: contains headline + num entries + export button
        headlineLabel = new Label(headline.orElse(""));
        headlineLabel.setManaged(headline.isPresent());
        headlineLabel.setVisible(headlineLabel.isManaged());
        headlineLabel.getStyleClass().add("bisq-easy-container-headline");

        numEntriesLabel = new Label();
        HBox.setMargin(numEntriesLabel, new Insets(0, 0, -5, 0));
        numEntriesLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text", "font-light");

        exportButton = new Button(Res.get("action.exportAsCsv"));
        exportButton.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
        exportButton.getStyleClass().addAll("export-button", "normal-text");

        // Show table info button
        tableInfoMenuItem = new BisqMenuItem("icon-help-grey", "icon-help-white");
        tableInfoMenuItem.setManaged(false);
        tableInfoMenuItem.setVisible(false);
        tableInfoMenuItem.setTooltip(Res.get("component.standardTable.tableInfo"));
        HBox.setMargin(tableInfoMenuItem, new Insets(0, 0, 0, 5));

        headerBox = new HBox(5, headlineLabel, numEntriesLabel, Spacer.fillHBox(), exportButton, tableInfoMenuItem);
        headerBox.getStyleClass().add("chat-container-header");

        VBox headerWithLineBox = new VBox(headerBox, Layout.hLine());
        if (headline.isEmpty()) {
            headerWithLineBox.setVisible(false);
            headerWithLineBox.setManaged(false);
        }

        // Subheader: contains search + filters
        searchBox = new SearchBox();
        searchBox.setManaged(searchTextHandler.isPresent());
        searchBox.setVisible(searchBox.isManaged());
        searchBox.setPrefWidth(200);

        filterMenu = new DropdownMenu("chevron-drop-menu-grey", "chevron-drop-menu-white", false);
        filterMenu.getStyleClass().add("dropdown-offer-rich-table-filter-menu");
        filterMenu.setManaged(filterItems.isPresent());
        filterMenu.setVisible(filterMenu.isManaged());
        filterItems.ifPresent(filterMenu::addMenuItems);
        tooltip = new BisqTooltip();
        filterMenu.setTooltip(tooltip);

        subheaderBox = new HBox(searchBox, Spacer.fillHBox(), filterMenu);
        subheaderBox.getStyleClass().add("rich-table-subheader");
        subheaderBox.setAlignment(Pos.CENTER);
        subheaderBox.setPadding(new Insets(0, 20, 0, 20));

        // TableView
        tableView = new BisqTableView<>(sortedList);
        tableView.getStyleClass().add("rich-table-view");
        tableView.setMinHeight(200);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        getChildren().addAll(headerWithLineBox, subheaderBox, tableView);
        VBox.setVgrow(this, Priority.ALWAYS);
        getStyleClass().add("rich-table-view-box");

        listChangeListener = c -> listItemsChanged();
        toggleChangeListener = (observable, oldValue, newValue) -> selectedFilterMenuItemChanged();
    }

    public void initialize() {
        tableView.initialize();
        tableView.getItems().addListener(listChangeListener);
        listItemsChanged();
        toggleGroup.ifPresent(toggleGroup -> toggleGroup.selectedToggleProperty().addListener(toggleChangeListener));
        selectedFilterMenuItemChanged();
        filterItems.ifPresent(filterItems -> filterItems.forEach(RichTableView.FilterMenuItem::initialize));
        searchTextHandler.ifPresent(stringConsumer -> searchTextPin = EasyBind.subscribe(searchBox.textProperty(), stringConsumer));
        exportButton.setOnAction(ev -> {
            List<String> headers = csvHeaders.orElseGet(() -> buildCsvHeaders());
            List<List<String>> data = csvData.orElseGet(() -> buildCsvData());
            String csv = Csv.toCsv(headers, data);
            String initialFileName = headline.orElse("Bisq-table-data") + ".csv";
            FileChooserUtil.saveFile(tableView.getScene(), initialFileName)
                    .ifPresent(filePath -> {
                        try {
                            FacadeProvider.getJdkFacade().writeString(csv, filePath);
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
        filterItems.ifPresent(filterItems -> filterItems.forEach(RichTableView.FilterMenuItem::dispose));
        if (searchTextPin != null) {
            searchTextPin.unsubscribe();
        }
        exportButton.setOnAction(null);
        tableInfoMenuItem.setOnAction(null);
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


    /* --------------------------------------------------------------------- */
    // TableView delegates
    /* --------------------------------------------------------------------- */

    public void refresh() {
        tableView.refresh();
    }

    public void sort() {
        if (!tableView.getSortOrder().isEmpty()) {
            tableView.sort();
        }
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

    public BisqTableColumn<T> getSelectionMarkerColumn() {
        return tableView.getSelectionMarkerColumn();
    }

    public TableView.TableViewSelectionModel<T> getSelectionModel() {
        return tableView.getSelectionModel();
    }

    public void setFixHeight(double value) {
        tableView.setFixHeight(value);
    }

    public void setTableInfo(String title, String content) {
        tableInfoMenuItem.setManaged(true);
        tableInfoMenuItem.setVisible(true);
        learnMoreTitle = Optional.of(title);
        learnMoreContent = Optional.of(content);
        tableInfoMenuItem.setOnAction(e -> openLearnMorePopup());
    }

    public void openLearnMorePopup() {
        if (learnMoreTitle.isPresent() && learnMoreContent.isPresent()) {
            Navigation.navigateTo(NavigationTarget.SHOW_TABLE_INFO,
                    new ShowTableInfo.InitData(learnMoreTitle.get(), learnMoreContent.get()));
        }
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void selectedFilterMenuItemChanged() {
        toggleGroup.flatMap(toggleGroup -> FilterMenuItem.fromToggle(toggleGroup.getSelectedToggle()))
                .ifPresent(filterMenuItem -> {
                    tooltip.setText(Res.get("component.standardTable.filter.tooltip", filterMenuItem.getTitle()));
                    filterMenu.setLabelAsContent(filterMenuItem.getTitle());
                });
    }

    private void listItemsChanged() {
        numEntriesLabel.setText(String.format("(%s %s)", tableView.getItems().size(), entriesUnit.toLowerCase()));
    }


    /* --------------------------------------------------------------------- */
    // FilterMenuItem
    /* --------------------------------------------------------------------- */

    @ToString
    @EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
    public static final class FilterMenuItem<T> extends DropdownBisqMenuItem implements Toggle {
        private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");

        public static <T> FilterMenuItem<T> getShowAllFilterMenuItem(ToggleGroup toggleGroup) {
            return new RichTableView.FilterMenuItem<>(toggleGroup, Res.get("component.standardTable.filter.showAll"), Optional.empty(), e -> true);
        }

        public static <T> Optional<FilterMenuItem<T>> fromToggle(Toggle selectedToggle) {
            if (selectedToggle == null) {
                return Optional.empty();
            }
            if (selectedToggle instanceof RichTableView.FilterMenuItem) {
                try {
                    //noinspection unchecked
                    return Optional.of((RichTableView.FilterMenuItem<T>) selectedToggle);
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


        /* --------------------------------------------------------------------- */
        // Toggle implementation
        /* --------------------------------------------------------------------- */

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


        /* --------------------------------------------------------------------- */
        // Private
        /* --------------------------------------------------------------------- */

        private void toggleChanged() {
            setSelected(this.equals(getToggleGroup().getSelectedToggle()));
        }

        private void applyStyle() {
            getContent().pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, isSelected());
        }
    }
}
