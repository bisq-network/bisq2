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

package bisq.desktop.main.content.authorized_role.mediator;

import bisq.desktop.common.Styles;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.TabView;
import bisq.desktop.common.view.View;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MediatorTabView extends TabView<MediatorTabModel, MediatorTabController> {
    public MediatorTabView(MediatorTabModel model, MediatorTabController controller) {
        super(model, controller);

        root.setPadding(new Insets(20, 40, 20, 40));
        root.setAlignment(Pos.TOP_LEFT);
        root.getStyleClass().add("manager-tab-view");

        Styles styles = new Styles("bisq-text-grey-9", "bisq-text-white", "bisq-text-green", "bisq-text-grey-9");
        addTab(Res.get("navigation.bisqEasy"),
                NavigationTarget.BISQ_EASY_MEDIATOR,
                styles);

        // We apply the height of the viewpoint according the content height as we delegate the scrolling to the parent.
        // Using setFitToHeight(true) should be the preferred way to do it but there seems to be a bug in calculating
        // the height and there is cut off about 20 px. We turn off vert. scrollbar as when toggling the show info button
        // in the child view we would get a flicker of the scrollbar.
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    }

    @Override
    protected void onViewAttached() {
        line.prefWidthProperty().unbind();
        line.prefWidthProperty().bind(root.widthProperty().subtract(81));

        UIThread.runOnNextRenderFrame(this::maybeAnimateMark);
    }

    @Override
    protected void onViewDetached() {
        line.prefWidthProperty().unbind();
    }

    @Override
    protected void setupTopBox() {
        tabs.setFillHeight(true);
        tabs.setSpacing(46);
        tabs.setMinHeight(35);

        topBox = new VBox();
        topBox.getChildren().addAll(tabs);
    }

    @SuppressWarnings("RedundantMethodOverride")
    @Override
    protected boolean useFitToHeight(View<? extends Parent, ? extends Model, ? extends Controller> childView) {
        return false;
    }

    @Override
    protected void onChildView(View<? extends Parent, ? extends Model, ? extends Controller> oldValue,
                               View<? extends Parent, ? extends Model, ? extends Controller> newValue) {
        super.onChildView(oldValue, newValue);

        scrollPane.prefViewportHeightProperty().unbind();
        scrollPane.prefViewportWidthProperty().unbind();
        if (newValue != null) {
            scrollPane.prefViewportHeightProperty().bind(newValue.getRoot().heightProperty());
            scrollPane.prefViewportWidthProperty().bind(newValue.getRoot().widthProperty());
        }
    }
}
