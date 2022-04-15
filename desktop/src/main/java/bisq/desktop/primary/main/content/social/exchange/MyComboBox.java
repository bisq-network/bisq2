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

package bisq.desktop.primary.main.content.social.exchange;

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.TextInputBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
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
public class MyComboBox<T> extends ComboBox<T> {
    private List<? extends T> list;
    private List<? extends T> extendedList;
    private List<T> matchingList;
    private final Skin<T> skin = new Skin<>(this);
    private final TextField editor;

    private static class Skin<T> extends ComboBoxListViewSkin<T> {
        private static final double x = 25;
        // private static final double y = 25;
        private static final double listOffset = 8;
        // relative to visible top-left point 
        private static final double arrowX_l = 22;
        private static final double arrowX_m = 31.5;
        // private static final double arrowY_m = 17.5;
        private static final double arrowX_r = 41;

        private final TextInputBox textInputBox;
        private final ImageView arrow;
        private final Polygon listBackground = new Polygon();
        private final ObservableList<T> items;
        private final ComboBox<T> comboBox;
        private final Pane background;
        private ListView listView;

        public Skin(ComboBox<T> control) {
            super(control);
            textInputBox = new TextInputBox("desc", "promp");
            comboBox = control;

            arrow = ImageUtil.getImageViewById("arrow-down");
            arrow.setLayoutY(22);
            arrow.setMouseTransparent(true);

            background = new Pane();
            background.getChildren().setAll(textInputBox, arrow, listBackground);
            getChildren().setAll(background);

            Node node = getPopupContent();
            if (node instanceof ListView listView) {
                this.listView = listView;
                listView.getStyleClass().add("bisq-combo-box-list-view");
            }

            DropShadow dropShadow = new DropShadow();
            dropShadow.setBlurType(BlurType.GAUSSIAN);
            dropShadow.setHeight(25);
            dropShadow.setWidth(25);
            dropShadow.setRadius(25);
            dropShadow.setSpread(0.5);
            dropShadow.setColor(Color.rgb(0, 0, 0, 0.3));

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
        protected void layoutChildren(final double x, final double y,
                                      final double w, final double h) {
            super.layoutChildren(x, y, w, h);
            arrow.setLayoutX(w - 22);
            textInputBox.setPrefWidth(w);
            layoutListView();
        }

        private void layoutListView() {
            if (items.isEmpty()) {
                listBackground.getPoints().clear();
            } else {
                double height = Math.min(comboBox.getVisibleRowCount(), items.size()) * 40 + listOffset;
                double width = comboBox.getWidth() -  comboBox.getPadding().getLeft() -  comboBox.getPadding().getRight() - 10;
                double y = textInputBox.getHeight() + 25;
                double arrowY_m = y - 7.5;
                listBackground.getPoints().setAll(
                        x, y,
                        x + arrowX_l, y,
                        x + arrowX_m, arrowY_m,
                        x + arrowX_r, y,
                        x + width, y,
                        x + width, y + height,
                        x, y + height);
                listBackground.setLayoutX(-20);
                background.setLayoutX(20);
                listView.setLayoutY(54);
                listView.setLayoutX(25);
                listView.setPrefWidth(width);
            }
        }
    }

    public MyComboBox() {
        this(FXCollections.observableArrayList());
    }

    public MyComboBox(ObservableList<T> items) {
        super(items);

        editor = skin.textInputBox.getInputTextField();
        setPadding(new Insets(0, 20, 0, 20));
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

        clearOnFocus();
        fixSpaceKey();
        setAutocompleteItems(items);
        reactToQueryChanges();
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


    // By default pressing [SPACE] caused editor text to reset. The solution
    // is to suppress relevant event on the underlying ListViewSkin.
    private void fixSpaceKey() {
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
        if (pos > query.length()) pos = query.length();
        editor.setText(query);
        editor.positionCaret(pos);
    }

    private void reactToQueryChanges() {
        editor.addEventHandler(KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
            UIThread.runOnNextRenderFrame(() -> {
                String query = editor.getText();
                var exactMatch = list.stream().anyMatch(item -> asString(item).equalsIgnoreCase(query));
                if (!exactMatch) {
                    if (query.isEmpty())
                        removeFilter();
                    else
                        filterBy(query);
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
}