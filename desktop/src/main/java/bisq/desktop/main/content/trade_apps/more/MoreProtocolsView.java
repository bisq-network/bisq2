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

package bisq.desktop.main.content.trade_apps.more;

import bisq.desktop.common.view.View;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class MoreProtocolsView extends View<VBox, MoreProtocolsModel, MoreProtocolsController> {
    private Subscription viewPin;

    public MoreProtocolsView(MoreProtocolsModel model, MoreProtocolsController controller) {
        super(new VBox(10), model, controller);
    }

    @Override
    protected void onViewAttached() {
        viewPin = EasyBind.subscribe(model.getProtocolRoadmapView(), view -> {
            if (view != null) {
                root.getChildren().clear();
                root.getChildren().add(view.getRoot());
            }
        });
    }

    @Override
    protected void onViewDetached() {
        viewPin.unsubscribe();
    }
}
