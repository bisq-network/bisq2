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

package bisq.desktop.components.controls;

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.validator.ValidatorBase;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polygon;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class AutoCompleteComboBox<T> extends ComboBox<T> {
    protected final String description;
    @Nullable
    protected final String prompt;
    protected List<? extends T> list;
    protected List<? extends T> extendedList;
    protected List<T> matchingList;
    protected Skin<T> skin;
    protected TextInputControl editor;
    @Getter
    private final BooleanProperty isValidSelection;

    public AutoCompleteComboBox() {
        this(FXCollections.observableArrayList());
    }

    public AutoCompleteComboBox(ObservableList<T> items) {
        this(items, "", "");
    }

    public AutoCompleteComboBox(ObservableList<T> items, String description) {
        this(items, description, null);
    }

    public AutoCompleteComboBox(ObservableList<T> items, String description, @Nullable String prompt) {
        super(items);
        this.description = description;
        this.prompt = prompt;
        this.isValidSelection = new SimpleBooleanProperty(true);

        createDefaultSkin();

        setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    if (editor.getText() != null) {
                        skin.getMaterialTextField().update();
                        skin.getMaterialTextField().getDescriptionLabel().setLayoutY(6.5);
                        editor.setText(asString(item));
                    }
                    if (getParent() != null) {
                        getParent().requestFocus();
                    }
                } else {
                    editor.setText("");
                }
            }
        });
        setAutocompleteItems(items);
        registerKeyHandlers();
        reactToQueryChanges();

        // Text input gets focus when added to stage (not clear why...)
        // This prevents that the list gets opened and steals the focus
        setupFocusHandler();

        // todo (investigate, low prio) does not update items when we change list so add a handler here for a quick fix
        // need to figure out why its not updating
        items.addListener(new WeakReference<>((ListChangeListener<T>) c -> setAutocompleteItems(items)).get());
    }

    public void validateOnNoItemSelectedWithMessage(String message) {
        skin.materialTextField.setValidators(new ValidatorBase(message) {
            @Override
            protected void eval() {
                hasErrors.set(getSelectionModel().getSelectedItem() == null);
                isValidSelection.set(!hasErrors.get());
            }
        });
    }

    public boolean validate() {
        return skin.materialTextField.validate();
    }

    public void resetValidation() {
        skin.materialTextField.resetValidation();
    }

    public Skin<T> getAutoCompleteComboBoxSkin() {
        return skin;
    }

    public TextInputControl getEditorTextField() {
        return editor;
    }

    @Override
    protected double computeMinHeight(double width) {
        // MaterialField height
        return 56;
    }

    @Override
    protected javafx.scene.control.Skin<?> createDefaultSkin() {
        if (skin == null) {
            skin = new Skin<>(this, description, prompt);
            editor = skin.getMaterialTextField().getTextInputControl();
        }
        return skin;
    }

    public void setDescription(String description) {
        skin.setDescription(description);
    }

    public final StringProperty descriptionProperty() {
        return skin.descriptionProperty();
    }

    /**
     * Set the complete list of ComboBox items. Use this instead of setItems().
     */
    public void setAutocompleteItems(List<? extends T> items, List<? extends T> allItems) {
        list = items;
        extendedList = allItems;
        matchingList = new ArrayList<>(list);
        setValue(null);
        getSelectionModel().clearSelection();
        setItems(FXCollections.observableList(matchingList));
        editor.setText("");
    }

    public void setAutocompleteItems(List<? extends T> items) {
        setAutocompleteItems(items, null);
    }

    /**
     * Triggered when value change is *confirmed*. In practical terms
     * this is when user clicks item on the dropdown or hits [ENTER]
     * while typing in the text.
     * <p>
     * This is in contrast to onAction event that is triggered
     * on every (unconfirmed) value change. The onAction is not really
     * suitable for the search enabled ComboBox.
     */
    public final void setOnChangeConfirmed(EventHandler<Event> eventHandler) {
        if (eventHandler == null) {
            return;
        }
        setOnHidden(e -> {
            String inputText = editor.getText();
            // Case 1: fire if input text selects (matches) an item
            String selectedItemAsString = getConverter().toString(getSelectionModel().getSelectedItem());
            if (selectedItemAsString != null && selectedItemAsString.equals(inputText)) {
                eventHandler.handle(e);
                getParent().requestFocus();
                return;
            }

            // Case 2: fire if the text is empty to support special "show all" case
            if (inputText.isEmpty()) {
                eventHandler.handle(e);
                getParent().requestFocus();
            }
        });
    }

    // Clear selection and query when ComboBox gets new focus. This is usually what user
    // wants - to have a blank slate for a new search. The primary motivation though
    // was to work around UX glitches related to (starting) editing text when comboBox
    // had specific item selected.
    protected void setupFocusHandler() {
        editor.focusedProperty().addListener(new WeakReference<>((ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
            if (!oldValue && newValue) {
                removeFilter();
                forceRedraw();
            }
        }).get());
    }

    protected void registerKeyHandlers() {
        // By default, pressing [SPACE] caused editor text to reset. The solution
        // is to suppress relevant event on the underlying ListViewSkin.
        EventHandler<KeyEvent> weakEventFilter = new WeakReference<>((EventHandler<KeyEvent>) event -> {
            if (event.getCode() == KeyCode.SPACE) {
                event.consume();
            }
        }).get();
        if (weakEventFilter != null) {
            skin.getPopupContent().addEventFilter(KeyEvent.ANY, weakEventFilter);
        }

        EventHandler<KeyEvent> weakEventFilter2 = new WeakReference<>((EventHandler<KeyEvent>) event -> {
            if (event.getCode() == KeyCode.DOWN ||
                    event.getCode() == KeyCode.UP ||
                    event.getCode() == KeyCode.ENTER) {
                event.consume();
            }
        }).get();
        if (weakEventFilter2 != null) {
            skin.materialTextField.addEventFilter(KeyEvent.ANY, weakEventFilter2);
        }
    }

    protected void filterBy(String query) {
        matchingList = (extendedList != null && !query.isEmpty() ? extendedList : list)
                .stream()
                .filter(item -> asString(item).toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());

        setValue(null);
        getSelectionModel().clearSelection();
        setItems(FXCollections.observableList(matchingList));
        int pos = editor.getCaretPosition();
        if (pos > query.length()) {
            pos = query.length();
        }
        editor.setText(query);
        editor.positionCaret(pos);
    }

    protected void reactToQueryChanges() {
        EventHandler<KeyEvent> weakEventHandler = new WeakReference<>((EventHandler<KeyEvent>) event ->
                UIThread.runOnNextRenderFrame(() -> {
                    String query = editor.getText();
                    boolean exactMatch = list.stream().anyMatch(item -> asString(item).equalsIgnoreCase(query));
                    if (!exactMatch) {
                        if (query.isEmpty()) {
                            removeFilter();
                        } else {
                            filterBy(query);
                        }
                        forceRedraw();
                    }
                })).get();
        if (weakEventHandler != null) {
            editor.addEventFilter(KeyEvent.KEY_RELEASED, weakEventHandler);
        }
    }

    protected void removeFilter() {
        matchingList = new ArrayList<>(list);
        setValue(null);
        getSelectionModel().clearSelection();
        setItems(FXCollections.observableList(matchingList));
        editor.setText("");
    }

    public void forceRedraw() {
        adjustVisibleRowCount();
        if (matchingListSize() > 0) {
            skin.getPopupContent().autosize();
            UIThread.runOnNextRenderFrame(skin::layoutListView);
            show();
        } else {
            hide();
        }
    }

    protected void adjustVisibleRowCount() {
        setVisibleRowCount(Math.min(10, matchingListSize()));
    }

    protected String asString(T item) {
        return getConverter().toString(item);
    }

    protected int matchingListSize() {
        return matchingList.size();
    }

    @Getter
    @Slf4j
    public static class Skin<T> extends ComboBoxListViewSkin<T> {
        protected final static double DEFAULT_ARROW_X_L = 17;
        protected final static double DEFAULT_ARROW_X_M = 26.5;
        protected final static double DEFAULT_ARROW_X_R = 36;
        protected final MaterialTextField materialTextField;
        protected final ImageView arrow;
        protected final Polygon listBackground = new Polygon();
        protected final ObservableList<T> items;
        protected final AutoCompleteComboBox<T> comboBox;
        protected final Pane buttonPane;
        private final DropShadow dropShadow;
        protected ListView<T> listView;

        protected double arrowX_l = DEFAULT_ARROW_X_L;
        protected double arrowX_m = DEFAULT_ARROW_X_M;
        protected double arrowX_r = DEFAULT_ARROW_X_R;
        @Setter
        private boolean hideArrow;

        public Skin(ComboBox<T> control, String description, @Nullable String prompt) {
            super(control);
            comboBox = (AutoCompleteComboBox<T>) control;
            items = comboBox.getItems();

            if (prompt == null) {
                materialTextField = new MaterialTextField(description);
            } else {
                materialTextField = new MaterialTextField(description, prompt);
            }
            materialTextField.setStyle("-fx-background-color: transparent");
            arrow = ImageUtil.getImageViewById("arrow-down");
            arrow.setLayoutY(22);
            arrow.setMouseTransparent(true);

            buttonPane = new Pane();
            buttonPane.getChildren().setAll(materialTextField, arrow);
            getChildren().setAll(buttonPane);
            buttonPane.autosize();

            // The list pane has a 5x left and right padding. To not exceed clipping area we use dropShadow
            // of 5 as well. Design had 25 but that would complicate layouts where comboBoxes would require 
            // larger paddings in containers or some adoptions to clipping areas...

            dropShadow = new DropShadow();
            dropShadow.setBlurType(BlurType.GAUSSIAN);
            dropShadow.setRadius(25);
            dropShadow.setSpread(0.65);
            dropShadow.setColor(Color.rgb(0, 0, 0, 0.4));

            listBackground.setFill(Paint.valueOf("#212121"));
            listBackground.setEffect(dropShadow);
        }

        @Override
        protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
            // another hack...
            double computed = super.computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
            return Math.max(computed, buttonPane.getHeight());
        }

        protected void setDescription(String description) {
            materialTextField.setDescription(description);
        }

        public final StringProperty descriptionProperty() {
            return materialTextField.descriptionProperty();
        }

        @Override
        public Node getPopupContent() {
            // Hack to get listView from base class and put it into a container with the listBackground below.
            if (listView == null) {
                Node node = super.getPopupContent();
                if (node instanceof ListView) {
                    //noinspection unchecked,rawtypes
                    listView = (ListView) node;
                    listView.setId("bisq-combo-box-list-view");

                    // Hack to get access to background and insert our polygon
                    listView.parentProperty().addListener(new WeakReference<>(
                            (ChangeListener<Parent>) (observable, oldValue, newValue) -> {
                                if (newValue != null && listView.getParent() != null) {
                                    Parent rootPopup = listView.getParent().getParent();
                                    if (rootPopup instanceof Pane && ((Pane) rootPopup).getChildren().size() == 1) {
                                        ((Pane) rootPopup).getChildren().add(0, listBackground);
                                    }
                                }
                            }).get());
                } else {
                    log.error("Node is expected to be of type ListView. node={}", node);
                }
            }
            return listView;
        }

        @Override
        protected void layoutChildren(final double x, final double y,
                                      final double w, final double h) {
            super.layoutChildren(x, y, w, h);

            arrow.setLayoutX(w - 22);
            materialTextField.setPrefWidth(w);
            layoutListView();
        }

        protected void layoutListView() {
            if (items.isEmpty()) {
                listBackground.getPoints().clear();
            } else {
                double x = 0;
                double listOffset = hideArrow ? -17.5 : 7.5;
                double listWidthOffset = 0;
                // relative to visible top-left point 
                double height = Math.min(comboBox.getVisibleRowCount(), items.size()) * getRowHeight() + listOffset;
                double width = comboBox.getWidth() - listWidthOffset;
                double y = materialTextField.getHeight() - 35;
                double arrowY_m = y - 7.5;
                if (hideArrow) {
                    listBackground.getPoints().setAll(
                            x, y,
                            x + width, y,
                            x + width, y + height,
                            x, y + height);
                } else {
                    listBackground.getPoints().setAll(
                            x, y,
                            x + arrowX_l, y,
                            x + arrowX_m, arrowY_m,
                            x + arrowX_r, y,
                            x + width, y,
                            x + width, y + height,
                            x, y + height);
                }

                listBackground.setLayoutX(0);
                buttonPane.setLayoutX(0);
                listView.setLayoutX(listWidthOffset / 2);
                listView.setLayoutY(y + listOffset);
                listView.setPrefWidth(width);
                listView.setPrefHeight(height - listOffset + 2);
                listView.autosize();
            }
        }

        protected int getRowHeight() {
            return 40;
        }

        public void setDropShadowColor(Color color) {
            dropShadow.setColor(color);
        }
    }
}