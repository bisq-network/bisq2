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

package bisq.desktop.main.content.settings.network.transport;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.main.content.settings.SettingsViewUtils;
import bisq.i18n.Res;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class TransportView extends View<VBox, TransportModel, TransportController> {
    private final MaterialTextField myAddress;

    public TransportView(TransportModel model,
                         TransportController controller,
                         Pane traffic,
                         Pane systemLoad,
                         Pane connectionAndNodes) {
        super(new VBox(25), model, controller);

        Label headline = SettingsViewUtils.getHeadline(Res.get("settings.network.transport.headline." + model.getTransportType().name()));

        myAddress = new MaterialTextField(Res.get("settings.network.nodeInfo.myAddress"));
        myAddress.setEditable(false);
        myAddress.showCopyIcon();

        root.getChildren().addAll(headline,
                SettingsViewUtils.getLineAfterHeadline(getRoot().getSpacing()),
                myAddress,
                connectionAndNodes,
                traffic,
                systemLoad);
    }

    @Override
    protected void onViewAttached() {
        myAddress.setText(model.getMyDefaultNodeAddress());
    }

    @Override
    protected void onViewDetached() {
    }
}
