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

package bisq.desktop.main.content.user.bonded_roles.tabs;

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.TabView;
import bisq.desktop.common.view.View;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BondedRolesTabView<M extends BondedRolesTabModel, C extends BondedRolesTabController<M>> extends TabView<M, C> {
    public BondedRolesTabView(M model, C controller) {
        super(model, controller);

        root.setPadding(new Insets(20, 30, 20, 30));
        root.getStyleClass().add("user-bonded-roles-tab-view");

        addTabs();

        // We apply the height of the viewpoint according the content height as we delegate the scrolling to the parent.
        // Using setFitToHeight(true) should be the preferred way to do it but there seems to be a bug in calculating
        // the height and there is cut off about 20 px. We turn off vert. scrollbar as when toggling the show info button
        // in the child view we would get a flicker of the scrollbar.
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    }

    protected abstract void addTabs();

    @Override
    protected void onViewAttached() {
        line.prefWidthProperty().unbind();
        line.prefWidthProperty().bind(root.widthProperty().subtract(61));

        UIThread.runOnNextRenderFrame(this::maybeAnimateMark);
    }

    @Override
    protected void onViewDetached() {
        line.prefWidthProperty().unbind();
    }

    @Override
    protected void setupTopBox() {
        headline = new Label(getHeadline());
        headline.getStyleClass().add("bisq-text-headline-5");
        VBox.setMargin(headline, new Insets(0, 0, 17, -2));

        tabs.setFillHeight(true);
        tabs.setSpacing(46);
        tabs.setMinHeight(35);

        topBox = new VBox();
        topBox.getChildren().addAll(headline, tabs);
    }

    protected abstract String getHeadline();

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
