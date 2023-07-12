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

import bisq.common.util.StringUtils;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.validation.InputValidator;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
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
    protected final Region bg = new Region();
    protected final Region line = new Region();
    protected final Region selectionLine = new Region();
    protected final Label descriptionLabel = new Label();
    protected final TextInputControl textInputControl;
    @Getter
    protected final Label helpLabel = new Label();
    @Getter
    private final BisqIconButton iconButton = new BisqIconButton();
    private ChangeListener<Number> iconButtonHeightListener;

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
        this(description, prompt, help, null);
    }

    public MaterialTextField(@Nullable String description, @Nullable String prompt, @Nullable String help, @Nullable String value) {
        bg.getStyleClass().add("material-text-field-bg");

        line.setPrefHeight(1);
        line.setStyle("-fx-background-color: -bisq-grey-dimmed");
        line.setMouseTransparent(true);

        selectionLine.setPrefWidth(0);
        selectionLine.setPrefHeight(2);
        selectionLine.getStyleClass().add("bisq-green-line");
        selectionLine.setMouseTransparent(true);

        descriptionLabel.setLayoutX(16);
        descriptionLabel.setMouseTransparent(true);
        descriptionLabel.setStyle("-fx-font-family: \"IBM Plex Sans Light\";");
        if (StringUtils.isNotEmpty(description)) {
            descriptionLabel.setText(description);
        }

        textInputControl = createTextInputControl();
        if (StringUtils.isNotEmpty(value)) {
            textInputControl.setText(value);
        }
        textInputControl.setLayoutX(6.5);
        textInputControl.getStyleClass().add("material-text-field");
        if (StringUtils.isNotEmpty(prompt)) {
            textInputControl.setPromptText(prompt);
        }

        iconButton.setAlignment(Pos.TOP_RIGHT);
        iconButton.setIcon("info");
        iconButton.setOpacity(0.6);
        iconButton.setManaged(false);
        iconButton.setVisible(false);

        helpLabel.setLayoutX(16);
        helpLabel.getStyleClass().add("material-text-field-help");
        helpLabel.setMouseTransparent(true);
        if (StringUtils.isNotEmpty(help)) {
            helpLabel.setText(help);
        }

        getChildren().addAll(bg, line, selectionLine, descriptionLabel, textInputControl, iconButton, helpLabel);

        widthProperty().addListener(new WeakReference<ChangeListener<Number>>((observable, oldValue, newValue) ->
                onWidthChanged((double) newValue)).get());

        textInputControl.focusedProperty().addListener(new WeakReference<ChangeListener<Boolean>>((observable, oldValue, newValue) ->
                onInputTextFieldFocus(newValue)).get());
        descriptionLabel.textProperty().addListener(new WeakReference<ChangeListener<String>>((observable, oldValue, newValue) ->
                update()).get());

        promptTextProperty().addListener(new WeakReference<ChangeListener<String>>((observable, oldValue, newValue) ->
                update()).get());
        helpHelpProperty().addListener(new WeakReference<ChangeListener<String>>((observable, oldValue, newValue) ->
                update()).get());
        textInputControl.editableProperty().addListener(new WeakReference<ChangeListener<Boolean>>((observable, oldValue, newValue) ->
                update()).get());
        disabledProperty().addListener(new WeakReference<ChangeListener<Boolean>>((observable, oldValue, newValue) ->
                update()).get());
        widthProperty().addListener(new WeakReference<ChangeListener<Number>>((observable, oldValue, newValue) ->
                layoutIconButton()).get());
        textInputControl.textProperty().addListener(new WeakReference<ChangeListener<String>>((observable, oldValue, newValue) ->
                update()).get());

        bg.setOnMousePressed(e -> textInputControl.requestFocus());
        bg.setOnMouseEntered(e -> onMouseEntered());
        bg.setOnMouseExited(e -> onMouseExited());
        textInputControl.setOnMouseEntered(e -> onMouseEntered());
        textInputControl.setOnMouseExited(e -> onMouseExited());

        doLayout();
        update();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Description
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public String getDescription() {
        return descriptionLabel.getText();
    }

    public void setDescription(String description) {
        descriptionLabel.setText(description);
    }

    public final StringProperty descriptionProperty() {
        return descriptionLabel.textProperty();
    }

    public Label getDescriptionLabel() {
        return descriptionLabel;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Text
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public String getText() {
        return textInputControl.getText();
    }

    public void setText(String value) {
        textInputControl.setText(value);
        update();
    }

    public final StringProperty textProperty() {
        return textInputControl.textProperty();
    }

    protected TextInputControl createTextInputControl() {
        return new TextField();
    }

    public TextInputControl getTextInputControl() {
        return textInputControl;
    }

    public void setEditable(boolean value) {
        textInputControl.setEditable(value);
        update();
    }

    public void setValidator(InputValidator validator) {
        //todo
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // PromptText
    ///////////////////////////////////////////////////////////////////////////////////////////////////


    public String getPromptText() {
        return textInputControl.getPromptText();
    }

    public void setPromptText(String value) {
        textInputControl.setPromptText(value);
    }

    public final StringProperty promptTextProperty() {
        return textInputControl.promptTextProperty();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Help
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public String getHelpText() {
        return helpLabel.getText();
    }

    public void setHelpText(String value) {
        helpLabel.setText(value);
    }

    public final StringProperty helpHelpProperty() {
        return helpLabel.textProperty();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Icon
    ///////////////////////////////////////////////////////////////////////////////////////////////////

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

    public void showIcon() {
        iconButton.setManaged(true);
        iconButton.setVisible(true);
        layoutIconButton();
    }

    public void hideIcon() {
        iconButton.setManaged(false);
        iconButton.setVisible(false);
    }

    public void showCopyIcon() {
        setIcon(AwesomeIcon.COPY);
        setIconTooltip(Res.get("action.copyToClipboard"));
        iconButton.setOnAction(new WeakReference<EventHandler<ActionEvent>>(e ->
                ClipboardUtil.copyToClipboard(getText())).get());
    }

    public void showEditIcon() {
        setIcon(AwesomeIcon.EDIT);
        setIconTooltip(Res.get("action.editable"));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Focus
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public ReadOnlyBooleanProperty textInputFocusedProperty() {
        return textInputControl.focusedProperty();
    }

    public void requestFocus() {
        textInputControl.requestFocus();
        textInputControl.deselect();
        textInputControl.selectHome();
    }

    public void deselect() {
        textInputControl.deselect();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Event handlers
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void setOnMousePressedHandler(EventHandler<? super MouseEvent> handler) {
        setOnMousePressed(handler);
        textInputControl.setOnMousePressed(handler);
    }

    protected void onMouseEntered() {
        removeBgStyles();
        if (textInputControl.isFocused() && textInputControl.isEditable()) {
            bg.getStyleClass().add("material-text-field-bg-selected");
        } else {
            bg.getStyleClass().add("material-text-field-bg-hover");
        }
    }

    protected void onMouseExited() {
        removeBgStyles();
        if (textInputControl.isFocused() && textInputControl.isEditable()) {
            bg.getStyleClass().add("material-text-field-bg-selected");
        } else {
            bg.getStyleClass().add("material-text-field-bg");
        }
    }

    protected void onInputTextFieldFocus(boolean focus) {
        if (focus && textInputControl.isEditable()) {
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
            selectionLine.setPrefWidth(textInputControl.isFocused() && textInputControl.isEditable() ? width : 0);
            descriptionLabel.setPrefWidth(width - 2 * descriptionLabel.getLayoutX());
            double iconWidth = iconButton.isVisible() ? 25 : 0;
            textInputControl.setPrefWidth(width - 2 * textInputControl.getLayoutX() - iconWidth);
            helpLabel.setPrefWidth(width - 2 * helpLabel.getLayoutX());
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Layout
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    protected void doLayout() {
        bg.setMinHeight(getBgHeight());
        bg.setMaxHeight(getBgHeight());
        line.setLayoutY(getBgHeight() - 1);
        selectionLine.setLayoutY(getBgHeight() - 2);
        textInputControl.setLayoutY(getFieldLayoutY());
        helpLabel.setLayoutY(getBgHeight() + 3.5);
    }

    private void layoutIconButton() {
        if (iconButton.getHeight() > 0) {
            if (getWidth() > 0 && iconButton.isManaged()) {
                if (iconButton.getAlignment() == Pos.CENTER ||
                        iconButton.getAlignment() == Pos.CENTER_LEFT ||
                        iconButton.getAlignment() == Pos.CENTER_RIGHT) {
                    iconButton.setLayoutY((getBgHeight() - iconButton.getHeight()) / 2 - 1);
                } else {
                    iconButton.setLayoutY(6);
                }
                iconButton.setLayoutX(getWidth() - iconButton.getWidth() - 12 + iconButton.getPadding().getLeft());
            }
        } else {
            iconButtonHeightListener = (observable, oldValue, newValue) -> {
                if (newValue.doubleValue() > 0) {
                    layoutIconButton();
                    UIThread.runOnNextRenderFrame(() -> iconButton.heightProperty().removeListener(iconButtonHeightListener));
                }
            };
            iconButton.heightProperty().addListener(iconButtonHeightListener);
        }
    }

    void update() {
        if (StringUtils.isNotEmpty(descriptionLabel.getText())) {
            if (showInputTextField()) {
                Transitions.animateLayoutY(descriptionLabel, 6.5, Transitions.DEFAULT_DURATION / 6d, null);
            } else {
                Transitions.animateLayoutY(descriptionLabel, 16.5, Transitions.DEFAULT_DURATION / 6d, null);
            }
        }
        helpLabel.setVisible(StringUtils.isNotEmpty(helpHelpProperty().get()));
        helpLabel.setManaged(StringUtils.isNotEmpty(helpHelpProperty().get()));

        descriptionLabel.getStyleClass().remove("material-text-field-description-read-only");
        textInputControl.getStyleClass().remove("material-text-field-read-only");

        descriptionLabel.getStyleClass().remove("material-text-field-description-small");
        descriptionLabel.getStyleClass().remove("material-text-field-description-big");
        descriptionLabel.getStyleClass().remove("material-text-field-description-selected");
        descriptionLabel.getStyleClass().remove("material-text-field-description-deselected");
        descriptionLabel.getStyleClass().remove("material-text-field-description-read-only");

        if (showInputTextField()) {
            descriptionLabel.getStyleClass().add("material-text-field-description-small");
        } else {
            descriptionLabel.getStyleClass().add("material-text-field-description-big");
        }
        if (textInputControl.isFocused()) {
            descriptionLabel.getStyleClass().add("material-text-field-description-selected");
        } else {
            descriptionLabel.getStyleClass().add("material-text-field-description-deselected");
        }

        if (textInputControl.isEditable()) {
            bg.setMouseTransparent(false);
            bg.setOpacity(1);
            line.setOpacity(1);
            textInputControl.getStyleClass().remove("material-text-field-read-only");
        } else {
            bg.setMouseTransparent(true);
            bg.setOpacity(0.4);
            line.setOpacity(0.25);
            descriptionLabel.getStyleClass().add("material-text-field-description-small");
            descriptionLabel.getStyleClass().add("material-text-field-description-read-only");
            textInputControl.getStyleClass().add("material-text-field-read-only");
        }
        setOpacity(textInputControl.isDisabled() ? 0.35 : 1);
        UIThread.runOnNextRenderFrame(this::layoutIconButton);
    }

    protected void removeBgStyles() {
        bg.getStyleClass().remove("material-text-field-bg-hover");
        bg.getStyleClass().remove("material-text-field-bg-selected");
        bg.getStyleClass().remove("material-text-field-bg");
    }

    protected boolean showInputTextField() {
        return StringUtils.isNotEmpty(promptTextProperty().get()) ||
                StringUtils.isNotEmpty(textInputControl.getText()) ||
                textInputControl.isFocused();
    }

    protected double getBgHeight() {
        return 56;
    }

    protected double getFieldLayoutY() {
        return 19;
    }

    @Override
    protected double computeMinHeight(double width) {
        if (helpLabel.isManaged()) {
            return helpLabel.getLayoutY() + helpLabel.getHeight();
        } else {
            return getBgHeight();
        }
    }

    @Override
    protected double computeMaxHeight(double width) {
        return computeMinHeight(width);
    }

    @Override
    protected double computePrefHeight(double width) {
        return computeMinHeight(width);
    }

    @Override
    protected double computePrefWidth(double height) {
        layoutIconButton();
        return super.computePrefWidth(height);
    }

    @Override
    protected double computeMinWidth(double height) {
        return super.computeMinWidth(height);
    }

    @Override
    protected double computeMaxWidth(double height) {
        return super.computeMaxWidth(height);
    }
}