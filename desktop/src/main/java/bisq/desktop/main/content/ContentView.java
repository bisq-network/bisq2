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

import bisq.desktop.common.ViewTransition;
import bisq.desktop.common.view.NavigationView;
import bisq.desktop.main.content.chat.ChatView;
import javafx.geometry.Insets;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContentView extends NavigationView<StackPane, ContentModel, ContentController> {
    private ViewTransition viewTransition;

    public ContentView(ContentModel model, ContentController controller) {
        super(new StackPane(), model, controller);

        // We only get created once my MainView after the splashscreen and then never get removed, 
        // so we do not need to remove the listener.
        model.getView().addListener((observable, oldValue, newValue) -> {
            Region newValueRoot = newValue.getRoot();
            if (!(newValue instanceof ChatView)) {
                StackPane.setMargin(newValueRoot, new Insets(33, 67, 67, 67));
            }

            if (viewTransition != null) {
                viewTransition.stop();
            }

            if (!root.getChildren().contains(newValueRoot)) {
                root.getChildren().add(newValueRoot);
            } else {
                log.warn("We did not add the new child view as we still had it in out children list. " +
                        "This should not happen as the viewTransition.stop() call should remove any old dangling child view. New child view={}", newValue);
            }
            Region oldValueRoot = oldValue != null ? oldValue.getRoot() : null;
            viewTransition = new ViewTransition(oldValueRoot, newValue);
        });
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
