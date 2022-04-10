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
import bisq.desktop.components.containers.Spacer;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public abstract class TabView<M extends TabModel, C extends TabController<M>> extends NavigationView<VBox, M, C>
        implements TransitionedView {
    protected final Label label;
    protected final HBox tabs;
    private final Region selectionMarker, line;
    private final ToggleGroup toggleGroup = new ToggleGroup();
    protected final ChangeListener<View<? extends Parent, ? extends Model, ? extends Controller>> viewChangeListener;
    private Subscription selectedTabButtonSubscription, rootWidthSubscription, layoutDoneSubscription;
    private boolean transitionStarted;

    public TabView(M model, C controller) {
        super(new VBox(), model, controller);

        root.setFillWidth(true);
        root.setPadding(new Insets(0, -100, 0, 0));

        label = new Label();
        label.setStyle("-fx-text-fill: -fx-light-text-color;  -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 2.8em;");
        HBox.setMargin(label, new Insets(-8, 0, 31, -3));

        tabs = new HBox();
        tabs.setFillHeight(true);
        tabs.setSpacing(46);
        tabs.getChildren().addAll(label, Spacer.fillHBox());
        tabs.setPadding(new Insets(0, 100, 0, 0));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);


        line = new Region();
        line.setStyle("-fx-background-color: -bisq-grey-1;");
        double lineHeight = 1.5;
        line.setMinHeight(lineHeight);

        selectionMarker = new Region();
        selectionMarker.setStyle("-fx-background-color: -fx-accent;");
        selectionMarker.setMinHeight(lineHeight);

        Pane lineAndMarker = new Pane();
        lineAndMarker.getChildren().addAll(line, selectionMarker);
        lineAndMarker.setMinHeight(lineHeight);
        lineAndMarker.setPadding(new Insets(0, 100, 0, 0));

        root.getChildren().addAll(tabs, lineAndMarker, scrollPane);

        viewChangeListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                Region childRoot = newValue.getRoot();
                childRoot.setStyle("-fx-background-color: -fx-base;");
                childRoot.setPadding(new Insets(50, 100, 0, 0));
                scrollPane.setContent(childRoot);
            }
        };
    }

    @Override
    void onViewAttachedInternal() {
        selectionMarker.setLayoutX(0);
        selectionMarker.setPrefWidth(0);
        transitionStarted = false;
        UIThread.runOnNextRenderFrame(() -> {
            NavigationTarget navigationTarget = model.getNavigationTarget();
            if (navigationTarget != null) {
                Navigation.navigateTo(navigationTarget);
            }
        });

        line.prefWidthProperty().bind(root.widthProperty());
        model.getTabButtons().forEach(tabButton ->
                tabButton.setOnAction(() -> controller.onTabSelected(tabButton.getNavigationTarget())));

        rootWidthSubscription = EasyBind.subscribe(root.widthProperty(), w -> {
            if (model.getSelectedTabButton().get() != null) {
                selectionMarker.setLayoutX(model.getSelectedTabButton().get().getLayoutX());
            }
        });

        selectedTabButtonSubscription = EasyBind.subscribe(model.getSelectedTabButton(), selectedTabButton -> {
            if (selectedTabButton != null) {
                toggleGroup.selectToggle(selectedTabButton);

                if (transitionStarted) {
                    maybeAnimateMark();
                }
            }
        });

        model.getView().addListener(viewChangeListener);

        UIThread.runOnNextRenderFrame(() -> {
            controller.onTabSelected(model.getNavigationTarget());
        });
        onViewAttached();
    }

    @Override
    public void onStartTransition() {
        this.transitionStarted = true;
        UIThread.runOnNextRenderFrame(this::maybeAnimateMark);
    }

    @Override
    public void onTransitionCompleted() {
    }

    @Override
    void onViewDetachedInternal() {
        super.onViewDetachedInternal();

        line.prefWidthProperty().unbind();
        selectedTabButtonSubscription.unsubscribe();
        rootWidthSubscription.unsubscribe();
        if (layoutDoneSubscription != null) {
            layoutDoneSubscription.unsubscribe();
        }
        model.getTabButtons().forEach(tabButton -> tabButton.setOnAction(null));
        model.getView().removeListener(viewChangeListener);
    }

    protected void addTab(String text, NavigationTarget navigationTarget) {
        TabButton tabButton = new TabButton(text, toggleGroup, navigationTarget);
        controller.onTabButtonCreated(tabButton);
        tabs.getChildren().addAll(tabButton);
    }

    //todo 
    protected void removeTab(NavigationTarget navigationTarget) {
        controller.findTabButton(navigationTarget).ifPresent(tabButton -> {
            controller.onTabButtonRemoved(tabButton);
            tabs.getChildren().remove(tabButton);
        });

    }

    private void maybeAnimateMark() {
        TabButton selectedTabButton = model.getSelectedTabButton().get();
        if (selectedTabButton == null) {
            return;
        }
        if (layoutDoneSubscription != null) {
            layoutDoneSubscription.unsubscribe();
        }

        //todo maybe listen to transition animation from outer container to start animation after view is visible
        layoutDoneSubscription = EasyBind.subscribe(model.getSelectedTabButton().get().layoutXProperty(), x -> {
            // At the time when x is available the width is available as well
            if (x.doubleValue() > 0) {
                Transitions.animateTabButtonMarks(selectionMarker, selectedTabButton.getWidth(),
                        selectedTabButton.getBoundsInParent().getMinX());
                UIThread.runOnNextRenderFrame(() -> {
                    if (layoutDoneSubscription != null) {
                        layoutDoneSubscription.unsubscribe();
                        layoutDoneSubscription = null;
                    }
                });
            }
        });
    }

}
