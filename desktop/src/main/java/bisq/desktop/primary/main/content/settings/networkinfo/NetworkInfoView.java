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

import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.NavigationTargetTab;
import bisq.desktop.common.view.TabView;
import bisq.desktop.common.view.View;
import bisq.desktop.primary.main.content.settings.networkinfo.transport.TransportTypeView;
import bisq.i18n.Res;
import com.jfoenix.controls.JFXTabPane;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.Tab;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class NetworkInfoView extends TabView<JFXTabPane, NetworkInfoModel, NetworkInfoController> {
    private final Map<NavigationTarget, Tab> tabByNavigationTarget = new HashMap<>();
    private final ChangeListener<Optional<TransportTypeView>> transportTypeViewChangeListener;
   // private final ChangeListener<Tab> tabChangeListener;

    public NetworkInfoView(NetworkInfoModel model, NetworkInfoController controller) {
        super(new JFXTabPane(), model, controller);

        root.setPadding(new Insets(20, 20, 20, 0));

      /*  tabChangeListener = (observable, oldValue, newValue) -> {
            Optional.ofNullable(newValue).ifPresent(tab -> controller.onTabSelected((NavigationTarget) tab.getUserData()));
        };*/

        transportTypeViewChangeListener = (observable, oldValue, transportTypeViewOptional) -> {
            Optional<Tab> tabOptional = model.getSelectedTransportType().flatMap(e -> Optional.ofNullable(tabByNavigationTarget.get(e)));
            tabOptional.ifPresent(tab -> tab.setContent(transportTypeViewOptional.map(View::getRoot).orElse(null)));
            root.getSelectionModel().select(tabOptional.orElse(null));
            root.requestFocus();
        };
    }

    @Override
    protected void createAndAddTabs() {
        NavigationTargetTab clearNetTab = createTab(Res.network.get("clearNet"), NavigationTarget.CLEAR_NET);
        NavigationTargetTab torTab = createTab("Tor", NavigationTarget.TOR);
        NavigationTargetTab i2pTab = createTab("I2P", NavigationTarget.I2P);
        root.getTabs().setAll(clearNetTab, torTab, i2pTab);
    }

    @Override
    protected NavigationTargetTab createTab(String title, NavigationTarget navigationTarget) {
        NavigationTargetTab tab = super.createTab(title.toUpperCase(), navigationTarget);
        tabByNavigationTarget.put(navigationTarget, tab);
        return tab;
    }

    @Override
    public void onViewAttached() {
        super.onViewAttached();

        model.getTransportTypeView().addListener(transportTypeViewChangeListener);
        Tab clearNetTab = tabByNavigationTarget.get(NavigationTarget.CLEAR_NET);
        clearNetTab.disableProperty().bind(model.getClearNetDisabled());
        Tab torTab = tabByNavigationTarget.get(NavigationTarget.TOR);
        torTab.disableProperty().bind(model.getTorDisabled());
        Tab i2pTab = tabByNavigationTarget.get(NavigationTarget.I2P);
        i2pTab.disableProperty().bind(model.getI2pDisabled());

        if (!model.getClearNetDisabled().get()) {
            root.getSelectionModel().select(clearNetTab);
        } else if (!model.getTorDisabled().get()) {
            root.getSelectionModel().select(torTab);
        } else if (!model.getI2pDisabled().get()) {
            root.getSelectionModel().select(i2pTab);
        }
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        model.getTransportTypeView().removeListener(transportTypeViewChangeListener);
        tabByNavigationTarget.values().forEach(tab -> tab.disableProperty().unbind());
    }
}
