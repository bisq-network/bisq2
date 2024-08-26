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

package bisq.desktop.main.content.settings.display;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.Switch;
import bisq.desktop.main.content.settings.SettingsViewUtils;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DisplaySettingsView extends View<VBox, DisplaySettingsModel, DisplaySettingsController> {
    private static final double TEXT_FIELD_WIDTH = 500;

    private final Button resetDontShowAgain;
    private final Switch useAnimations, preventStandbyMode;

    public DisplaySettingsView(DisplaySettingsModel model, DisplaySettingsController controller) {
        super(new VBox(50), model, controller);

        root.setPadding(new Insets(0, 40, 40, 40));
        root.setAlignment(Pos.TOP_LEFT);

        Label displayHeadline = SettingsViewUtils.getHeadline(Res.get("settings.display.headline"));

        useAnimations = new Switch(Res.get("settings.display.useAnimations"));
        preventStandbyMode = new Switch(Res.get("settings.display.preventStandbyMode"));
        resetDontShowAgain = new Button(Res.get("settings.display.resetDontShowAgain"));
        resetDontShowAgain.getStyleClass().add("grey-transparent-outlined-button");

        VBox.setMargin(resetDontShowAgain, new Insets(10, 0, 0, 0));
        VBox displayVBox = new VBox(10, useAnimations, preventStandbyMode, resetDontShowAgain);

        Insets insets = new Insets(0, 5, 0, 5);
        VBox.setMargin(displayVBox, insets);
        root.getChildren().addAll(displayHeadline, SettingsViewUtils.getLineAfterHeadline(root.getSpacing()), displayVBox);
    }

    @Override
    protected void onViewAttached() {
        useAnimations.selectedProperty().bindBidirectional(model.getUseAnimations());
        preventStandbyMode.selectedProperty().bindBidirectional(model.getPreventStandbyMode());
        resetDontShowAgain.setOnAction(e -> controller.onResetDontShowAgain());
    }

    @Override
    protected void onViewDetached() {
        useAnimations.selectedProperty().unbindBidirectional(model.getUseAnimations());
        preventStandbyMode.selectedProperty().unbindBidirectional(model.getPreventStandbyMode());
        resetDontShowAgain.setOnAction(null);
    }
}
