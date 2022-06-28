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

package bisq.desktop.primary.main.content.settings.preferences;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqToggleButton;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PreferencesView extends View<VBox, PreferencesModel, PreferencesController> {

    private final BisqToggleButton resetDontShowAgain;

    public PreferencesView(PreferencesModel model, PreferencesController controller) {
        super(new VBox(20), model, controller);

        root.setAlignment(Pos.TOP_LEFT);
        //root.setPadding(new Insets(30, 30, 30, 30));

        Label headlineLabel = new Label(Res.get("settings.preferences.displaySettings"));
        headlineLabel.getStyleClass().addAll("bisq-text-headline-2", "wrap-text");

        resetDontShowAgain = new BisqToggleButton(Res.get("settings.preferences.resetDontShowAgain"));
        VBox.setMargin(headlineLabel, new Insets(30, 0, 0, 0));
        root.getChildren().addAll(headlineLabel, resetDontShowAgain);
    }

    @Override
    protected void onViewAttached() {
        resetDontShowAgain.setSelected(false);
        resetDontShowAgain.setOnAction(e -> controller.onResetDontShowAgain(resetDontShowAgain.isSelected()));
    }

    @Override
    protected void onViewDetached() {
    }
}
