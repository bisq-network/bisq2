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

package bisq.desktop.common.view;

import bisq.desktop.NavigationTarget;
import javafx.beans.value.ChangeListener;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public abstract class TabView<R extends TabPane, M extends TabModel, C extends TabController> extends View<R, M, C> {
    private final ChangeListener<Tab> tabChangeListener;
    private final ChangeListener<View<? extends Parent, ? extends Model, ? extends Controller>> viewChangeListener;

    public TabView(R root, M model, C controller) {
        super(root, model, controller);

        viewChangeListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                Tab tab = getTab(model.getNavigationTarget());
                tab.setContent(newValue.getRoot());
                root.getSelectionModel().select(tab);
            }
        };
        tabChangeListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                controller.onTabSelected(NavigationTarget.valueOf(newValue.getId()));
            }
        };
    }

    protected abstract void createAndAddTabs();

    @Override
    public void onViewAttached() {
        createAndAddTabs();
        
        model.getView().addListener(viewChangeListener);
        root.getSelectionModel().selectedItemProperty().addListener(tabChangeListener);
    }

    @Override
    protected void onViewDetached() {
        model.getView().removeListener(viewChangeListener);
        root.getSelectionModel().selectedItemProperty().removeListener(tabChangeListener);
    }

    protected Tab createTab(String title, NavigationTarget navigationTarget) {
        Tab tab = new Tab(title.toUpperCase());
        tab.setClosable(false);
        tab.setId(navigationTarget.name());
        return tab;
    }

    protected Tab getTab(NavigationTarget navigationTarget) {
        return root.getTabs().stream()
                .filter(tab -> tab.getId().equals(navigationTarget.name()))
                .findAny()
                .orElseThrow();
    }
}
