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
import bisq.desktop.common.ManagedDuration;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.components.controls.validator.ValidationControl;
import bisq.desktop.components.controls.validator.ValidatorBase;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.WeakEventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static bisq.desktop.components.controls.validator.ValidatorBase.PSEUDO_CLASS_ERROR;

@Slf4j
public class MaterialTextField extends Pane {
    private static final double BORDER_LINE_OFFSET = 1.0;
    private static final double SELECTION_LINE_OFFSET = 2.0;

    private static final List<String> BG_STYLE_CLASSES = List.of(
            "material-text-field-bg-hover",
            "material-text-field-bg-selected",
            "material-text-field-bg"
    );

    private static final List<String> DESCRIPTION_STYLE_CLASSES = List.of(
            "material-text-field-description-read-only",
            "material-text-field-description-small",
            "material-text-field-description-big",
            "material-text-field-description-selected",
            "material-text-field-description-deselected"
    );

    private static final List<String> TEXT_INPUT_STYLE_CLASSES = List.of(
            "material-text-field",
            "material-text-field-read-only"
    );

    protected final Region bg = new Region();
    protected final Region line = new Region();
    @Getter
    protected final Region selectionLine = new Region();
    @Getter
    protected final Label descriptionLabel = new Label();
    @Getter
    protected final TextInputControl textInputControl;
    @Getter
    protected final Label helpLabel = new Label();
    @Getter
    protected final Label errorLabel = new Label();
    @Getter
    protected final BisqIconButton iconButton = new BisqIconButton();
    protected final ValidationControl validationControl;
    private final BooleanProperty isValid = new SimpleBooleanProperty(false);
    private Optional<StringConverter<Number>> stringConverter = Optional.empty();

    private boolean compactMode = false;
    private double compactHeight = -1;

    private ChangeListener<Number> iconButtonHeightListener;
    private ChangeListener<Number> layoutWidthListener;

    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<Number> widthListener = (observable, oldValue, newValue) -> onWidthChanged((double) newValue);
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<Boolean> textInputControlFocusListener = (observable, oldValue, newValue) -> onInputTextFieldFocus(newValue);
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<String> descriptionLabelTextListener = (observable, oldValue, newValue) -> update();
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<String> promptTextListener = (observable, oldValue, newValue) -> update();
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<String> helpListener = (observable, oldValue, newValue) -> update();
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<Boolean> textInputControlEditableListener = (observable, oldValue, newValue) -> update();
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<Boolean> disabledListener = (observable, oldValue, newValue) -> update();
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<String> textInputControlTextListener = (observable, oldValue, newValue) -> update();
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakEventHandler
    private final EventHandler<ActionEvent> iconButtonOnActionHandler = event -> ClipboardUtil.copyToClipboard(getText());
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakEventHandler
    private final EventHandler<MouseEvent> textInputControlMouseEventFilter = event -> {
        if (!getTextInputControl().isEditable()) {
            event.consume();
        }
    };

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

    public MaterialTextField(@Nullable String description,
                             @Nullable String prompt,
                             @Nullable String help,
                             @Nullable String value) {
        textInputControl = createTextInputControl();

        setupVisualComponents(description, prompt, help, value);
        setupEventHandlers();

        validationControl = new ValidationControl(textInputControl);

        doLayout();
        update();
    }


    public void useCompactModeWithHeight(double compactHeight) {
        this.compactMode = true;
        this.compactHeight = compactHeight;
        applyCompactModeLayout(compactHeight);
    }

    public void useRegularMode() {
        compactMode = false;
        compactHeight = 0;
        doLayout();
        update();
    }

    public void showSelectionLine(boolean animate) {
        if (getWidth() <= 0 || getHeight() <= 0) {
            ensureLayoutThenShowSelectionLine(animate);
            return;
        }

        selectionLine.setVisible(true);
        selectionLine.setOpacity(1);

        if (animate) {
            selectionLine.setPrefWidth(0);

            UIThread.run(() -> {
                Transitions.animatePrefWidth(selectionLine, getWidth());
                UIThread.runOnNextRenderFrame(() -> {
                    if (textInputControl.isFocused()) {
                        maintainSelectionLineVisibility();
                    }
                });
            });
        } else {
            selectionLine.setPrefWidth(getWidth());
        }
    }

    /* --------------------------------------------------------------------- */
    // Validation
    /* --------------------------------------------------------------------- */

    public Optional<ValidatorBase> getActiveValidator() {
        return Optional.ofNullable(validationControl.getActiveValidator());
    }

    public ReadOnlyObjectProperty<ValidatorBase> activeValidatorProperty() {
        return validationControl.activeValidatorProperty();
    }

    public ObservableList<ValidatorBase> getValidators() {
        return validationControl.getValidators();
    }

    public void setValidators(ValidatorBase... validators) {
        Stream.of(validators).forEach(this::setValidator);
    }

    public void setValidator(ValidatorBase validator) {
        validationControl.setValidators(validator);

        // TODO that cause an endless loop if hasErrors is set to false in the eval method in validators (as in NumberValidator).
        // validator.hasErrorsProperty().addListener((observable, oldValue, newValue) -> validate());
    }

    public boolean validate() {
        resetValidation();
        boolean valid = validationControl.validate();
        isValid.set(valid);
        applyValidationStyling(!valid);
        updateErrorDisplay(valid);
        update();
        return valid;
    }

    public void resetValidation() {
        validationControl.resetValidation();
        isValid.set(false);
        selectionLine.pseudoClassStateChanged(PSEUDO_CLASS_ERROR, false);
        descriptionLabel.pseudoClassStateChanged(PSEUDO_CLASS_ERROR, false);
        errorLabel.setText("");
    }

    public BooleanProperty isValidProperty() {
        return isValid;
    }


    /* --------------------------------------------------------------------- */
    // Description
    /* --------------------------------------------------------------------- */

    public String getDescription() {
        return descriptionLabel.getText();
    }

    public void setDescription(String description) {
        descriptionLabel.setText(description);
    }

    public final StringProperty descriptionProperty() {
        return descriptionLabel.textProperty();
    }


    /* --------------------------------------------------------------------- */
    // Text
    /* --------------------------------------------------------------------- */

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

    public void setEditable(boolean value) {
        textInputControl.setEditable(value);
        update();
    }

    public void setStringConverter(StringConverter<Number> stringConverter) {
        this.stringConverter = Optional.of(stringConverter);
    }


    /* --------------------------------------------------------------------- */
    // PromptText
    /* --------------------------------------------------------------------- */

    public String getPromptText() {
        return textInputControl.getPromptText();
    }

    public void setPromptText(String value) {
        textInputControl.setPromptText(value);
    }

    public final StringProperty promptTextProperty() {
        return textInputControl.promptTextProperty();
    }


    /* --------------------------------------------------------------------- */
    // Help
    /* --------------------------------------------------------------------- */

    public String getHelpText() {
        return helpLabel.getText();
    }

    public void setHelpText(String value) {
        helpLabel.setText(value);
    }

    public final StringProperty helpProperty() {
        return helpLabel.textProperty();
    }


    /* --------------------------------------------------------------------- */
    // Icon
    /* --------------------------------------------------------------------- */

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
        iconButton.setOnAction(new WeakEventHandler<>(iconButtonOnActionHandler));
    }

    public void showEditIcon() {
        setIcon(AwesomeIcon.EDIT);
        setIconTooltip(Res.get("action.editable"));
    }


    /* --------------------------------------------------------------------- */
    // Focus
    /* --------------------------------------------------------------------- */

    public ReadOnlyBooleanProperty textInputFocusedProperty() {
        return textInputControl.focusedProperty();
    }

    public void requestFocus() {
        textInputControl.requestFocus();
        textInputControl.deselect();
        textInputControl.selectHome();
    }

    public void requestFocusWithCursor() {
        requestFocus();
        textInputControl.selectRange(textInputControl.getLength(), textInputControl.getLength());
    }

    public void deselect() {
        textInputControl.deselect();
    }


    /* --------------------------------------------------------------------- */
    // Event handlers
    /* --------------------------------------------------------------------- */

    public void setOnMousePressedHandler(EventHandler<? super MouseEvent> handler) {
        setOnMousePressed(handler);
        textInputControl.setOnMousePressed(handler);
    }

    public void filterMouseEventOnNonEditableText() {
        textInputControl.addEventFilter(MouseEvent.ANY, new WeakEventHandler<>(textInputControlMouseEventFilter));
    }

    /* --------------------------------------------------------------------- */
    // Style Management
    /* --------------------------------------------------------------------- */

    protected void removeBgStyles() {
        bg.getStyleClass().removeAll(BG_STYLE_CLASSES);
    }

    protected void removeDescriptionStyles() {
        descriptionLabel.getStyleClass().removeAll(DESCRIPTION_STYLE_CLASSES);
    }

    protected void removeTextInputStyles() {
        textInputControl.getStyleClass().removeAll(TEXT_INPUT_STYLE_CLASSES);
    }

    protected void applyBgStyle(String styleClass) {
        if (BG_STYLE_CLASSES.contains(styleClass)) {
            removeBgStyles();
            bg.getStyleClass().add(styleClass);
        }
    }

    /* --------------------------------------------------------------------- */
    // Mouse Event Handling
    /* --------------------------------------------------------------------- */

    protected void onMouseEntered() {
        if (textInputControl.isFocused() && textInputControl.isEditable()) {
            applyBgStyle("material-text-field-bg-selected");
        } else {
            applyBgStyle("material-text-field-bg-hover");
        }
    }

    protected void onMouseExited() {
        if (textInputControl.isFocused() && textInputControl.isEditable()) {
            applyBgStyle("material-text-field-bg-selected");
        } else {
            applyBgStyle("material-text-field-bg");
        }
    }

    protected void onInputTextFieldFocus(boolean focus) {
        if (textInputControl.isEditable()) {
            handleEditableFocusChange(focus);
        } else {
            super.requestFocus();
        }
    }

    private void handleEditableFocusChange(boolean focus) {
        if (focus) {
            resetValidation();
            applyBgStyle("material-text-field-bg-selected");
            showSelectionLine(true);
        } else {
            hideSelectionLine();
            applyStringConverter();
            validate();
            onMouseExited();
        }
        update();
    }

    private void applyStringConverter() {
        stringConverter.ifPresent(converter -> {
            try {
                setText(converter.toString(converter.fromString(getText())));
            } catch (Exception ignore) {
            }
        });
    }

    protected void onWidthChanged(double width) {
        if (width > 0) {
            updateComponentWidths(width);
            updateSelectionLineIfVisible(width);
        }
        layoutIconButton();
    }

    private void updateComponentWidths(double width) {
        bg.setPrefWidth(width);
        line.setPrefWidth(width);

        double iconWidth = iconButton.isVisible() ? 25 : 0;
        double labelWidth = width - 2 * descriptionLabel.getLayoutX();
        double textFieldWidth = width - 2 * textInputControl.getLayoutX() - iconWidth;

        descriptionLabel.setPrefWidth(labelWidth);
        textInputControl.setPrefWidth(textFieldWidth);
        helpLabel.setPrefWidth(labelWidth);
        errorLabel.setPrefWidth(labelWidth);
    }

    private void updateSelectionLineIfVisible(double width) {
        if (textInputControl.isFocused() && textInputControl.isEditable() && selectionLine.isVisible()) {
            selectionLine.setPrefWidth(width);
        }
    }

    /* --------------------------------------------------------------------- */
    // Layout
    /* --------------------------------------------------------------------- */

    protected void doLayout() {
        double bgHeight = getBgHeight();
        bg.setMinHeight(bgHeight);
        bg.setMaxHeight(bgHeight);
        line.setLayoutY(bgHeight - BORDER_LINE_OFFSET);
        selectionLine.setLayoutY(bgHeight - SELECTION_LINE_OFFSET);
        textInputControl.setLayoutY(getFieldLayoutY());
        helpLabel.setLayoutY(bgHeight + 3.5);
        errorLabel.setLayoutY(bgHeight + 3.5);
    }

    private void layoutIconButton() {
        if (iconButton.getHeight() > 0) {
            positionIconButton();
        } else {
            setupIconButtonHeightListener();
        }
    }

    private void positionIconButton() {
        if (getWidth() > 0 && iconButton.isManaged()) {
            double yPosition = calculateIconButtonYPosition();
            double xPosition = getWidth() - iconButton.getWidth() - 12 + iconButton.getPadding().getLeft();

            iconButton.setLayoutY(yPosition);
            iconButton.setLayoutX(xPosition);
        }
    }

    private double calculateIconButtonYPosition() {
        if (iconButton.getAlignment() == Pos.CENTER ||
                iconButton.getAlignment() == Pos.CENTER_LEFT ||
                iconButton.getAlignment() == Pos.CENTER_RIGHT) {
            return (getBgHeight() - iconButton.getHeight()) / 2 - 1;
        } else {
            return 6;
        }
    }

    private void setupIconButtonHeightListener() {
        iconButtonHeightListener = (observable, oldValue, newValue) -> {
            if (newValue.doubleValue() > 0) {
                layoutIconButton();
                UIThread.runOnNextRenderFrame(() ->
                        iconButton.heightProperty().removeListener(iconButtonHeightListener));
            }
        };
        iconButton.heightProperty().addListener(new WeakChangeListener<>(iconButtonHeightListener));
    }

    /* --------------------------------------------------------------------- */
    // Update Methods
    /* --------------------------------------------------------------------- */

    void update() {
        if (compactMode) {
            updateCompactMode();
        } else {
            updateNormalMode();
        }
    }

    private void updateCompactMode() {
        applyCompactModeStyles();
        updateEditableState();
        setOpacity(textInputControl.isDisabled() ? 0.35 : 1);
        UIThread.runOnNextRenderFrame(this::layoutIconButton);
        layout();
    }

    private void applyCompactModeStyles() {
        StringUtils.toOptional(descriptionLabel.getText()).ifPresent(text -> {
            removeDescriptionStyles();
            descriptionLabel.getStyleClass().add("material-text-field-description-small");
        });

        helpLabel.setVisible(false);
        helpLabel.setManaged(false);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        removeTextInputStyles();
        textInputControl.getStyleClass().add("material-text-field");
    }

    private void updateNormalMode() {
        animateDescriptionPosition();
        updateHelpLabelVisibility();
        applyNormalModeStyles();
        updateEditableState();
        setOpacity(textInputControl.isDisabled() ? 0.35 : 1);
        UIThread.runOnNextRenderFrame(this::layoutIconButton);
        layout();
    }

    private void animateDescriptionPosition() {
        StringUtils.toOptional(descriptionLabel.getText()).ifPresent(text -> {
            long duration = ManagedDuration.getOneSixthOfDefaultDurationMillis();
            double targetY = showInputTextField() ? 6.5 : 16.5;
            Transitions.animateLayoutY(descriptionLabel, targetY, duration, null);
        });
    }

    private void updateHelpLabelVisibility() {
        boolean shouldShowHelp = StringUtils.isNotEmpty(getHelpText()) &&
                StringUtils.isEmpty(errorLabel.getText());
        helpLabel.setVisible(shouldShowHelp);
        helpLabel.setManaged(helpLabel.isVisible());
    }

    private void applyNormalModeStyles() {
        removeDescriptionStyles();
        removeTextInputStyles();

        textInputControl.getStyleClass().add("material-text-field");

        if (showInputTextField()) {
            descriptionLabel.getStyleClass().add("material-text-field-description-small");
        } else {
            descriptionLabel.getStyleClass().add("material-text-field-description-big");
        }

        String focusStyle = textInputControl.isFocused() ?
                "material-text-field-description-selected" :
                "material-text-field-description-deselected";
        descriptionLabel.getStyleClass().add(focusStyle);
    }

    private void updateEditableState() {
        if (textInputControl.isEditable()) {
            bg.setMouseTransparent(false);
            bg.setOpacity(1);
            line.setOpacity(1);
        } else {
            bg.setMouseTransparent(true);
            bg.setOpacity(0.4);
            line.setOpacity(0.25);

            if (!compactMode) {
                descriptionLabel.getStyleClass().addAll(
                        "material-text-field-description-small",
                        "material-text-field-description-read-only"
                );
                textInputControl.getStyleClass().add("material-text-field-read-only");
            }
        }
    }

    private void setupVisualComponents(@Nullable String description, @Nullable String prompt,
                                       @Nullable String help, @Nullable String value) {
        bg.getStyleClass().add("material-text-field-bg");

        line.setPrefHeight(1);
        line.setStyle("-fx-background-color: -bisq-mid-grey-20");
        line.setMouseTransparent(true);

        setupSelectionLine();

        setupDescriptionLabel(description);
        setupTextInputControl(prompt, value);
        setupLabels(help);
        setupIconButton();

        getChildren().addAll(bg, line, selectionLine, descriptionLabel, textInputControl, iconButton, helpLabel, errorLabel);
    }

    private void setupSelectionLine() {
        selectionLine.setPrefWidth(0);
        selectionLine.setPrefHeight(2);
        selectionLine.getStyleClass().add("material-text-field-selection-line");
        selectionLine.setMouseTransparent(true);
        selectionLine.setVisible(false);
    }

    private void setupDescriptionLabel(@Nullable String description) {
        descriptionLabel.setLayoutX(16);
        descriptionLabel.setMouseTransparent(true);
        StringUtils.toOptional(description).ifPresent(descriptionLabel::setText);
    }

    private void setupTextInputControl(@Nullable String prompt, @Nullable String value) {
        StringUtils.toOptional(value).ifPresent(textInputControl::setText);
        textInputControl.setLayoutX(6.5);
        textInputControl.getStyleClass().add("material-text-field");
        StringUtils.toOptional(prompt).ifPresent(textInputControl::setPromptText);
    }

    private void setupLabels(@Nullable String help) {
        helpLabel.setLayoutX(16);
        helpLabel.getStyleClass().add("material-text-field-help");
        helpLabel.setMouseTransparent(true);
        StringUtils.toOptional(help).ifPresent(helpLabel::setText);

        errorLabel.setLayoutX(16);
        errorLabel.setMouseTransparent(true);
        errorLabel.getStyleClass().add("material-text-field-error");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

    private void setupIconButton() {
        iconButton.setAlignment(Pos.TOP_RIGHT);
        iconButton.setIcon("info");
        iconButton.setOpacity(0.6);
        iconButton.setManaged(false);
        iconButton.setVisible(false);
    }

    private void setupEventHandlers() {
        widthProperty().addListener(new WeakChangeListener<>(widthListener));
        textInputControl.focusedProperty().addListener(new WeakChangeListener<>(textInputControlFocusListener));
        descriptionLabel.textProperty().addListener(new WeakChangeListener<>(descriptionLabelTextListener));
        promptTextProperty().addListener(new WeakChangeListener<>(promptTextListener));
        helpProperty().addListener(new WeakChangeListener<>(helpListener));
        textInputControl.editableProperty().addListener(new WeakChangeListener<>(textInputControlEditableListener));
        disabledProperty().addListener(new WeakChangeListener<>(disabledListener));
        textInputControl.textProperty().addListener(new WeakChangeListener<>(textInputControlTextListener));

        setupMouseHandlers();
    }

    private void setupMouseHandlers() {
        bg.setOnMousePressed(e -> textInputControl.requestFocus());
        bg.setOnMouseEntered(e -> onMouseEntered());
        bg.setOnMouseExited(e -> onMouseExited());

        textInputControl.setOnMouseEntered(e -> onMouseEntered());
        textInputControl.setOnMouseExited(e -> onMouseExited());
    }

    private void applyCompactModeLayout(double height) {
        helpLabel.setVisible(false);
        helpLabel.setManaged(false);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        StringUtils.toOptional(descriptionLabel.getText())
                .ifPresent(text -> descriptionLabel.setLayoutY(2));

        textInputControl.setLayoutY(2);

        bg.setMinHeight(height);
        bg.setMaxHeight(height);
        line.setLayoutY(height - BORDER_LINE_OFFSET);
        selectionLine.setLayoutY(height - SELECTION_LINE_OFFSET);

        requestLayout();
    }

    private void ensureLayoutThenShowSelectionLine(boolean animate) {
        applyCss();
        layout();

        if (getWidth() > 0) {
            showSelectionLine(animate);
        } else {
            setupLayoutCompleteListener(animate);
        }
    }

    private void setupLayoutCompleteListener(boolean animate) {
        if (layoutWidthListener != null) {
            widthProperty().removeListener(layoutWidthListener);
        }

        layoutWidthListener = (observable, oldValue, newValue) -> {
            if (newValue.doubleValue() > 0) {
                widthProperty().removeListener(layoutWidthListener);
                layoutWidthListener = null;
                Platform.runLater(() -> showSelectionLine(animate));
            }
        };
        widthProperty().addListener(new WeakChangeListener<>(layoutWidthListener));
    }

    private void maintainSelectionLineVisibility() {
        if (textInputControl.isFocused() && textInputControl.isEditable()) {
            selectionLine.setVisible(true);
            selectionLine.setOpacity(1);
            selectionLine.setPrefWidth(getWidth());
        }
    }

    private void hideSelectionLine() {
        Transitions.fadeOut(selectionLine, 200);
    }

    private void applyValidationStyling(boolean hasError) {
        selectionLine.pseudoClassStateChanged(PSEUDO_CLASS_ERROR, hasError);
        descriptionLabel.pseudoClassStateChanged(PSEUDO_CLASS_ERROR, hasError);
        textInputControl.pseudoClassStateChanged(PSEUDO_CLASS_ERROR, hasError);
    }

    private void updateErrorDisplay(boolean isValid) {
        errorLabel.setVisible(!isValid);
        errorLabel.setManaged(errorLabel.isVisible());

        String errorMessage = getActiveValidator()
                .map(ValidatorBase::getMessage)
                .filter(StringUtils::isNotEmpty)
                .orElse("");
        errorLabel.setText(errorMessage);
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
        if (compactMode) {
            return compactHeight;
        }

        if (helpLabel.isManaged()) {
            return helpLabel.getLayoutY() + helpLabel.getHeight();
        } else if (errorLabel.isManaged()) {
            return errorLabel.getLayoutY() + errorLabel.getHeight();
        } else {
            return getBgHeight();
        }
    }

    @Override
    protected double computeMaxHeight(double width) {
        if (compactMode) {
            return compactHeight;
        }
        return computeMinHeight(width);
    }

    @Override
    protected double computePrefHeight(double width) {
        if (compactMode) {
            return compactHeight;
        }
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