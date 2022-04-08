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

package bisq.desktop.primary.main.content.settings.networkinfo;

import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.NavigationTargetTab;
import bisq.desktop.common.view.TabView;
import bisq.i18n.Res;
import com.jfoenix.controls.JFXTabPane;
import javafx.geometry.Insets;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NetworkInfoView extends TabView<JFXTabPane, NetworkInfoModel, NetworkInfoController> {

    public NetworkInfoView(NetworkInfoModel model, NetworkInfoController controller) {
        super(new JFXTabPane(), model, controller);

        root.setPadding(new Insets(20,0,0,0));

    }

    @Override
    protected void createAndAddTabs() {
        NavigationTargetTab clearNetTab = createTab(Res.get("clearNet"), NavigationTarget.CLEAR_NET);
        NavigationTargetTab torTab = createTab("Tor", NavigationTarget.TOR);
        NavigationTargetTab i2pTab = createTab("I2P", NavigationTarget.I2P);
        root.getTabs().setAll(clearNetTab, torTab, i2pTab);
    }

    @Override
    protected NavigationTargetTab createTab(String title, NavigationTarget navigationTarget) {
        NavigationTargetTab tab = super.createTab(title.toUpperCase(), navigationTarget);
        tab.setDisable(model.isDisabled(navigationTarget));
        return tab;
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
