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
import bisq.desktop.common.Transitions;
import bisq.desktop.common.view.NavigationView;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScrollableContentView extends NavigationView<ScrollPane, ScrollableContentModel, ScrollableContentController> {

    public ScrollableContentView(ScrollableContentModel model, ScrollableContentController controller) {
        super(new ScrollPane(), model, controller);

        root.setFitToWidth(true);
        root.setFitToHeight(false);

        Pane anchorPane = new VBox();
        root.setContent(anchorPane);

        anchorPane.setPadding(new Insets(33, 67, 67, 67));
        model.getView().addListener((observable, oldValue, newValue) -> {
            Layout.pinToAnchorPane(newValue.getRoot(), 0, 0, 0, 0);

            Parent parent = newValue.getRoot().getParent();
            // At fast navigation changes we might have not removed the new view and would get 
            // an exception, so we remove it from its parent first. The instanceof check for parent only returns true 
            // if parent is not null. 
            try {
                if (parent instanceof Pane) {
                    Region root1 = newValue.getRoot();
                    log.error("root1 {}", root1);
                    log.error(" ((Pane) parent).getChildren() {}", ((Pane) parent).getChildren());
                    ((Pane) parent).getChildren().remove(root1);
                    //todo still issues with fadeout animation... need to return the transition and stop it...
                }
                anchorPane.getChildren().add(newValue.getRoot());
                Transitions.transitContentViews(oldValue, newValue);
            } catch (Exception e) {
                log.error(e.toString());
            }
        });
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
