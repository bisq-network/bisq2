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

package bisq.desktop.primary.main.content.settings;

import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.TabView;
import bisq.i18n.Res;
import com.jfoenix.controls.JFXTabPane;
import javafx.scene.control.Tab;

public class SettingsView extends TabView<JFXTabPane, SettingsModel, SettingsController> {

    public SettingsView(SettingsModel model, SettingsController controller) {
        super(new JFXTabPane(), model, controller);
    }

    @Override
    protected void createAndAddTabs() {
        Tab preferencesTab = createTab(Res.common.get("settings.preferences"), NavigationTarget.PREFERENCES);
        Tab networkTab = createTab(Res.common.get("settings.networkInfo"), NavigationTarget.NETWORK_INFO);
        Tab aboutTab = createTab(Res.common.get("settings.about"), NavigationTarget.ABOUT);
        root.getTabs().setAll(preferencesTab, networkTab, aboutTab);
    }

    @Override
    public void onViewAttached() {
        super.onViewAttached();
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
    }
}
