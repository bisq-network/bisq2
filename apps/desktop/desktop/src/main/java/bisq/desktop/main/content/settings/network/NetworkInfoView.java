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

package bisq.desktop.main.content.settings.network;

import bisq.desktop.common.view.View;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;


@Slf4j
public class NetworkInfoView extends View<VBox, NetworkInfoModel, NetworkInfoController> {

    public NetworkInfoView(NetworkInfoModel model,
                           NetworkInfoController controller,
                           VBox versionDistribution,
                           Optional<Node> clear,
                           Optional<Node> tor,
                           Optional<Node> i2p) {
        super(new VBox(50), model, controller);

        root.setPadding(new Insets(0, 40, 40, 40));
        root.setAlignment(Pos.TOP_LEFT);

        clear.ifPresent(childRoot -> root.getChildren().add(childRoot));
        tor.ifPresent(childRoot -> root.getChildren().add(childRoot));
        i2p.ifPresent(childRoot -> root.getChildren().add(childRoot));

        root.getChildren().addAll(versionDistribution);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
