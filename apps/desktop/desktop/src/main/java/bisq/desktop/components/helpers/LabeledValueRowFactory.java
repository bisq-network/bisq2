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

package bisq.desktop.components.helpers;

import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.Optional;

public class LabeledValueRowFactory {

    public static final double DESCRIPTION_LABEL_WIDTH = 180;

    public static HBox createAndGetDescriptionAndValueBox(String descriptionKey, Node valueNode) {
        return createAndGetDescriptionAndValueBox(descriptionKey, valueNode, Optional.empty());
    }

    public static HBox createAndGetDescriptionAndValueBox(String descriptionKey,
                                                          Node detailsNode,
                                                          BisqMenuItem button) {
        return createAndGetDescriptionAndValueBox(descriptionKey, detailsNode, Optional.of(button));
    }

    public static HBox createAndGetDescriptionAndValueBox(String descriptionKey,
                                                          Node detailsNode,
                                                          Optional<BisqMenuItem> button) {
        return createAndGetDescriptionAndValueBox(getDescriptionLabel(Res.get(descriptionKey)), detailsNode, button);
    }

    public static HBox createAndGetDescriptionAndValueBox(Label descriptionLabel,
                                                          Node detailsNode,
                                                          BisqMenuItem button) {
        return createAndGetDescriptionAndValueBox(descriptionLabel, detailsNode, Optional.of(button));
    }

    public static HBox createAndGetDescriptionAndValueBox(Label descriptionLabel,
                                                          Node detailsNode,
                                                          Optional<BisqMenuItem> button) {
        return createAndGetDescriptionAndValueBox(descriptionLabel, detailsNode, button.map(List::of).orElse(List.of()));
    }

    public static HBox createAndGetDescriptionAndValueBox(Label descriptionLabel,
                                                          Node detailsNode,
                                                          List<BisqMenuItem> buttons) {
        descriptionLabel.setMaxWidth(DESCRIPTION_LABEL_WIDTH);
        descriptionLabel.setMinWidth(DESCRIPTION_LABEL_WIDTH);
        descriptionLabel.setPrefWidth(DESCRIPTION_LABEL_WIDTH);

        HBox hBox = getValueBox(descriptionLabel, detailsNode);

        if (!buttons.isEmpty()) {
            HBox hBoxButtons = new HBox(5);
            hBoxButtons.getChildren().addAll(buttons);
            buttons.forEach(button -> button.useIconOnly(17));
            HBox.setMargin(hBoxButtons, new Insets(0, 0, 0, 40));
            hBox.getChildren().addAll(Spacer.fillHBox(), hBoxButtons);
        }
        return hBox;
    }

    public static Label getDescriptionLabel(String description) {
        Label label = new Label(description);
        label.getStyleClass().addAll("text-fill-grey-dimmed", "medium-text", "font-light");
        return label;
    }

    public static Label getValueLabel() {
        return getValueLabel(ValueLabelStyle.NORMAL);
    }

    public static Label getValueLabel(ValueLabelStyle style, String text) {
        Label label = getValueLabel(style);
        label.setText(text);
        return label;
    }

    public static Label getValueLabel(ValueLabelStyle style) {
        Label label = new Label();
        label.getStyleClass().addAll(style.textColor, style.textSize, "font-light");
        return label;
    }

    public static HBox getValueBox(Node... children) {
        HBox box = new HBox(5, children);
        box.setAlignment(Pos.BASELINE_LEFT);
        return box;
    }

    public static BisqMenuItem getCopyButton(String tooltip) {
        BisqMenuItem bisqMenuItem = new BisqMenuItem("copy-grey", "copy-white");
        bisqMenuItem.setTooltip(tooltip);
        return bisqMenuItem;
    }

    public static Region createSeparatorLine() {
        Region line = new Region();
        line.setMinHeight(1);
        line.setMaxHeight(1);
        line.getStyleClass().add("separator-line");
        line.setPadding(new Insets(9, 0, 8, 0));
        return line;
    }

    //
    // Style enums
    //

    public enum ValueLabelStyle {

        NORMAL("normal-text", "text-fill-white"),
        SMALL("small-text", "text-fill-white"),
        DIMMED("normal-text", "text-fill-grey-dimmed"),
        SMALL_DIMMED("small-text", "text-fill-grey-dimmed");

        private final String textColor, textSize;

        ValueLabelStyle(String textSize, String textColor) {
            this.textColor = textColor;
            this.textSize = textSize;
        }
    }
}
