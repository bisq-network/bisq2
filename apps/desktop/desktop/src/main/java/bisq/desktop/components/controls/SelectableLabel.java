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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * As Label or Text are not selectable and TextField does not support wrapping we use a custom component which
 * displays the Label and when the user clicks into it, we show a TextArea with exact the same size and style as the
 * Label to add support for both selection and wrapped text.
 * <p>
 * We want to keep the API mirrored to the Label. With more usage we will add more support for the Label API.
 */
@Slf4j
public class SelectableLabel extends StackPane {
    private final Label label = new Label();
    private final TextArea textArea = new TextArea();

    boolean isInSelectionMode;

    // Pin down the listeners to not get GCed before our object gets GCed
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<Boolean> textAreaFocusListener;
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<Boolean> rootFocusListener;
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ListChangeListener<String> styleClassListener;

    // As we remove some WeakChangeListeners we keep them as class fields
    private final WeakChangeListener<Boolean> weakRootFocusListener;
    private final WeakChangeListener<Boolean> weakTextAreaFocusListener;

    public SelectableLabel() {
        this("");
    }

    public SelectableLabel(String text) {
        getChildren().add(label);

        label.setText(text);
        textArea.setText(text);

        textArea.getStyleClass().addAll("selectable-label", "hide-vertical-scrollbar");

        label.setOnMouseClicked(e -> {
            setInSelectionMode(true);
            requestFocus();
        });
        textAreaFocusListener = (observableValue, oldValue, newValue) -> {
            if (oldValue) {
                setInSelectionMode(false);
            }
        };
        weakTextAreaFocusListener = new WeakChangeListener<>(textAreaFocusListener);

        rootFocusListener = (observableValue, oldValue, newValue) -> {
            if (textArea.isFocused()) {
                textArea.focusedProperty().addListener(weakTextAreaFocusListener);
            } else if (oldValue) {
                setInSelectionMode(false);
            }
        };
        weakRootFocusListener = new WeakChangeListener<>(rootFocusListener);

        styleClassListener = c -> {
            c.next();
            if (c.wasAdded()) {
                List<? extends String> addedSubList = c.getAddedSubList();
                label.getStyleClass().addAll(addedSubList);
                textArea.getStyleClass().addAll(addedSubList);
            } else if (c.wasRemoved()) {
                List<? extends String> removedList = c.getRemoved();
                label.getStyleClass().removeAll(removedList);
                textArea.getStyleClass().removeAll(removedList);
            }
        };
        getStyleClass().addListener(new WeakListChangeListener<>(styleClassListener));
    }

    private void setInSelectionMode(boolean inSelectionMode) {
        isInSelectionMode = inSelectionMode;
        if (isInSelectionMode) {
            getChildren().remove(label);
            getChildren().add(textArea);
            // using prefWidth/prefHeight does not force the same size
            textArea.setMinWidth(label.getWidth());
            textArea.setMaxWidth(textArea.getMinWidth());
            textArea.setMinHeight(label.getHeight());
            textArea.setMaxHeight(textArea.getMinHeight());
            textArea.deselect();
            focusedProperty().addListener(weakRootFocusListener);
        } else {
            focusedProperty().removeListener(weakRootFocusListener);
            textArea.focusedProperty().removeListener(weakTextAreaFocusListener);
            getChildren().remove(textArea);
            getChildren().add(label);
        }
    }

    // Label API
    public void setText(String value) {
        label.setText(value);
        textArea.setText(value);
    }

    public void setWrapText(boolean value) {
        label.setWrapText(value);
        textArea.setWrapText(value);
    }
}
