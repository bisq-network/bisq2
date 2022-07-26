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
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.utils.validation.InputValidator;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

@Slf4j
public class MaterialTextField extends Pane {
    protected final Region bg, line, selectionLine;
    protected final Label descriptionLabel;
    protected final TextInputControl field;
    protected final Label helpLabel;
    protected final StringProperty promptProperty = new SimpleStringProperty();
    protected final StringProperty descriptionProperty = new SimpleStringProperty();
    protected final StringProperty helpProperty = new SimpleStringProperty();
    @Getter
    private final BisqIconButton iconButton;

    public MaterialTextField() {
        this(null, null, null);
    }

    public MaterialTextField(String description) {
        this(description, null, null);
    }

    public MaterialTextField(String description, String prompt) {
        this(description, prompt, null);
    }

    public MaterialTextField(@Nullable String description, @Nullable String prompt, @Nullable String help) {
        promptProperty.set(prompt);
        descriptionProperty.set(description);
        helpProperty.set(help);

        bg = new Region();
        bg.getStyleClass().add("material-text-field-bg");

        line = new Region();

        line.setPrefHeight(1);
        line.setStyle("-fx-background-color: -bisq-grey-dimmed");
        line.setMouseTransparent(true);

        selectionLine = new Region();
        selectionLine.setPrefWidth(0);
        selectionLine.setPrefHeight(2);
        selectionLine.getStyleClass().add("bisq-green-line");
        selectionLine.setMouseTransparent(true);

        descriptionLabel = new Label();
        descriptionLabel.setLayoutX(16);
        descriptionLabel.setMouseTransparent(true);
        descriptionLabel.setStyle("-fx-font-family: \"IBM Plex Sans Light\";");
        if (description != null) {
            descriptionLabel.setText(description);
        }

        field = createTextInputControl();
        field.setLayoutX(6.5);
        field.getStyleClass().add("material-text-field");
        if (prompt != null) {
            field.setPromptText(prompt);
        }

        iconButton = new BisqIconButton();
        iconButton.setIcon("info");
        iconButton.setLayoutY(6);
        iconButton.setOpacity(0.6);
        iconButton.setManaged(false);
        iconButton.setVisible(false);

        helpLabel = new Label();
        helpLabel.setLayoutX(16);
        helpLabel.setStyle("-fx-font-size: 0.95em; -fx-text-fill: -bisq-grey-dimmed; -fx-font-family: \"IBM Plex Sans Light\";");
        helpLabel.setMouseTransparent(true);
        if (help != null) {
            helpLabel.setText(help);
        }

        getChildren().addAll(bg, line, selectionLine, descriptionLabel, field, iconButton, helpLabel);

        widthProperty().addListener(new WeakReference<ChangeListener<Number>>((observable, oldValue, newValue) ->
                onWidthChanged((double) newValue)).get());

        field.focusedProperty().addListener(new WeakReference<ChangeListener<Boolean>>((observable, oldValue, newValue) ->
                onInputTextFieldFocus(newValue)).get());
        descriptionProperty.addListener(new WeakReference<ChangeListener<String>>((observable, oldValue, newValue) ->
                update()).get());
        promptProperty.addListener(new WeakReference<ChangeListener<String>>((observable, oldValue, newValue) ->
                update()).get());
        helpProperty.addListener(new WeakReference<ChangeListener<String>>((observable, oldValue, newValue) ->
                update()).get());
        field.editableProperty().addListener(new WeakReference<ChangeListener<Boolean>>((observable, oldValue, newValue) ->
        {
            update();
        }).get());
        disabledProperty().addListener(new WeakReference<ChangeListener<Boolean>>((observable, oldValue, newValue) ->
                update()).get());
        widthProperty().addListener(new WeakReference<ChangeListener<Number>>((observable, oldValue, newValue) ->
                update()).get());
        field.textProperty().addListener(new WeakReference<ChangeListener<String>>((observable, oldValue, newValue) ->
                update()).get());

        bg.setOnMousePressed(e -> field.requestFocus());
        bg.setOnMouseEntered(e -> onMouseEntered());
        bg.setOnMouseExited(e -> onMouseExited());
        field.setOnMouseEntered(e -> onMouseEntered());
        field.setOnMouseExited(e -> onMouseExited());

        doLayout();
        update();
    }

    public void requestFocus() {
        field.requestFocus();
    }

    public void hideIcon() {
        iconButton.setManaged(false);
        iconButton.setVisible(false);
    }

    public void showIcon() {
        iconButton.setManaged(true);
        iconButton.setVisible(true);
        layoutIconButton();
    }

    public void setIcon(AwesomeIcon icon) {
        iconButton.setIcon(icon);
        showIcon();
    }

    public void setIcon(String iconId) {
        iconButton.setIcon(iconId);
        showIcon();
    }

    public void setIconTooltip(String text) {
        iconButton.setTooltip(new BisqTooltip(text));
        showIcon();
    }

    public void setOnMousePressedHandler(EventHandler<? super MouseEvent> handler) {
        setOnMousePressed(handler);
        field.setOnMousePressed(handler);
    }

    public final StringProperty promptTextProperty() {
        return field.promptTextProperty();
    }

    public final StringProperty descriptionProperty() {
        return descriptionProperty;
    }

    public final StringProperty textProperty() {
        return field.textProperty();
    }

    public ReadOnlyBooleanProperty inputTextFieldFocusedProperty() {
        return field.focusedProperty();
    }

    public String getText() {
        return field.getText();
    }

    public void setText(String value) {
        field.setText(value);
        update();
    }

    public void setValidator(InputValidator validator) {
        //todo
    }

    public void setDescription(String description) {
        descriptionProperty.set(description);
    }

    public void setEditable(boolean value) {
        field.setEditable(value);
        update();
    }

    public TextInputControl getField() {
        return field;
    }

    Label getDescriptionLabel() {
        return descriptionLabel;
    }


    protected void onMouseEntered() {
        if (!field.isEditable()) {
            return;
        }
        removeBgStyles();
        if (field.isFocused()) {
            bg.getStyleClass().add("material-text-field-bg-selected");
        } else {
            bg.getStyleClass().add("material-text-field-bg-hover");
        }
    }

    protected void onMouseExited() {
        if (!field.isEditable()) {
            return;
        }
        removeBgStyles();
        if (field.isFocused()) {
            bg.getStyleClass().add("material-text-field-bg-selected");
        } else {
            bg.getStyleClass().add("material-text-field-bg");
        }
    }

    protected void onInputTextFieldFocus(boolean focus) {
        if (!field.isEditable()) {
            return;
        }
        if (focus) {
            selectionLine.setPrefWidth(0);
            selectionLine.setOpacity(1);
            Transitions.animateWidth(selectionLine, getWidth());
        } else {
            Transitions.fadeOut(selectionLine, 200);
        }
        onMouseExited();
        update();
    }

    protected void onWidthChanged(double width) {
        if (width > 0) {
            bg.setPrefWidth(width);
            line.setPrefWidth(width);
            selectionLine.setPrefWidth(field.isFocused() && field.isEditable() ? width : 0);
            descriptionLabel.setPrefWidth(width - 2 * descriptionLabel.getLayoutX());
            field.setPrefWidth(width - 2 * field.getLayoutX());
            helpLabel.setPrefWidth(width - 2 * helpLabel.getLayoutX());
        }
    }

    protected void doLayout() {
        bg.setMinHeight(getBgHeight());
        bg.setMaxHeight(getBgHeight());
        line.setLayoutY(getBgHeight() - 1);
        selectionLine.setLayoutY(getBgHeight() - 2);
        field.setLayoutY(getFieldLayoutY());
    }

    private void layoutIconButton() {
        if (getWidth() > 0 && iconButton.isManaged()) {
            iconButton.setLayoutX(getWidth() - iconButton.getWidth() - 12);
        }
    }

    void update() {
        if (descriptionProperty.get() != null) {
            if (showInputTextField()) {
                Transitions.animateLayoutY(descriptionLabel, 6.5, Transitions.DEFAULT_DURATION / 6d, null);
            } else {
                Transitions.animateLayoutY(descriptionLabel, 16.5, Transitions.DEFAULT_DURATION / 6d, null);
            }
        }
        helpLabel.setVisible(helpProperty.get() != null);
        helpLabel.setManaged(helpProperty.get() != null);

        descriptionLabel.getStyleClass().remove("material-text-field-description-read-only");
        field.getStyleClass().remove("material-text-field-read-only");
        if (showInputTextField()) {
            descriptionLabel.getStyleClass().remove("material-text-field-description-big");
            descriptionLabel.getStyleClass().add("material-text-field-description-small");
        } else {
            descriptionLabel.getStyleClass().remove("material-text-field-description-small");
            descriptionLabel.getStyleClass().add("material-text-field-description-big");
        }
        if (field.isFocused()) {
            descriptionLabel.getStyleClass().remove("material-text-field-description-deselected");
            descriptionLabel.getStyleClass().add("material-text-field-description-selected");
        } else {
            descriptionLabel.getStyleClass().remove("material-text-field-description-selected");
            descriptionLabel.getStyleClass().add("material-text-field-description-deselected");
        }

        if (field.isEditable()) {
            bg.setMouseTransparent(false);
            line.setOpacity(1);
        } else {
            bg.setMouseTransparent(true);
            line.setOpacity(0.25);
            descriptionLabel.getStyleClass().remove("material-text-field-description-big");
            descriptionLabel.getStyleClass().remove("material-text-field-description-deselected");
            descriptionLabel.getStyleClass().remove("material-text-field-description-selected");
            descriptionLabel.getStyleClass().add("material-text-field-description-small");
            descriptionLabel.getStyleClass().add("material-text-field-description-read-only");
            field.getStyleClass().add("material-text-field-read-only");
        }
        setOpacity(field.isDisabled() ? 0.35 : 1);
        UIThread.runOnNextRenderFrame(this::layoutIconButton);
    }

    protected void removeBgStyles() {
        bg.getStyleClass().remove("material-text-field-bg-hover");
        bg.getStyleClass().remove("material-text-field-bg-selected");
        bg.getStyleClass().remove("material-text-field-bg");
    }

    protected boolean showInputTextField() {
        return promptProperty.get() != null || ((field.getText() != null &&
                !field.getText().isEmpty()) ||
                field.isFocused());
    }

    protected TextInputControl createTextInputControl() {
        return new TextField();
    }

    protected double getBgHeight() {
        return 56;
    }

    protected double getFieldLayoutY() {
        return 18;
    }

    @Override
    protected double computeMinHeight(double width) {
        log.error("computeMinHeight {}", super.computeMinHeight(width));
        if (helpLabel.isManaged()) {
            return helpLabel.getLayoutY() + helpLabel.getHeight();
        } else {
            return getBgHeight();
        }
    }

    @Override
    protected double computeMaxHeight(double width) {
        log.error("computeMaxHeight {}", super.computeMaxHeight(width));
        return computeMinHeight(width);
    }

    @Override
    protected double computePrefHeight(double width) {
        log.error("computePrefHeight {}", super.computePrefHeight(width));
        return computeMinHeight(width);
    }

    @Override
    protected double computePrefWidth(double height) {
        log.error("computePrefWidth {}", super.computePrefWidth(height));
        layoutIconButton();
        return super.computePrefWidth(height);
        // return bg.getWidth();
    }

    @Override
    protected double computeMinWidth(double height) {
        log.error("computeMinWidth {}", super.computeMinWidth(height));
        return super.computeMinWidth(height);
    }

    @Override
    protected double computeMaxWidth(double height) {
        log.error("computeMaxWidth {}", super.computeMaxWidth(height));
        return super.computeMaxWidth(height);
    }
}