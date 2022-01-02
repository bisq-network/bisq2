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

import javafx.beans.value.ChangeListener;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import network.misq.desktop.common.view.View;
import network.misq.i18n.Res;

public class NetworkInfoView extends View<VBox, NetworkInfoModel, NetworkInfoController> {
    private final TabPane tabPane;
    private ChangeListener<SingleSelectionModel<Tab>> tabChangeListener;

    public NetworkInfoView(NetworkInfoModel model, NetworkInfoController controller) {
        super(new VBox(), model, controller);

        tabPane = new TabPane();
        Tab clearNet = new Tab(Res.network.get("clearNet"));
        clearNet.setId(NetworkInfoTab.CLEAR_NET.name());
        Tab tor = new Tab("Tor");
        tor.setId(NetworkInfoTab.TOR.name());
        Tab i2p = new Tab("I2P");
        i2p.setId(NetworkInfoTab.I2P.name());
        tabPane.getTabs().addAll(clearNet, tor, i2p);

        tabChangeListener = (observable, oldValue, newValue) ->
                controller.onTabSelected(newValue.getSelectedItem().getId());

        root.getChildren().addAll(tabPane);
    }

    @Override
    public void activate() {
        tabPane.selectionModelProperty().addListener(tabChangeListener);
    }

    @Override
    protected void deactivate() {
        tabPane.selectionModelProperty().removeListener(tabChangeListener);
    }
}
