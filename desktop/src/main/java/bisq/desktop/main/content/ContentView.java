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

package bisq.desktop.main.content;

import bisq.desktop.common.Layout;
import bisq.desktop.common.ViewTransition;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.NavigationView;
import bisq.desktop.common.view.View;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
public class ContentView extends NavigationView<AnchorPane, ContentModel, ContentController> {
    @Nullable
    private ViewTransition viewTransition;

    public ContentView(ContentModel model, ContentController controller) {
        super(new AnchorPane(), model, controller);

        root.setPadding(new Insets(33, 67, 67, 67));
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
            Layout.pinToAnchorPane(newValueRoot, 0, 0, 0, 0);
            if (viewTransition != null) {
                viewTransition.stop();
            }
            if (!root.getChildren().contains(newValueRoot)) {
                root.getChildren().add(newValueRoot);
            }
            Region oldValueRoot = oldValue != null ? oldValue.getRoot() : null;
            viewTransition = new ViewTransition(oldValueRoot, newValue);
        }
    }
}
