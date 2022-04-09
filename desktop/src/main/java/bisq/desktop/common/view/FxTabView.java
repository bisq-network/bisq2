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

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.Transitions;
import javafx.beans.value.ChangeListener;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Region;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class FxTabView<R extends TabPane, M extends FxTabModel, C extends FxTabController> extends NavigationView<R, M, C> {
    protected final ChangeListener<Tab> tabChangeListener;
    protected final ChangeListener<View<? extends Parent, ? extends Model, ? extends Controller>> viewChangeListener;
    private ChangeListener<Number> nodeInHeightListener;

    public FxTabView(R root, M model, C controller) {
        super(root, model, controller);

        tabChangeListener = (observable, oldValue, newValue) -> {
            if (newValue instanceof FxNavigationTargetTab navigationTargetTab) {
                controller.onTabSelected(navigationTargetTab.getNavigationTarget());
            }

            if (newValue.getContent() instanceof Region nodeIn) {
                nodeInHeightListener = (observable1, oldValue1, height) -> {
                    if (height.doubleValue() > 0) {
                        nodeIn.heightProperty().removeListener(nodeInHeightListener);
                        Transitions.transitInNewTab(nodeIn);
                    }
                };
                if (nodeIn.getHeight() > 0) {
                    Transitions.transitInNewTab(nodeIn);
                } else {
                    nodeIn.heightProperty().addListener(nodeInHeightListener);
                }
            } else {
                UIThread.runOnNextRenderFrame(() -> Transitions.fadeIn(newValue.getContent(), 100));
            }
        };

        viewChangeListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                FxNavigationTargetTab tab = getTabFromTarget(model.getNavigationTarget());
                tab.setContent(newValue.getRoot());
                // Remove tabChangeListener temporarily to avoid that the tabChangeListener gets called from the selection call
                root.getSelectionModel().selectedItemProperty().removeListener(tabChangeListener);
                root.getSelectionModel().select(tab);
                root.getSelectionModel().selectedItemProperty().addListener(tabChangeListener);
            }
        };
    }

    @Override
    void onViewAttachedInternal() {
        if (root.getTabs().isEmpty()) {
            createAndAddTabs();
        }

        //todo  hack for setting bg color, did not work via css
     /*   UIThread.runOnNextRenderFrame(() -> {
            Node node = root.lookup(".tab-header-background");
            if (node !=null){
                node.setStyle("-fx-background-color: -fx-base");
            }
        });*/

        model.getView().addListener(viewChangeListener);
        root.getSelectionModel().selectedItemProperty().addListener(tabChangeListener);
        // We need to delay a bit to give the child view chance to register the collection
        UIThread.runOnNextRenderFrame(() -> controller.onTabSelected(model.getNavigationTarget()));

        onViewAttached();
    }

    @Override
    void onViewDetachedInternal() {
        model.getView().removeListener(viewChangeListener);
        root.getSelectionModel().selectedItemProperty().removeListener(tabChangeListener);

        onViewDetached();
    }

    protected FxNavigationTargetTab createTab(String title, NavigationTarget navigationTarget) {
        FxNavigationTargetTab tab = new FxNavigationTargetTab(title, navigationTarget);
        tab.setClosable(false);
        return tab;
    }

    protected FxNavigationTargetTab getTabFromTarget(NavigationTarget navigationTarget) {
        return root.getTabs().stream()
                .filter(FxNavigationTargetTab.class::isInstance)
                .map(FxNavigationTargetTab.class::cast)
                .filter(tab -> navigationTarget == tab.getNavigationTarget())
                .findAny()
                .orElseThrow();
    }

    protected abstract void createAndAddTabs();
}
