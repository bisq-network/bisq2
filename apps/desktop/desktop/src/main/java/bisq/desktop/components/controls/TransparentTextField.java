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

import bisq.desktop.common.Transitions;
import javafx.util.StringConverter;

import java.util.Locale;

public class TransparentTextField extends MaterialTextField {
    private final BisqMenuItem editMenuItem, cancelMenuItem, saveMenuItem;

    public TransparentTextField(String description, boolean isEditable) {
        super(description.toUpperCase(Locale.ROOT));

        getStyleClass().add("transparent-text-field");
        descriptionLabel.setLayoutY(6.5);
        setEditable(false);

        editMenuItem = new BisqMenuItem("edit-grey", "edit-white");
        editMenuItem.useIconOnly(17);
        editMenuItem.setLayoutX(220);
        editMenuItem.setLayoutY(4);
        editMenuItem.setVisible(false);
        editMenuItem.setManaged(false);

        cancelMenuItem = new BisqMenuItem("edit-grey", "edit-white");
        cancelMenuItem.useIconOnly(17);
        cancelMenuItem.setLayoutX(190);
        cancelMenuItem.setLayoutY(4);
        cancelMenuItem.setVisible(false);
        cancelMenuItem.setManaged(false);

        saveMenuItem = new BisqMenuItem("edit-grey", "edit-white");
        saveMenuItem.useIconOnly(17);
        saveMenuItem.setLayoutX(220);
        saveMenuItem.setLayoutY(4);
        saveMenuItem.setVisible(false);
        saveMenuItem.setManaged(false);
        getChildren().addAll(editMenuItem, cancelMenuItem, saveMenuItem);

        if (isEditable) {
            setUpMenuItems();
            updateButtons(false);
        }
    }

    private void setUpMenuItems() {
        editMenuItem.setOnAction(e -> {
            resetValidation();
            selectionLine.setPrefWidth(0);
            selectionLine.setOpacity(1);
            Transitions.animatePrefWidth(selectionLine, getWidth());
            bg.getStyleClass().add("active-bg");
            updateButtons(true);
            setEditable(true);
        });
        cancelMenuItem.setOnAction(e -> {
            Transitions.fadeOut(selectionLine, 200);
            bg.getStyleClass().remove("active-bg");
            line.setOpacity(0.25);
            updateButtons(false);
            setEditable(false);
        });
        saveMenuItem.setOnAction(e -> {
            Transitions.fadeOut(selectionLine, 200);
            stringConverter.ifPresent(stringConverter -> {
                try {
                    Object o = stringConverter.fromString(getText());
                    //noinspection unchecked
                    setText(((StringConverter<Object>) stringConverter).toString(o));
                } catch (Exception ignore) {
                }
            });
            validate();
            bg.getStyleClass().remove("active-bg");
            line.setOpacity(0.25);
            updateButtons(false);
            setEditable(false);
        });
    }

    public void dispose() {
        editMenuItem.setOnAction(null);
        cancelMenuItem.setOnAction(null);
        saveMenuItem.setOnAction(null);
    }

    @Override
    protected double getBgHeight() {
        return 56;
    }

    @Override
    protected double getFieldLayoutY() {
        return 27;
    }

    @Override
    protected void animateDescriptionLabel() {
        // Do not animate
    }

    @Override
    protected void onMouseEntered() {
        // Do nothing
    }

    @Override
    protected void onMouseExited() {
        // Do nothing
    }

    @Override
    protected void onInputTextFieldFocus(boolean focus) {
        // Do nothing
    }

    private void updateButtons(boolean isEditing) {
        editMenuItem.setVisible(!isEditing);
        editMenuItem.setManaged(!isEditing);
        cancelMenuItem.setVisible(isEditing);
        cancelMenuItem.setManaged(isEditing);
        saveMenuItem.setVisible(isEditing);
        saveMenuItem.setManaged(isEditing);
    }
}
