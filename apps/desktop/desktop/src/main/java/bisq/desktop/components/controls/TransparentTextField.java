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
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.ImageUtil;
import bisq.i18n.Res;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.util.StringConverter;

import java.util.Locale;
import java.util.function.Consumer;

public class TransparentTextField extends MaterialTextField {
    private static final double TEXT_FIELD_WIDTH = 250;
    private static final double ICON_LAYOUT_Y = 4;

    private final boolean isEditable;
    private final Button editButton, cancelButton, saveButton;
    private final ImageView editGreyIcon, editWhiteIcon, cancelGreyIcon, cancelWhiteIcon,
            saveGreyIcon, saveWhiteIcon, saveGreenIcon;
    private final Consumer<String> onSaveClicked;
    private final Runnable onCancelClicked;
    private UIScheduler saveOrCancelScheduler, editScheduler;

    public TransparentTextField(String description, boolean isEditable) {
        this(description, isEditable, null, null);
    }

    public TransparentTextField(String description,
                                boolean isEditable,
                                Consumer<String> onSaveClicked,
                                Runnable onCancelClicked) {
        super(description.toUpperCase(Locale.ROOT));

        this.isEditable = isEditable;
        this.onSaveClicked = onSaveClicked;
        this.onCancelClicked = onCancelClicked;

        getStyleClass().add("transparent-text-field");
        setPrefWidth(TEXT_FIELD_WIDTH);
        setEditable(false);
        descriptionLabel.setLayoutY(6.5);

        editGreyIcon = ImageUtil.getImageViewById("edit-grey");
        editWhiteIcon = ImageUtil.getImageViewById("edit-white");
        cancelGreyIcon = ImageUtil.getImageViewById("close-mid-grey");
        cancelWhiteIcon = ImageUtil.getImageViewById("close-mid-white");
        saveGreyIcon = ImageUtil.getImageViewById("save-grey");
        saveWhiteIcon = ImageUtil.getImageViewById("save-white");
        saveGreenIcon = ImageUtil.getImageViewById("save-green");

        editButton = createAndGetIconButton(editGreyIcon,
                Res.get("user.profileCard.myNotes.transparentTextField.buttonTooltip.edit"),
                TEXT_FIELD_WIDTH - 30);
        saveButton = createAndGetIconButton(saveGreyIcon,
                Res.get("user.profileCard.myNotes.transparentTextField.buttonTooltip.save"),
                TEXT_FIELD_WIDTH - 60);
        cancelButton = createAndGetIconButton(cancelGreyIcon,
                Res.get("user.profileCard.myNotes.transparentTextField.buttonTooltip.cancel"),
                TEXT_FIELD_WIDTH - 30);
        getChildren().addAll(editButton, saveButton, cancelButton);
    }

    public void initialize() {
        if (isEditable) {
            attachListeners();
            updateButtons(false);
        } else {
            hideButtons();
        }
    }

    public void dispose() {
        setToNonEditingMode();
        selectionLine.setOpacity(0);
        resetValidation();

        if (saveOrCancelScheduler != null) {
            saveOrCancelScheduler.stop();
            saveOrCancelScheduler = null;
        }

        if (editScheduler != null) {
            editScheduler.stop();
            editScheduler = null;
        }

        editButton.setOnMouseEntered(null);
        editButton.setOnMouseExited(null);
        editButton.setOnMouseClicked(null);
        editButton.setOnAction(null);

        saveButton.setOnMouseEntered(null);
        saveButton.setOnMouseExited(null);
        saveButton.setOnMouseClicked(null);
        saveButton.setOnAction(null);

        cancelButton.setOnMouseEntered(null);
        cancelButton.setOnMouseExited(null);
        cancelButton.setOnMouseClicked(null);
        cancelButton.setOnAction(null);
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

    private void attachListeners() {
        editButton.setOnMouseEntered(e -> editButton.setGraphic(editWhiteIcon));
        editButton.setOnMouseExited(e -> editButton.setGraphic(editGreyIcon));
        editButton.setOnMouseClicked(e -> editButton.setGraphic(editWhiteIcon));

        saveButton.setOnMouseEntered(e -> saveButton.setGraphic(saveWhiteIcon));
        saveButton.setOnMouseExited(e -> saveButton.setGraphic(saveGreyIcon));
        saveButton.setOnMouseClicked(e -> saveButton.setGraphic(saveGreenIcon));

        cancelButton.setOnMouseEntered(e -> cancelButton.setGraphic(cancelWhiteIcon));
        cancelButton.setOnMouseExited(e -> cancelButton.setGraphic(cancelGreyIcon));
        cancelButton.setOnMouseClicked(e -> cancelButton.setGraphic(cancelWhiteIcon));

        editButton.setOnAction(e -> {
            resetValidation();
            selectionLine.setPrefWidth(0);
            selectionLine.setOpacity(1);
            Transitions.animatePrefWidth(selectionLine, getWidth());
            transitionToEditingMode();
        });
        cancelButton.setOnAction(e -> {
            Transitions.fadeOut(selectionLine, 200);
            transitionToNonEditingMode();
            resetValidation();
            if (onCancelClicked != null) {
                onCancelClicked.run();
            }
        });
        saveButton.setOnAction(e -> {
            stringConverter.ifPresent(stringConverter -> {
                try {
                    Object o = stringConverter.fromString(getText());
                    //noinspection unchecked
                    setText(((StringConverter<Object>) stringConverter).toString(o));
                } catch (Exception ignore) {
                }
            });
            if (validate()) {
                Transitions.fadeOut(selectionLine, 200);
                transitionToNonEditingMode();
                resetValidation();
                if (onSaveClicked != null) {
                    onSaveClicked.accept(getText());

                }
            }
        });
    }

    private void updateButtons(boolean isEditing) {
        editButton.setVisible(!isEditing);
        editButton.setManaged(!isEditing);
        cancelButton.setVisible(isEditing);
        cancelButton.setManaged(isEditing);
        saveButton.setVisible(isEditing);
        saveButton.setManaged(isEditing);
    }

    private void transitionToNonEditingMode() {
        if (Transitions.useAnimations()) {
            saveOrCancelScheduler = UIScheduler.run(this::setToNonEditingMode).after(200);
        } else {
            setToNonEditingMode();
        }
    }

    private void setToNonEditingMode() {
        bg.getStyleClass().remove("active-bg");
        line.setOpacity(0.25);
        updateButtons(false);
        setEditable(false);
    }

    private void transitionToEditingMode() {
        if (Transitions.useAnimations()) {
            editScheduler = UIScheduler.run(this::setToEditingMode).after(200);
        } else {
            setToEditingMode();
        }
    }

    private void setToEditingMode() {
        bg.getStyleClass().add("active-bg");
        updateButtons(true);
        setEditable(true);
    }

    private Button createAndGetIconButton(ImageView icon, String tooltip, double layoutX) {
        Button button = new Button();
        button.getStyleClass().add("icon-buttons");
        button.setGraphic(icon);
        button.setLayoutX(layoutX);
        button.setLayoutY(ICON_LAYOUT_Y);
        button.setVisible(false);
        button.setManaged(false);
        button.setTooltip(new BisqTooltip(tooltip));
        return button;
    }

    private void hideButtons() {
        editButton.setVisible(false);
        editButton.setManaged(false);
        cancelButton.setVisible(false);
        cancelButton.setManaged(false);
        saveButton.setVisible(false);
        saveButton.setManaged(false);
    }
}
