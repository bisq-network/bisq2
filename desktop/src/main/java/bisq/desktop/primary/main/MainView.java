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

package bisq.desktop.primary.main;

import bisq.desktop.common.view.NavigationView;
import bisq.desktop.layout.Layout;
import bisq.desktop.primary.main.top.TopPanelView;
import javafx.scene.layout.*;

public class MainView extends NavigationView<HBox, MainModel, MainController> {
    public MainView(MainModel model,
                    MainController controller,
                    Pane leftNavView,
                    Pane topPanelView) {
        super(new HBox(), model, controller);

        root.setFillHeight(true);
        AnchorPane topPanelAndContentBox = new AnchorPane();
        topPanelAndContentBox.getChildren().add(topPanelView);
        Layout.pinToAnchorPane(topPanelView, 0,0,null,0);

        model.getView().addListener((observable, oldValue, contentView) -> {
            Region child = contentView.getRoot();
            HBox.setHgrow(child, Priority.ALWAYS);
            Layout.pinToAnchorPane(child, TopPanelView.HEIGHT,0,0,0);
            topPanelAndContentBox.getChildren().add(child);
        });

        HBox.setHgrow(topPanelAndContentBox, Priority.ALWAYS);
        root.getChildren().addAll(leftNavView, topPanelAndContentBox);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
