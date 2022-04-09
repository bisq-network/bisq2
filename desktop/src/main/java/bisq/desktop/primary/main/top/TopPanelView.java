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

package bisq.desktop.primary.main.top;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

public class TopPanelView extends View<HBox, TopPanelModel, TopPanelController> {
    public static final int HEIGHT = 85;

    public TopPanelView(TopPanelModel model,
                        TopPanelController controller,
                        Pane marketPriceBox,
                        Pane walletBalanceBox) {
        super(new HBox(), model, controller);

        root.setPrefHeight(HEIGHT);
        root.setSpacing(28);
        root.setStyle("-fx-background-color: -bisq-dark-bg;");
        root.setPadding(new Insets(28, 100, 0, 100));
        marketPriceBox.setPadding(new Insets(-16, 0, 0, 0));
        root.getChildren().addAll(marketPriceBox, Spacer.fillHBox(), walletBalanceBox);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
