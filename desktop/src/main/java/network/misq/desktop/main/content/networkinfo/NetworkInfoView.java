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

package network.misq.desktop.main.content.networkinfo;

import com.jfoenix.controls.JFXTabPane;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import lombok.extern.slf4j.Slf4j;
import network.misq.desktop.common.view.View;
import network.misq.desktop.main.content.networkinfo.transport.TransportTypeView;
import network.misq.i18n.Res;
import network.misq.network.p2p.node.transport.Transport;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class NetworkInfoView extends View<JFXTabPane, NetworkInfoModel, NetworkInfoController> {
    private final Map<Transport.Type, Tab> tabByTransportType = new HashMap<>();
    private final ChangeListener<Optional<TransportTypeView>> transportTypeViewChangeListener;
    private final ChangeListener<Tab> tabChangeListener;

    public NetworkInfoView(NetworkInfoModel model, NetworkInfoController controller) {
        super(new JFXTabPane(), model, controller);

        TabPane tabPane = getRoot();
        Tab clearNetTab = createTab(Transport.Type.CLEAR, Res.network.get("clearNet"));
        Tab torTab = createTab(Transport.Type.TOR, "Tor");
        Tab i2pTab = createTab(Transport.Type.I2P, "I2P");
        tabPane.getTabs().addAll(clearNetTab, torTab, i2pTab);

        tabChangeListener = (observable, oldValue, newValue) -> {
            controller.onTabSelected(Optional.ofNullable(newValue).map(tab -> Transport.Type.valueOf(tab.getId())));
        };

        transportTypeViewChangeListener = (observable, oldValue, transportTypeViewOptional) -> {
            Optional<Tab> tabOptional = model.getSelectedTransportType().flatMap(e -> Optional.ofNullable(tabByTransportType.get(e)));
            tabOptional.ifPresent(tab -> tab.setContent(transportTypeViewOptional.map(View::getRoot).orElse(null)));
            tabPane.getSelectionModel().select(tabOptional.orElse(null));
            tabPane.requestFocus();
        };
    }

    @Override
    public void activate() {
        TabPane tabPane = getRoot();
        model.getTransportTypeView().addListener(transportTypeViewChangeListener);
        tabPane.getSelectionModel().selectedItemProperty().addListener(tabChangeListener);

        Tab clearNetTab = tabByTransportType.get(Transport.Type.CLEAR);
        clearNetTab.disableProperty().bind(model.getClearNetDisabled());
        Tab torTab = tabByTransportType.get(Transport.Type.TOR);
        torTab.disableProperty().bind(model.getTorDisabled());
        Tab i2pTab = tabByTransportType.get(Transport.Type.I2P);
        i2pTab.disableProperty().bind(model.getI2pDisabled());

        if (!model.getClearNetDisabled().get()) {
            tabPane.getSelectionModel().select(clearNetTab);
        } else if (!model.getTorDisabled().get()) {
            tabPane.getSelectionModel().select(torTab);
        } else if (!model.getI2pDisabled().get()) {
            tabPane.getSelectionModel().select(i2pTab);
        }
    }

    @Override
    protected void deactivate() {
        model.getTransportTypeView().removeListener(transportTypeViewChangeListener);
        getRoot().getSelectionModel().selectedItemProperty().removeListener(tabChangeListener);

        tabByTransportType.values().forEach(tab -> tab.disableProperty().unbind());
    }

    private Tab createTab(Transport.Type transportType, String title) {
        Tab tab = new Tab(title.toUpperCase());
        tab.setClosable(false);
        tab.setId(transportType.name());
        tabByTransportType.put(transportType, tab);
        return tab;
    }
}
