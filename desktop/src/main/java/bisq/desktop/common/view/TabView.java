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
import bisq.desktop.common.threading.UIThread;
import javafx.beans.value.ChangeListener;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class TabView<R extends TabPane, M extends NavigationModel, C extends TabController> extends View<R, M, C> {
    protected final ChangeListener<Tab> tabChangeListener;
    protected final ChangeListener<View<? extends Parent, ? extends Model, ? extends Controller>> viewChangeListener;

    public TabView(R root, M model, C controller) {
        super(root, model, controller);

        tabChangeListener = (observable, oldValue, newValue) -> {
            if (newValue instanceof NavigationTargetTab navigationTargetTab) {
                controller.onTabSelected(navigationTargetTab.getNavigationTarget());
            }
        };

        viewChangeListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                NavigationTargetTab tab = getTabFromTarget(model.getNavigationTarget());
                tab.setContent(newValue.getRoot());

                // Remove tabChangeListener temporarily to avoid that the tabChangeListener gets called from the selection call
                root.getSelectionModel().selectedItemProperty().removeListener(tabChangeListener);
                root.getSelectionModel().select(tab);
                root.getSelectionModel().selectedItemProperty().addListener(tabChangeListener);
            }
        };
    }

    @Override
    protected void onViewAttached() {
        if (root.getTabs().isEmpty()) {
            createAndAddTabs();
        }
        model.getView().addListener(viewChangeListener);
        root.getSelectionModel().selectedItemProperty().addListener(tabChangeListener);
        // We need to delay a bit to give the child view chance to register the collection
        UIThread.runLater(() -> controller.onTabSelected(model.getNavigationTarget()));
    }

    @Override
    protected void onViewDetached() {
        model.getView().removeListener(viewChangeListener);
        root.getSelectionModel().selectedItemProperty().removeListener(tabChangeListener);
    }

    protected NavigationTargetTab createTab(String title, NavigationTarget navigationTarget) {
        NavigationTargetTab tab = new NavigationTargetTab(title.toUpperCase(), navigationTarget);
        tab.setClosable(false);
        return tab;
    }

    protected NavigationTargetTab getTabFromTarget(NavigationTarget navigationTarget) {
        return root.getTabs().stream()
                .filter(NavigationTargetTab.class::isInstance)
                .map(NavigationTargetTab.class::cast)
                .filter(tab -> navigationTarget == tab.getNavigationTarget())
                .findAny()
                .orElseThrow();
    }

    protected abstract void createAndAddTabs();
}
