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

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.common.Styles;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.containers.Spacer;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;



@Slf4j
public abstract class TabView<M extends TabModel, C extends TabController<M>> extends NavigationView<VBox, M, C>
        implements TransitionedView {
    public static final double SIDE_PADDING = 40;
    public static final Insets DEFAULT_TOP_PANE_PADDING = new Insets(30, SIDE_PADDING, 0, SIDE_PADDING);

    protected Label headline;
    protected final HBox tabs = new HBox();
    protected Region selectionMarker, line;
    private final ToggleGroup toggleGroup = new ToggleGroup();
    @Getter
    protected final ScrollPane scrollPane;
    protected Pane lineAndMarker;
    protected double lineSidePadding = 0;
    @Getter
    protected Pane topBox;
    private Subscription selectedTabButtonSubscription, rootWidthSubscription, layoutDoneSubscription;
    private boolean transitionStarted;
    private ChangeListener<View<? extends Parent, ? extends Model, ? extends Controller>> viewListener;

    public TabView(M model, C controller) {
        super(new VBox(), model, controller);

        setupTopBox();
        setupLineAndMarker();

        scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setVvalue(0);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        root.getChildren().addAll(topBox, lineAndMarker, scrollPane);
    }

    @Override
    void onViewAttachedInternal() {
        selectionMarker.setLayoutX(0);
        selectionMarker.setPrefWidth(0);
        transitionStarted = false;

        line.prefWidthProperty().bind(root.widthProperty().subtract(2 * lineSidePadding));
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
                maybeAnimateMark();
            }
        });

        viewListener = (observable, oldValue, newValue) -> {
            onChildView(oldValue, newValue);
        };
        model.getView().addListener(viewListener);
        onChildView(null, model.getView().get());

        super.onViewAttachedInternal();
    }

    protected void onChildView(View<? extends Parent, ? extends Model, ? extends Controller> oldValue,
                               View<? extends Parent, ? extends Model, ? extends Controller> newValue) {
        if (oldValue != null) {
            Transitions.slideOutRight(oldValue.getRoot(), () -> setNewContent(newValue));
        } else {
            setNewContent(newValue);
        }
    }

    private void setNewContent(View<? extends Parent, ? extends Model, ? extends Controller> newValue) {
        if (newValue != null) {
            newValue.getRoot().setOpacity(0);

            scrollPane.setFitToHeight(useFitToHeight(newValue));
            scrollPane.setContent(newValue.getRoot());
            scrollPane.setVvalue(0);

            Transitions.fadeIn(newValue.getRoot());
        } else {
            scrollPane.setContent(null);
        }
    }

    protected boolean useFitToHeight(View<? extends Parent, ? extends Model, ? extends Controller> childView) {
        return false;
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
        model.getView().removeListener(viewListener);
        super.onViewDetachedInternal();
    }

    @Override
    public void onStartTransition() {
        transitionStarted = true;
        UIThread.runOnNextRenderFrame(this::maybeAnimateMark);
    }

    @Override
    public void onTransitionCompleted() {
    }

    protected boolean isRightSide() {
        return false;
    }

    protected void setupTopBox() {
        setupTopBox(isRightSide());
    }

    protected void setupTopBox(boolean isRightSide) {
        headline = new Label();
        headline.getStyleClass().add("tab-view");

        tabs.setFillHeight(true);
        tabs.setSpacing(46);

        if (isRightSide) {
            topBox = new HBox(headline, Spacer.fillHBox(), tabs);
        } else {
            topBox = new HBox(tabs, Spacer.fillHBox(), headline);
        }
        topBox.setPadding(DEFAULT_TOP_PANE_PADDING);
    }

    protected void setupLineAndMarker() {
        line = new Region();
        line.getStyleClass().add("tab-view-line");
        double lineHeight = 1;
        line.setMinHeight(lineHeight);

        selectionMarker = new Region();
        selectionMarker.getStyleClass().add("tab-view-selection");
        selectionMarker.setMinHeight(lineHeight);

        lineAndMarker = new Pane();
        lineAndMarker.getChildren().addAll(line, selectionMarker);
        lineAndMarker.setMinHeight(lineHeight);
        lineAndMarker.setMaxHeight(lineHeight);
        VBox.setMargin(lineAndMarker, new Insets(0, lineSidePadding, 0, lineSidePadding));
    }

    protected TabButton addTab(String text, NavigationTarget navigationTarget) {
        return addTab(text,
                navigationTarget,
                new Styles("bisq-text-grey-9", "bisq-text-white", "bisq-text-green", "bisq-text-grey-9"),
                null);
    }

    protected TabButton addTab(String text, NavigationTarget navigationTarget, String icon) {
        return addTab(text,
                navigationTarget,
                new Styles("bisq-text-grey-9", "bisq-text-white", "bisq-text-green", "bisq-text-grey-9"),
                icon);
    }

    protected TabButton addTab(String text, NavigationTarget navigationTarget, Styles styles) {
        return addTab(text, navigationTarget, styles, null);
    }

    protected TabButton addTab(String text, NavigationTarget navigationTarget, Styles styles, @Nullable String icon) {
        TabButton tabButton = new TabButton(text, toggleGroup, navigationTarget, styles, icon);
        controller.onTabButtonCreated(tabButton);
        tabs.getChildren().add(tabButton);
        return tabButton;
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
                            selectedTabButton.getWidth() - TabButton.BADGE_PADDING,
                            getSelectionMarkerX(selectedTabButton)))
                    .after(150);

            UIThread.runOnNextRenderFrame(() -> {
                if (layoutDoneSubscription != null) {
                    layoutDoneSubscription.unsubscribe();
                    layoutDoneSubscription = null;
                }
            });
        });
    }

    protected double getSelectionMarkerX(TabButton selectedTabButton) {
        return selectedTabButton.getBoundsInParent().getMinX() + tabs.getBoundsInParent().getMinX();
    }
}
