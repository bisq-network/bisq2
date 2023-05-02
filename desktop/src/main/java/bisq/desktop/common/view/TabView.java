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

import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.Styles;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.components.containers.Spacer;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;

@Slf4j
public abstract class TabView<M extends TabModel, C extends TabController<M>> extends NavigationView<VBox, M, C>
        implements TransitionedView {
    protected Label headLine;
    protected final HBox tabs = new HBox();
    protected Region selectionMarker, line;
    private final ToggleGroup toggleGroup = new ToggleGroup();
    protected final VBox contentPane;
    protected Pane lineAndMarker;
    protected Pane topBox;
    private Subscription selectedTabButtonSubscription, rootWidthSubscription, layoutDoneSubscription;
    private boolean transitionStarted;
    //  private Subscription viewSubscription;
    private ChangeListener<View<? extends Parent, ? extends Model, ? extends Controller>> viewListener;

    public TabView(M model, C controller) {
        super(new VBox(), model, controller);

        setupTopBox();
        contentPane = new VBox();
        setupLineAndMarker();
        root.getChildren().addAll(topBox, lineAndMarker, contentPane);
    }

    @Override
    void onViewAttachedInternal() {
        selectionMarker.setLayoutX(0);
        selectionMarker.setPrefWidth(0);
        transitionStarted = false;

        line.prefWidthProperty().bind(root.widthProperty());
        model.getTabButtons().forEach(tabButton ->
                tabButton.setOnAction(() -> controller.onTabSelected(tabButton.getNavigationTarget())));

        rootWidthSubscription = EasyBind.subscribe(root.widthProperty(), w -> {
            if (model.getSelectedTabButton().get() != null) {
                // Need delay to give time for rendering in case of scrollbars
                UIThread.runOnNextRenderFrame(() -> selectionMarker.setLayoutX(getSelectionMarkerX(model.getSelectedTabButton().get())));
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

        viewListener = (observable, oldValue, newValue) -> onChildView(oldValue, newValue);
        model.getView().addListener(viewListener);
        onChildView(null, model.getView().get());

        super.onViewAttachedInternal();
    }

    protected void onChildView(View<? extends Parent, ? extends Model, ? extends Controller> oldValue,
                               View<? extends Parent, ? extends Model, ? extends Controller> newValue) {
        if (newValue != null) {
            contentPane.getChildren().setAll(newValue.getRoot());
        } else {
            contentPane.getChildren().clear();
        }
    }

    @Override
    void onViewDetachedInternal() {
        line.prefWidthProperty().unbind();
        selectedTabButtonSubscription.unsubscribe();
        rootWidthSubscription.unsubscribe();
        if (layoutDoneSubscription != null) {
            layoutDoneSubscription.unsubscribe();
        }
        model.getTabButtons().forEach(tabButton -> tabButton.setOnAction(null));
        // viewSubscription.unsubscribe();
        model.getView().removeListener(viewListener);
        super.onViewDetachedInternal();
    }

    @Override
    public void onStartTransition() {
        this.transitionStarted = true;
        UIThread.runOnNextRenderFrame(this::maybeAnimateMark);
    }

    @Override
    public void onTransitionCompleted() {
    }

    protected void setupTopBox() {
        headLine = new Label();
        headLine.getStyleClass().add("bisq-content-headline-label");

        tabs.setFillHeight(true);
        tabs.setSpacing(46);
        tabs.setMinHeight(52);

        HBox.setMargin(headLine, new Insets(-5, 0, 0, -2));
        topBox = new HBox(headLine, Spacer.fillHBox(), tabs);
    }

    protected void setupLineAndMarker() {
        line = new Region();
        line.getStyleClass().add("bisq-dark-bg");
        double lineHeight = 1;
        line.setMinHeight(lineHeight);

        selectionMarker = new Region();
        selectionMarker.getStyleClass().add("bisq-green-line");
        selectionMarker.setMinHeight(lineHeight);

        lineAndMarker = new Pane();
        lineAndMarker.getChildren().addAll(line, selectionMarker);
        lineAndMarker.setMinHeight(lineHeight);
        lineAndMarker.setMaxHeight(lineHeight);
        lineAndMarker.setPadding(new Insets(0, 67, 0, 0));
    }

    protected void addTab(String text, NavigationTarget navigationTarget) {
        addTab(text,
                navigationTarget,
                new Styles("bisq-text-grey-9", "bisq-text-white", "bisq-text-logo-green", "bisq-text-grey-9"),
                null);
    }

    protected void addTab(String text, NavigationTarget navigationTarget, String icon) {
        addTab(text,
                navigationTarget,
                new Styles("bisq-text-grey-9", "bisq-text-white", "bisq-text-logo-green", "bisq-text-grey-9"),
                icon);
    }

    protected void addTab(String text, NavigationTarget navigationTarget, Styles styles) {
        addTab(text, navigationTarget, styles, null);
    }

    protected void addTab(String text, NavigationTarget navigationTarget, Styles styles, @Nullable String icon) {
        TabButton tabButton = new TabButton(text, toggleGroup, navigationTarget, styles, icon);
        controller.onTabButtonCreated(tabButton);
        tabs.getChildren().add(tabButton);
    }

    //todo 
    protected void removeTab(NavigationTarget navigationTarget) {
        controller.findTabButton(navigationTarget).ifPresent(tabButton -> {
            controller.onTabButtonRemoved(tabButton);
            tabs.getChildren().remove(tabButton);
        });
    }

    protected void maybeAnimateMark() {
        TabButton selectedTabButton = model.getSelectedTabButton().get();
        if (selectedTabButton == null) {
            return;
        }
        if (layoutDoneSubscription != null) {
            layoutDoneSubscription.unsubscribe();
        }

        layoutDoneSubscription = EasyBind.subscribe(model.getSelectedTabButton().get().layoutXProperty(), x -> {
            // Need delay to give time for rendering in case of scrollbars
            UIScheduler.run(() -> Transitions.animateTabButtonMarks(selectionMarker,
                            selectedTabButton.getWidth(),
                            getSelectionMarkerX(selectedTabButton)))
                    .after(100);

            UIThread.runOnNextRenderFrame(() -> {
                if (layoutDoneSubscription != null) {
                    layoutDoneSubscription.unsubscribe();
                    layoutDoneSubscription = null;
                }
            });
        });
    }

    private double getSelectionMarkerX(TabButton selectedTabButton) {
        return selectedTabButton.getBoundsInParent().getMinX() + tabs.getBoundsInParent().getMinX();
    }
}
