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

package bisq.desktop.main.content.settings;

import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import com.jfoenix.controls.JFXTabPane;
import javafx.beans.value.ChangeListener;
import javafx.scene.Parent;
import javafx.scene.control.Tab;

public class SettingsView extends View<JFXTabPane, SettingsModel, SettingsController> {
    private final ChangeListener<Tab> tabChangeListener;
    private final ChangeListener<View<? extends Parent, ? extends Model, ? extends Controller>> viewChangeListener;

    public SettingsView(SettingsModel model, SettingsController controller) {
        super(new JFXTabPane(), model, controller);

        Tab preferencesTab = createTab(Res.common.get("settings.preferences"), NavigationTarget.PREFERENCES);
        Tab networkTab = createTab(Res.common.get("settings.networkInfo"), NavigationTarget.NETWORK_INFO);
        Tab aboutTab = createTab(Res.common.get("settings.about"), NavigationTarget.ABOUT);
        root.getTabs().addAll(preferencesTab, networkTab, aboutTab);

        viewChangeListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                Tab tab = getTab(model.getSelectedNavigationTarget());
                tab.setContent(newValue.getRoot());
                root.getSelectionModel().select(tab);
            }
        };
        tabChangeListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                controller.onTabSelected(NavigationTarget.valueOf(newValue.getId()));
            }
        };
    }


    @Override
    public void activate() {
        model.getView().addListener(viewChangeListener);
        root.getSelectionModel().selectedItemProperty().addListener(tabChangeListener);
    }

    @Override
    protected void deactivate() {
        model.getView().removeListener(viewChangeListener);
        root.getSelectionModel().selectedItemProperty().removeListener(tabChangeListener);
    }

    private Tab createTab(String title, NavigationTarget navigationTarget) {
        Tab tab = new Tab(title.toUpperCase());
        tab.setClosable(false);
        tab.setId(navigationTarget.name());
        return tab;
    }

    private Tab getTab(NavigationTarget navigationTarget) {
        return root.getTabs().stream()
                .filter(tab -> tab.getId().equals(navigationTarget.name()))
                .findAny()
                .orElseThrow();
    }
}
