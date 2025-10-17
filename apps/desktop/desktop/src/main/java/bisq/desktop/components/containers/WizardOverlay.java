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

package bisq.desktop.components.containers;

import bisq.desktop.common.ManagedDuration;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.main.content.bisq_easy.trade_wizard.TradeWizardView;
import bisq.i18n.Res;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.Getter;

import java.util.Arrays;

public class WizardOverlay extends VBox {
    public static final double OVERLAY_WIDTH = 700;

    private final Node owner;
    @Getter
    private Label headlineLabel = new Label("");
    private VBox textContentBox;
    private HBox buttonsBox;

    public WizardOverlay(Node owner) {
        this.owner = owner;
        getStyleClass().add("wizard-overlay");
    }

    public WizardOverlay warning() {
        ImageView icon = ImageUtil.getImageViewById("warning-white"); // default
        headlineLabel.setGraphic(icon);
        headlineLabel.getStyleClass().add("default-warning");
        return this;
    }

    public WizardOverlay yellowWarning() {
        ImageView icon = ImageUtil.getImageViewById("warning-yellow");
        headlineLabel.setGraphic(icon);
        headlineLabel.getStyleClass().add("yellow-warning");
        return this;
    }

    public WizardOverlay redWarning() {
        ImageView icon = ImageUtil.getImageViewById("warning-red");
        headlineLabel.setGraphic(icon);
        headlineLabel.getStyleClass().add("red-warning");
        return this;
    }

    public WizardOverlay greenWarning() {
        ImageView icon = ImageUtil.getImageViewById("warning-green");
        headlineLabel.setGraphic(icon);
        headlineLabel.getStyleClass().add("green-warning");
        return this;
    }

    public WizardOverlay headlineIcon(Node icon) {
        headlineLabel.setGraphic(icon);
        return this;
    }

    public WizardOverlay info() {
        ImageView icon = ImageUtil.getImageViewById("info-green"); // default
        headlineLabel.setGraphic(icon);
        headlineLabel.getStyleClass().add("default-info");
        return this;
    }

    public WizardOverlay headline(String headline) {
        headlineLabel.setText(Res.get(headline));
        return this;
    }

    public WizardOverlay description(String text) {
        textContentBox = createAndGetTextBox(text);
        return this;
    }

    public WizardOverlay description(String... texts) {
        textContentBox = createAndGetTextBox(texts);
        return this;
    }

    public WizardOverlay description(VBox textContent) {
        textContentBox = textContent;
        return this;
    }

    public WizardOverlay buttons(Button... buttons) {
        buttonsBox = createAndGetButtonsBox(buttons);
        return this;
    }

    public WizardOverlay build() {
        VBox content = createAndGetContentBox();
        setupWizardOverlay(content);
        return this;
    }

    public void updateOverlayVisibility(Node content,
                                        boolean shouldShow,
                                        EventHandler<? super KeyEvent> onKeyPressedHandler) {
        if (shouldShow) {
            setVisible(true);
            setOpacity(1);
            Transitions.blurStrong(content, 0);
            Transitions.slideInTop(this, 450);
            owner.requestFocus();
            owner.setOnKeyPressed(onKeyPressedHandler);
        } else {
            Transitions.removeEffect(content);
            if (isVisible()) {
                Transitions.fadeOut(this, ManagedDuration.getHalfOfDefaultDurationMillis(),
                        () -> setVisible(false));
            }
            owner.setOnKeyPressed(null);
            // Return the focus to the wizard
            if (owner.getParent() != null) {
                owner.getParent().requestFocus();
            }
        }
    }

    private VBox createAndGetContentBox() {
        VBox content = new VBox(40);

        if (headlineLabel != null) {
            headlineLabel.setGraphicTextGap(15);
            headlineLabel.getStyleClass().add("headline");
            HBox headlineBox = new HBox(headlineLabel);
            headlineBox.setAlignment(Pos.CENTER);
            VBox.setMargin(headlineBox, new Insets(20, 0, 0, 0));
            content.getChildren().add(headlineBox);
        }

        if (textContentBox != null) {
            content.getChildren().add(textContentBox);
        }

        if (buttonsBox != null) {
            content.getChildren().add(buttonsBox);
        }

        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().setAll("trade-wizard-feedback-bg");
        content.setPadding(new Insets(30));
        content.setMaxWidth(OVERLAY_WIDTH);
        return content;
    }

    private VBox createAndGetTextBox(String... texts) {
        VBox textBox = new VBox(15);
        Arrays.stream(texts)
                .map(t -> {
                    Label textLabel = new Label(Res.get(t));
                    textLabel.setMinWidth(OVERLAY_WIDTH - 100);
                    textLabel.setMaxWidth(textLabel.getMinWidth());
                    textLabel.getStyleClass().addAll("normal-text", "wrap-text", "text-fill-grey-dimmed");
                    return textLabel;
                })
                .forEach(textBox.getChildren()::add);
        VBox.setMargin(textBox, new Insets(0, 30, 0, 30));
        return textBox;
    }

    private HBox createAndGetButtonsBox(Button... buttons) {
        HBox buttonsBox = new HBox(10, buttons);
        buttonsBox.setAlignment(Pos.CENTER);
        VBox.setMargin(buttonsBox, new Insets(15, 0, 10, 0));
        return buttonsBox;
    }

    private void setupWizardOverlay(VBox content) {
        setSpacing(20);
        setVisible(false);
        setAlignment(Pos.TOP_CENTER);
        getChildren().addAll(content, Spacer.fillVBox());
        StackPane.setMargin(this, new Insets(-TradeWizardView.TOP_PANE_HEIGHT, 0, 0, 0));
    }
}
