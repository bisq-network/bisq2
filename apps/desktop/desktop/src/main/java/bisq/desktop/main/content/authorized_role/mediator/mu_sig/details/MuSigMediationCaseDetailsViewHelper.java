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

package bisq.desktop.main.content.authorized_role.mediator.mu_sig.details;

import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import java.util.Optional;

public class MuSigMediationCaseDetailsViewHelper {

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
        double width = 180;
        descriptionLabel.setMaxWidth(width);
        descriptionLabel.setMinWidth(width);
        descriptionLabel.setPrefWidth(width);

        HBox hBox = new HBox(descriptionLabel, detailsNode);
        hBox.setAlignment(Pos.BASELINE_LEFT);

        if (button.isPresent()) {
            button.get().useIconOnly(17);
            HBox.setMargin(button.get(), new Insets(0, 0, 0, 40));
            hBox.getChildren().addAll(Spacer.fillHBox(), button.get());
        }
        return hBox;
    }

    public static Label getDescriptionLabel(String description) {
        Label label = new Label(description);
        label.getStyleClass().addAll("text-fill-grey-dimmed", "medium-text", "font-light");
        return label;
    }

    public static Label getValueLabel() {
        Label label = new Label();
        label.getStyleClass().addAll("text-fill-white", "normal-text", "font-light");
        return label;
    }

    public static BisqMenuItem getCopyButton(String tooltip) {
        BisqMenuItem bisqMenuItem = new BisqMenuItem("copy-grey", "copy-white");
        bisqMenuItem.setTooltip(tooltip);
        return bisqMenuItem;
    }

    public static Region getLine() {
        Region line = new Region();
        line.setMinHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: -bisq-border-color-grey");
        line.setPadding(new Insets(9, 0, 8, 0));
        return line;
    }
}
