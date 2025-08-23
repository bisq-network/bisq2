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
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class WizardOverlay extends VBox {
    private static final double OVERLAY_WIDTH = 700;

    public WizardOverlay(String headline, Label headlineIcon, List<String> texts, Button... buttons) {
        this(headline, Optional.of(headlineIcon), texts, buttons);
    }

    public WizardOverlay(String headline, String text, Button... buttons) {
        this(headline, Optional.empty(), Collections.singletonList(text), buttons);
    }

    private WizardOverlay(String headline, Optional<Label> iconLabel, List<String> texts, Button... buttons) {
        VBox content = new VBox(40);
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().setAll("trade-wizard-feedback-bg");
        content.setPadding(new Insets(30));
        content.setMaxWidth(OVERLAY_WIDTH);

        HBox headlineBox = new HBox(15);
        headlineBox.setAlignment(Pos.CENTER);

        iconLabel.ifPresent(label -> headlineBox.getChildren().add(label));

        Label headlineLabel = new Label(Res.get(headline));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");
        VBox.setMargin(headlineLabel, new Insets(20, 0, 0, 0));
        headlineBox.getChildren().add(headlineLabel);

        VBox textBox = new VBox(10);
        texts.stream()
            .map(t -> {
                Label textLabel = new Label(Res.get(t));
                textLabel.setMinWidth(OVERLAY_WIDTH - 100);
                textLabel.setMaxWidth(textLabel.getMinWidth());
                textLabel.getStyleClass().addAll("normal-text", "wrap-text", "text-fill-grey-dimmed");
                return textLabel;
            })
            .forEach(textBox.getChildren()::add);

        HBox buttonsBox = new HBox(10, buttons);
        buttonsBox.setAlignment(Pos.CENTER);
        VBox.setMargin(buttonsBox, new Insets(15, 0, 10, 0));

        setSpacing(20);
        setVisible(false);
        setAlignment(Pos.TOP_CENTER);
        content.getChildren().addAll(headlineBox, textBox, buttonsBox);
        getChildren().addAll(content, Spacer.fillVBox());
    }

    public void updateOverlayVisibility(VBox content, boolean shouldShow) {
        if (shouldShow) {
            setVisible(true);
            setOpacity(1);
            Transitions.blurStrong(content, 0);
            Transitions.slideInTop(this, 450);
            // TODO: runnable
        } else {
            Transitions.removeEffect(content);
            if (isVisible()) {
                Transitions.fadeOut(this, ManagedDuration.getHalfOfDefaultDurationMillis(),
                        () -> setVisible(false));
            }
            // TODO: runnable
        }
    }
}
