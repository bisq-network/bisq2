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
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * As Label or Text are not selectable and TextField does not support wrapping we use a custom component which
 * uses a Label for layout and then apply its size and style to a TextArea and swap it.
 * <p>
 * We want to keep the API mirrored to the Label. With more usage we will add more support for the Label API.
 * When setting size affecting properties, we need to add support for resizing the TextField. Currently, it is expected
 * that the text is set at initialization.
 */
@Slf4j
public class SelectableLabel extends StackPane {
    private final Label label = new Label();
    private final TextArea textArea = new TextArea();
    boolean isSwapped;

    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ListChangeListener<String> styleClassListener = c -> {
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

    private final ChangeListener<Bounds> labelBoundsListener = (observable, oldBounds, newBounds) -> {
        if (!isSwapped && newBounds.getWidth() > 0 && newBounds.getHeight() > 0) {
            UIThread.runOnNextRenderFrame(this::swap);
        }
    };

    public SelectableLabel() {
        this("");
    }

    public SelectableLabel(String text) {
        getChildren().add(label);
        label.setText(text);
        label.setCursor(Cursor.TEXT);
        textArea.setContextMenu(new ContextMenu());
        textArea.setText(text);
        textArea.setEditable(false);
        textArea.getStyleClass().addAll("selectable-label", "hide-vertical-scrollbar");
        label.boundsInParentProperty().addListener(labelBoundsListener);
        getStyleClass().addListener(new WeakListChangeListener<>(styleClassListener));
    }

    public String getText() {
        return this.textArea.getText();
    }

    private void swap() {
        if (isSwapped) {
            return;
        }

        isSwapped = true;
        label.boundsInParentProperty().removeListener(labelBoundsListener);

        // Required to set give size instead of taking available size from parent.
        // Default would be Double.MAX_VALUE, which allows to grow to max. size of parent.
        textArea.setMaxWidth(Region.USE_PREF_SIZE);
        textArea.setMaxHeight(Region.USE_PREF_SIZE);

        textArea.setPrefWidth(label.getWidth());
        textArea.setPrefHeight(label.getHeight());
        getChildren().remove(label);
        getChildren().add(textArea);
    }

    // If we support change of text after initialization we need to adjust the size of the textArea
    // Label API
    public void setText(String value) {
        label.setText(value);
        textArea.setText(value);
    }

    public void setWrapText(boolean value) {
        label.setWrapText(value);
        textArea.setWrapText(value);
    }

    public void setTooltip(Tooltip value) {
        label.setTooltip(value);
        textArea.setTooltip(value);
    }
}
