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

package bisq.desktop.main.content.components;

import bisq.common.util.StringUtils;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
public class MaterialUserProfileSelection extends Pane {
    protected final Region bg = new Region();
    protected final Region line = new Region();
    protected final Region selectionLine = new Region();
    @Getter
    protected final Label descriptionLabel = new Label();
    protected final Pane userProfileSelectionRoot;
    @Getter
    protected final Label helpLabel = new Label();
    @Getter
    private final BisqIconButton iconButton = new BisqIconButton();
    private final UserProfileSelection userProfileSelection;

    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<Number> widthListener = (observable, oldValue, newValue) -> {
        onWidthChanged((double) newValue);
        layoutIconButton();
    };
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<Boolean> userProfileSelectionFocusedListener = (observable, oldValue, newValue) -> onUserProfileSelectionFocus(newValue);
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakInvalidationListener
    private final InvalidationListener descriptionLabelTextListener = (observable) -> update();
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakInvalidationListener
    private final InvalidationListener helpListener = (observable) -> update();
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakInvalidationListener
    private final InvalidationListener disabledListener = (observable) -> update();
    @SuppressWarnings("FieldCanBeLocal") // Need to keep a reference as used in WeakChangeListener
    private final ChangeListener<Number> iconButtonHeightListener = (observable, oldValue, newValue) -> {
        if (newValue.doubleValue() > 0) {
            layoutIconButton();
        }
    };

    public MaterialUserProfileSelection(UserProfileSelection userProfileSelection, String description) {
        this(userProfileSelection, description, null);
    }

    public MaterialUserProfileSelection(UserProfileSelection userProfileSelection,
                                        String description,
                                        @Nullable String help) {
        this.userProfileSelection = userProfileSelection;

        bg.getStyleClass().add("material-text-field-bg");

        line.setPrefHeight(1);
        line.setStyle("-fx-background-color: -bisq-mid-grey-20");
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

        userProfileSelectionRoot = userProfileSelection.getRoot();
        userProfileSelectionRoot.setLayoutX(6.5);
        userProfileSelectionRoot.getStyleClass().add("material-text-field");

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

        getChildren().addAll(bg, line, selectionLine, descriptionLabel, userProfileSelectionRoot, iconButton, helpLabel);

        widthProperty().addListener(new WeakChangeListener<>(widthListener));
        userProfileSelection.focusedProperty().addListener(new WeakChangeListener<>(userProfileSelectionFocusedListener));
        descriptionLabel.textProperty().addListener(new WeakInvalidationListener(descriptionLabelTextListener));
        helpProperty().addListener(new WeakInvalidationListener(helpListener));
        disabledProperty().addListener(new WeakInvalidationListener(disabledListener));

        bg.setOnMouseEntered(e -> onMouseEntered());
        bg.setOnMouseExited(e -> onMouseExited());
        userProfileSelectionRoot.setOnMouseEntered(e -> onMouseEntered());
        userProfileSelectionRoot.setOnMouseExited(e -> onMouseExited());

        userProfileSelection.setPrefWidth(500);

        doLayout();
        update();
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


    /* --------------------------------------------------------------------- */
    // Focus
    /* --------------------------------------------------------------------- */

    public ReadOnlyBooleanProperty userProfileSelectionFocusedProperty() {
        return userProfileSelection.focusedProperty();
    }

    public void requestFocus() {
        userProfileSelection.requestFocus();
    }

    public void deselect() {
    }


    /* --------------------------------------------------------------------- */
    // Event handlers
    /* --------------------------------------------------------------------- */

    protected void onMouseEntered() {
        removeBgStyles();
        if (userProfileSelection.isFocused()) {
            bg.getStyleClass().add("material-text-field-bg-selected");
        } else {
            bg.getStyleClass().add("material-text-field-bg-hover");
        }
    }

    protected void onMouseExited() {
        removeBgStyles();
        if (userProfileSelection.isFocused()) {
            bg.getStyleClass().add("material-text-field-bg-selected");
        } else {
            bg.getStyleClass().add("material-text-field-bg");
        }
    }

    protected void onUserProfileSelectionFocus(boolean focus) {
        if (focus) {
            selectionLine.setPrefWidth(0);
            selectionLine.setOpacity(1);
            Transitions.animatePrefWidth(selectionLine, getWidth());
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
            selectionLine.setPrefWidth(userProfileSelection.isFocused() ? width : 0);
            descriptionLabel.setPrefWidth(width - 2 * descriptionLabel.getLayoutX());
            double iconWidth = iconButton.isVisible() ? 25 : 0;
            userProfileSelectionRoot.setPrefWidth(width - 2 * userProfileSelectionRoot.getLayoutX() - iconWidth);
            helpLabel.setPrefWidth(width - 2 * helpLabel.getLayoutX());
        }
    }


    /* --------------------------------------------------------------------- */
    // Layout
    /* --------------------------------------------------------------------- */

    protected void doLayout() {
        bg.setMinHeight(getBgHeight());
        bg.setMaxHeight(getBgHeight());
        line.setLayoutY(getBgHeight() - 1);
        selectionLine.setLayoutY(getBgHeight() - 2);
        userProfileSelectionRoot.setLayoutY(getFieldLayoutY());
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
            iconButton.heightProperty().addListener(new WeakChangeListener<>(iconButtonHeightListener));
        }
    }

    void update() {
        if (StringUtils.isNotEmpty(descriptionLabel.getText())) {
            Transitions.animateLayoutY(descriptionLabel, 6.5, Transitions.DEFAULT_DURATION / 6d, null);
        }
        helpLabel.setVisible(StringUtils.isNotEmpty(helpProperty().get()));
        helpLabel.setManaged(StringUtils.isNotEmpty(helpProperty().get()));

        descriptionLabel.getStyleClass().remove("material-text-field-description-read-only");
        userProfileSelectionRoot.getStyleClass().remove("material-text-field-read-only");

        descriptionLabel.getStyleClass().remove("material-text-field-description-small");
        descriptionLabel.getStyleClass().remove("material-text-field-description-big");
        descriptionLabel.getStyleClass().remove("material-text-field-description-selected");
        descriptionLabel.getStyleClass().remove("material-text-field-description-deselected");
        descriptionLabel.getStyleClass().remove("material-text-field-description-read-only");

        descriptionLabel.getStyleClass().add("material-text-field-description-small");
        if (userProfileSelection.isFocused()) {
            descriptionLabel.getStyleClass().add("material-text-field-description-selected");
        } else {
            descriptionLabel.getStyleClass().add("material-text-field-description-deselected");
        }

        bg.setMouseTransparent(false);
        bg.setOpacity(1);
        line.setOpacity(1);
        userProfileSelectionRoot.getStyleClass().remove("material-text-field-read-only");
        setOpacity(userProfileSelectionRoot.isDisabled() ? 0.35 : 1);
        UIThread.runOnNextRenderFrame(this::layoutIconButton);
    }

    protected void removeBgStyles() {
        bg.getStyleClass().remove("material-text-field-bg-hover");
        bg.getStyleClass().remove("material-text-field-bg-selected");
        bg.getStyleClass().remove("material-text-field-bg");
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