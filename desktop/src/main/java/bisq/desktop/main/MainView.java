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

package bisq.desktop.main;

import bisq.desktop.common.ViewTransition;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.NavigationView;
import bisq.desktop.common.view.View;
import javafx.scene.Parent;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
public class MainView extends NavigationView<HBox, MainModel, MainController> {
    private final VBox vBox;
    @Nullable
    private ViewTransition viewTransition;

    public MainView(MainModel model,
                    MainController controller,
                    Pane leftNavView,
                    Pane topPanelView) {
        super(new HBox(), model, controller);

        vBox = new VBox(topPanelView);
        HBox.setHgrow(vBox, Priority.ALWAYS);
        root.getChildren().addAll(leftNavView, vBox);

        model.getView().addListener((observable, oldValue, newValue) -> onChildViewChanged(oldValue, newValue));
    }

    @Override
    protected void onViewAttached() {
        onChildViewChanged(null, model.getView().get());
    }

    @Override
    protected void onViewDetached() {
    }

    private void onChildViewChanged(@Nullable View<? extends Parent, ? extends Model, ? extends Controller> oldValue, View<? extends Parent, ? extends Model, ? extends Controller> newValue) {
        if (newValue != null) {
            Region newValueRoot = newValue.getRoot();
            VBox.setVgrow(newValueRoot, Priority.ALWAYS);
            if (viewTransition != null) {
                viewTransition.stop();
            }
            if (!vBox.getChildren().contains(newValueRoot)) {
                vBox.getChildren().add(newValueRoot);
            }
            Region oldValueRoot = oldValue != null ? oldValue.getRoot() : null;
            viewTransition = new ViewTransition(oldValueRoot, newValue);
        }
    }
}
