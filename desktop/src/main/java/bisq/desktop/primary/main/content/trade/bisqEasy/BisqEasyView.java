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

package bisq.desktop.primary.main.content.trade.bisqEasy;

import bisq.desktop.common.view.NavigationView;
import bisq.desktop.layout.Layout;
import javafx.scene.layout.AnchorPane;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqEasyView extends NavigationView<AnchorPane, BisqEasyModel, BisqEasyController> {

    public BisqEasyView(BisqEasyModel model, BisqEasyController controller) {
        super(new AnchorPane(), model, controller);

        model.getView().addListener((observable, oldValue, newValue) -> {
            Layout.pinToAnchorPane(newValue.getRoot(), -34, 0, 0, -68);
            root.getChildren().clear();
            root.getChildren().add(newValue.getRoot());
        });
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
