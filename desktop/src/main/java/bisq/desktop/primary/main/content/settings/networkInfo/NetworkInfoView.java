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

package bisq.desktop.primary.main.content.settings.networkInfo;

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;


@Slf4j
public class NetworkInfoView extends View<VBox, NetworkInfoModel, NetworkInfoController> {

    private final Accordion accordion;

    public NetworkInfoView(NetworkInfoModel model, NetworkInfoController controller,
                           Optional<Node> clear,
                           Optional<Node> tor,
                           Optional<Node> i2p) {
        super(new VBox(), model, controller);

        root.setFillWidth(true);
        root.setSpacing(20);
        accordion = new Accordion();
        clear.ifPresent(childRoot -> {
            TitledPane titledPane = new TitledPane(Res.get("clearNet"), childRoot);
            accordion.getPanes().add(titledPane);
        });
        tor.ifPresent(childRoot -> accordion.getPanes().add(new TitledPane(Res.get("tor"), childRoot)));
        i2p.ifPresent(childRoot -> accordion.getPanes().add(new TitledPane(Res.get("i2p"), childRoot)));

        if (!accordion.getPanes().isEmpty()) {
            UIThread.runOnNextRenderFrame(() -> accordion.getPanes().get(0).setExpanded(true));
        }

        root.getChildren().addAll(accordion);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
