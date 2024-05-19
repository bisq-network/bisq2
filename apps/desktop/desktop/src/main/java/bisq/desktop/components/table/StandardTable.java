package bisq.desktop.components.table;

import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.DropdownMenu;
import bisq.desktop.components.controls.DropdownMenuItem;
import bisq.desktop.components.controls.SearchBox;
import bisq.desktop.main.content.user.reputation.list.ReputationListView;
import bisq.i18n.Res;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.SortedList;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Convenience class for a standardized table view with a headline, num entries and support for filters.
 */
@Slf4j
@Getter
public class StandardTable<T> extends VBox {
    private final BisqTableView<T> tableView;
    private final Label headlineLabel;
    private final Optional<List<FilterMenuItem<T>>> filterItems;
    private final Optional<ToggleGroup> toggleGroup;
    private final Optional<Consumer<String>> searchTextHandler;
    private final Label numEntriesLabel;
    private final HBox headerBox;
    private final DropdownMenu filterMenu;
    private final ChangeListener<Toggle> toggleChangeListener;
    private final ListChangeListener<T> listChangeListener;
    private final BisqTooltip tooltip;
    private final SearchBox searchBox;
    private Subscription searchTextPin;


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

        this.filterItems = filterItems;
        this.toggleGroup = toggleGroup;
        this.searchTextHandler = searchTextHandler;

        headlineLabel = new Label(headline.orElse(""));
        headlineLabel.setManaged(headline.isPresent());
        headlineLabel.setVisible(headlineLabel.isManaged());
        headlineLabel.getStyleClass().add("standard-table-headline");

        tableView = new BisqTableView<>(sortedList);
        tableView.getStyleClass().add("standard-table-view");
        tableView.setMinHeight(200);

        numEntriesLabel = new Label();
        numEntriesLabel.getStyleClass().add("standard-table-num-entries");

        filterMenu = new DropdownMenu("chevron-drop-menu-grey", "chevron-drop-menu-white", false);
        filterMenu.setManaged(filterItems.isPresent());
        filterMenu.setVisible(filterMenu.isManaged());
        filterItems.ifPresent(filterMenu::addMenuItems);
        tooltip = new BisqTooltip();
        filterMenu.setTooltip(tooltip);

        searchBox = new SearchBox();
        searchBox.setManaged(searchTextHandler.isPresent());
        searchBox.setVisible(searchBox.isManaged());
        searchBox.setPrefWidth(90);
        HBox.setMargin(searchBox, new Insets(8, 0, 0, 0));
        HBox hBox = new HBox(20, searchBox, filterMenu);

        headerBox = new HBox(headlineLabel, Spacer.fillHBox(), hBox);
        HBox.setMargin(filterMenu, new Insets(0, 20, -7, 0));

        VBox.setMargin(headerBox, new Insets(0, 0, 10, 10));
        VBox.setMargin(numEntriesLabel, new Insets(5, 0, 0, 10));
        VBox.setVgrow(tableView, Priority.ALWAYS);
        getChildren().addAll(headerBox, tableView, numEntriesLabel);

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
        searchBox.setManaged(searchBox.isVisible());
    }

    public void dispose() {
        tableView.dispose();
        tableView.getItems().removeListener(listChangeListener);
        toggleGroup.ifPresent(toggleGroup -> toggleGroup.selectedToggleProperty().removeListener(toggleChangeListener));
        filterItems.ifPresent(filterItems -> filterItems.forEach(StandardTable.FilterMenuItem::dispose));
        if (searchTextPin != null) {
            searchTextPin.unsubscribe();
        }
    }

    public void resetSearch() {
        searchBox.clear();
    }

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
            getStyleClass().add("dropdown-filter-menu-item");

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

        private void toggleChanged() {
            setSelected(this.equals(getToggleGroup().getSelectedToggle()));
        }

        private void applyStyle() {
            getContent().pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, isSelected());
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
    }
}
