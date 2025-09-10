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

package bisq.desktop.main.content.network.peers;

import bisq.desktop.common.view.View;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NetworkPeersView extends View<VBox, NetworkPeersModel, NetworkPeersController> {
    private static final double SIDE_PADDING = 40;

    public NetworkPeersView(NetworkPeersModel model,
                            NetworkPeersController controller,
                            VBox allPeers) {
        super(new VBox(40), model, controller);

        this.root.setPadding(new Insets(0, SIDE_PADDING, 0, SIDE_PADDING));
        this.root.getChildren().addAll(allPeers);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
