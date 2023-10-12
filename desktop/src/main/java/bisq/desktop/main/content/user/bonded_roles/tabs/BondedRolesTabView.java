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

import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.TabView;
import bisq.desktop.common.view.View;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BondedRolesTabView<M extends BondedRolesTabModel, C extends BondedRolesTabController<M>> extends TabView<M, C> {
    public BondedRolesTabView(M model, C controller) {
        super(model, controller);

        root.setPadding(new Insets(30));
        root.getStyleClass().add("user-bonded-roles-tab-view");

        topBox.setPadding(new Insets(0, 0, 0, 0));
        lineAndMarker.setPadding(new Insets(0, 0, 0, 0));

        addTabs();
    }

    protected abstract void addTabs();

    @Override
    protected void onViewAttached() {
        line.prefWidthProperty().unbind();
        line.prefWidthProperty().bind(root.widthProperty().subtract(61));

        onStartTransition();
    }

    @Override
    protected void onViewDetached() {
        line.prefWidthProperty().unbind();
    }

    @Override
    protected void setupTopBox() {
        headLine = new Label(getHeadline());
        headLine.getStyleClass().add("bisq-text-headline-5");

        tabs.setFillHeight(true);
        tabs.setSpacing(46);
        tabs.setMinHeight(35);

        topBox = new VBox();
        VBox.setMargin(headLine, new Insets(-10, 0, 17, -2));
        topBox.getChildren().addAll(headLine, tabs);
    }

    protected abstract String getHeadline();

    @Override
    protected void setupLineAndMarker() {
        super.setupLineAndMarker();

        line.getStyleClass().remove("bisq-dark-bg");
        line.getStyleClass().add("bisq-mid-grey");
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
