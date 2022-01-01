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

package network.misq.desktop.main.content.settings;

import javafx.scene.layout.VBox;
import network.misq.desktop.common.view.View;

public class SettingsView extends View<VBox, SettingsModel, SettingsController> {

    public SettingsView(SettingsModel model, SettingsController controller) {
        super(new VBox(), model, controller);
    }

    @Override
    public void onAddedToStage() {
        controller.onViewAdded();
    }

    @Override
    protected void onRemovedFromStage() {
        controller.onViewRemoved();
    }

    @Override
    protected void setupView() {
    }

    @Override
    protected void configModel() {
        // tableView.sort();
    }

    @Override
    protected void configController() {
    }
}
