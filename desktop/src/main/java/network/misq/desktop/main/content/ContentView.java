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

package network.misq.desktop.main.content;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import lombok.extern.slf4j.Slf4j;
import network.misq.desktop.common.view.Controller;
import network.misq.desktop.common.view.Model;
import network.misq.desktop.common.view.View;

@Slf4j
public class ContentView extends View<HBox, ContentViewModel, ContentViewController> {

    public ContentView(ContentViewModel model, ContentViewController controller) {
        super(new HBox(), model, controller);

        model.view.addListener(new ChangeListener<View<? extends Node, Model, Controller>>() {
            @Override
            public void changed(ObservableValue<? extends View<? extends Node, Model, Controller>> observable,
                                View<? extends Node, Model, Controller> oldValue,
                                View<? extends Node, Model, Controller> newValue) {
                HBox.setHgrow(newValue.getRoot(), Priority.ALWAYS);
                ObservableList<Node> children = root.getChildren();
                children.setAll(newValue.getRoot());
            }
        });

     /*   Navigation.addListener((viewPath, data) -> {
            if (viewPath.size() != 2 || viewPath.indexOf(MainView.class) != 0)
                return;

            Class<? extends View> viewClass = viewPath.tip();
            if (viewClass == null) {
                return;
            }
            try {
                View view = ViewLoader.load(viewClass);
                HBox.setHgrow(view.getRoot(), Priority.ALWAYS);
                ObservableList<Node> children = root.getChildren();
                children.setAll(view.getRoot());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });*/
    }
}
