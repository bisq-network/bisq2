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
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
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
import javafx.scene.control.TextField;
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
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class AutoCompleteComboBox<T> extends ComboBox<T> {
    private List<? extends T> list;
    private List<? extends T> extendedList;
    private List<T> matchingList;
    private final Skin<T> skin;
    private final TextField editor;

    public AutoCompleteComboBox() {
        this(FXCollections.observableArrayList());
    }

    public AutoCompleteComboBox(ObservableList<T> items) {
        this(items, "", "");
    }

    public AutoCompleteComboBox(ObservableList<T> items, String description) {
        this(items, description, "");
    }

    public AutoCompleteComboBox(ObservableList<T> items, String description, String prompt) {
        super(items);

        skin = new Skin<>(this, description, prompt);
        editor = skin.textInputBox.getInputTextField();
        // setPadding(new Insets(0, 20, 0, 20));
        setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    editor.setText(item.toString());
                    if (getParent() != null) {
                        getParent().requestFocus();
                    }
                } else {
                    editor.setText("");
                }
            }
        });
        setAutocompleteItems(items);
        clearOnFocus();
        registerKeyHandlers();
        reactToQueryChanges();

        //todo does not update items when we change list so add a handler here for a quick fix
        // need to figure out why its not updating and if we leave that remove the handler or make it weak reference
        items.addListener(new ListChangeListener<T>() {
            @Override
            public void onChanged(Change<? extends T> c) {
                setAutocompleteItems(items);
            }
        });
    }

    public void setDescription(String description) {
        skin.setDescription(description);
    }

    public final StringProperty descriptionProperty() {
        return skin.descriptionProperty();
    }

    @Override
    protected ComboBoxListViewSkin<?> createDefaultSkin() {
        return skin;
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
    public final void setOnChangeConfirmed(EventHandler<Event> eh) {
        setOnHidden(e -> {
            String inputText = editor.getText();

            // Case 1: fire if input text selects (matches) an item
            T selectedItem = getSelectionModel().getSelectedItem();
            T inputTextItem = getConverter().fromString(inputText);
            if (selectedItem != null && selectedItem.equals(inputTextItem)) {
                eh.handle(e);
                getParent().requestFocus();
                return;
            }

            // Case 2: fire if the text is empty to support special "show all" case
            if (inputText.isEmpty()) {
                eh.handle(e);
                getParent().requestFocus();
            }
        });
    }

    // Clear selection and query when ComboBox gets new focus. This is usually what user
    // wants - to have a blank slate for a new search. The primary motivation though
    // was to work around UX glitches related to (starting) editing text when combobox
    // had specific item selected.
    private void clearOnFocus() {
        editor.focusedProperty().addListener((observableValue, hadFocus, hasFocus) -> {
            if (!hadFocus && hasFocus) {
                removeFilter();
                forceRedraw();
            }
        });
    }

    private void registerKeyHandlers() {
        // By default pressing [SPACE] caused editor text to reset. The solution
        // is to suppress relevant event on the underlying ListViewSkin.
        skin.getPopupContent().addEventFilter(KeyEvent.ANY, (KeyEvent event) -> {
            if (event.getCode() == KeyCode.SPACE)
                event.consume();
        });

        skin.textInputBox.addEventFilter(KeyEvent.ANY, (KeyEvent event) -> {
            if (event.getCode() == KeyCode.DOWN ||
                    event.getCode() == KeyCode.UP ||
                    event.getCode() == KeyCode.ENTER) {
                event.consume();
            }
        });
    }

    private void filterBy(String query) {
        matchingList = (extendedList != null && query.length() > 0 ? extendedList : list)
                .stream()
                .filter(item -> item.toString().toLowerCase().contains(query.toLowerCase()))
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

    private void reactToQueryChanges() {
        editor.addEventHandler(KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
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
            });
        });
    }

    private void removeFilter() {
        matchingList = new ArrayList<>(list);
        setValue(null);
        getSelectionModel().clearSelection();
        setItems(FXCollections.observableList(matchingList));
        editor.setText("");
    }

    private void forceRedraw() {
        adjustVisibleRowCount();
        if (matchingListSize() > 0) {
            skin.getPopupContent().autosize();
            UIThread.runOnNextRenderFrame(skin::layoutListView);
            show();
        } else {
            hide();
        }
    }

    private void adjustVisibleRowCount() {
        setVisibleRowCount(Math.min(10, matchingListSize()));
    }

    private String asString(T item) {
        return getConverter().toString(item);
    }

    private int matchingListSize() {
        return matchingList.size();
    }


    private static class Skin<T> extends ComboBoxListViewSkin<T> {
        private final TextInputBox textInputBox;
        private final ImageView arrow;
        private final Polygon listBackground = new Polygon();
        private final ObservableList<T> items;
        private final ComboBox<T> comboBox;
        private final Pane buttonPane;
        private ListView<T> listView;


        public Skin(ComboBox<T> control, String description, String prompt) {
            super(control);
            textInputBox = new TextInputBox(description, prompt);
            comboBox = control;

            arrow = ImageUtil.getImageViewById("arrow-down");
            arrow.setLayoutY(22);
            arrow.setMouseTransparent(true);

            buttonPane = new Pane();
            buttonPane.getChildren().setAll(textInputBox, arrow);
            getChildren().setAll(buttonPane);

            // The list pane has a 5x left and right padding. To not exceed clipping area we use dropshadow
            // of 5 as well. Design had 25 but that would complicate layouts where comboBoxes would require 
            // larger paddings in containers or some adoptions to clipping areas...

            // todo: 5px does not look good, so leave it to 25 and wait for designer to make decision. Maybe we need
            // update layouts then...
            DropShadow dropShadow = new DropShadow();
            dropShadow.setBlurType(BlurType.GAUSSIAN);
            dropShadow.setRadius(25);
            dropShadow.setSpread(0.65);
            dropShadow.setColor(Color.rgb(0, 0, 0, 0.4));

            listBackground.setFill(Paint.valueOf("#212121"));
            listBackground.setEffect(dropShadow);
            listBackground.setManaged(false);
            listBackground.setVisible(false);

            items = comboBox.getItems();
            comboBox.setOnShown(e -> {
                listBackground.setManaged(true);
                listBackground.setVisible(true);
            });
            comboBox.setOnHidden(e -> {
                listBackground.setManaged(false);
                listBackground.setVisible(false);
            });
        }

        @Override
        protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset) {
            // another hack...
            double computed = super.computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
            return Math.max(computed, textInputBox.getHeight());
        }

        private void setDescription(String description) {
            textInputBox.setDescription(description);
        }

        public final StringProperty descriptionProperty() {
            return textInputBox.descriptionProperty();
        }

        //todo does not work as we lose the key navigation support when listView is packed into a pane

        @Override
        public Node getPopupContent() {
            // Hack to get listView from base class and put it into a container with the listBackground below.
            if (this.listView == null) {
                Node node = super.getPopupContent();
                if (node instanceof ListView _listView) {
                    this.listView = _listView;

                    this.listView.getStyleClass().add("bisq-combo-box-list-view");

                    // Hack to get access to background and insert our polygon
                    //todo make weak reference or remove listener
                    listView.parentProperty().addListener(new ChangeListener<Parent>() {
                        @Override
                        public void changed(ObservableValue<? extends Parent> observable, Parent oldValue, Parent newValue) {
                            if (newValue != null && listView.getParent() != null) {
                                Parent rootPopup = listView.getParent().getParent();
                                log.error("rootPopup "+ ((Pane)rootPopup).getChildren().size());
                                if (rootPopup instanceof Pane pane && pane.getChildren().size() == 1) {
                                    pane.getChildren().add(0, listBackground);
                                }
                            }
                        }
                    });
                } else {
                    throw new RuntimeException("node expected to be ListView");
                }
            }
            return listView;
        }

        @Override
        protected void layoutChildren(final double x, final double y,
                                      final double w, final double h) {
            super.layoutChildren(x, y, w, h);
            arrow.setLayoutX(w - 22);
            textInputBox.setPrefWidth(w);
            layoutListView();
        }

        private static final double x = 5;
        private static final double listOffset = 8;
        // relative to visible top-left point 
        private static final double arrowX_l = 22;
        private static final double arrowX_m = 31.5;
        private static final double arrowX_r = 41;

        private void layoutListView() {
            if (items.isEmpty()) {
                listBackground.getPoints().clear();
            } else {
                double height = Math.min(comboBox.getVisibleRowCount(), items.size()) * 40 + listOffset;
                double width = comboBox.getWidth() - 10;
                double y = textInputBox.getHeight() - 25;
                double arrowY_m = y - 7.5;
                listBackground.getPoints().setAll(
                        x, y,
                        x + arrowX_l, y,
                        x + arrowX_m, arrowY_m,
                        x + arrowX_r, y,
                        x + width, y,
                        x + width, y + height,
                        x, y + height);

                listBackground.setLayoutX(0);
                buttonPane.setLayoutX(0);
                listView.setLayoutX(5);
                listView.setLayoutY(y + listOffset);
                listView.setPrefWidth(width);
                listView.setPrefHeight(height - listOffset + 2);
                listView.autosize();
            }
        }
    }

}