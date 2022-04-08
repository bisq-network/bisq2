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
public abstract class TabView<M extends TabModel, C extends TabController> extends NavigationView<VBox, M, C> {
    protected final Label label;
    protected final HBox tabs;
    private final Region selectionMarker, line;
    private final ToggleGroup toggleGroup = new ToggleGroup();
    protected final ChangeListener<View<? extends Parent, ? extends Model, ? extends Controller>> viewChangeListener;
    private Subscription selectedTabButtonSubscription;

    public TabView(M model, C controller) {
        super(new VBox(), model, controller);

        root.setFillWidth(true);

        label = new Label();
        label.setStyle("-fx-text-fill: -bisq-text;  -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 2.8em;");
        HBox.setMargin(label, new Insets(-8, 0, 31, -3));

        tabs = new HBox();
        tabs.setFillHeight(true);
        tabs.setSpacing(46);
        tabs.getChildren().addAll(label, Spacer.fillHBox());

        ScrollPane content = new ScrollPane();
        content.setFitToHeight(true);
        content.setFitToWidth(true);

        line = new Region();
        line.setStyle("-fx-background-color: -bisq-dark-bg;");
        double lineHeight = 1.5;
        line.setMinHeight(lineHeight);

        selectionMarker = new Region();
        selectionMarker.setStyle("-fx-background-color: -fx-accent;");
        selectionMarker.setPrefHeight(lineHeight);

        Pane lineAndMarker = new Pane();
        lineAndMarker.getChildren().addAll(line, selectionMarker);

        root.getChildren().addAll(tabs, lineAndMarker, content);

        viewChangeListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                content.setContent(newValue.getRoot());
            }
        };
    }

    protected void addTab(String text, NavigationTarget navigationTarget) {
        TabButton tabButton = new TabButton(text, toggleGroup, navigationTarget);
        model.getTabButtons().add(tabButton);
        tabs.getChildren().addAll(tabButton);
    }

    @Override
    void onViewAttachedInternal() {
        UIThread.runOnNextRenderFrame(() -> {
            NavigationTarget navigationTarget = model.getNavigationTarget();
            if (navigationTarget != null) {
                Navigation.navigateTo(navigationTarget);
            }
        });

        line.prefWidthProperty().bind(root.widthProperty());
        model.getTabButtons().forEach(tabButton ->
                tabButton.setOnAction(() -> controller.onTabSelected(tabButton.getNavigationTarget())));
        selectedTabButtonSubscription = EasyBind.subscribe(model.getSelectedTabButton(), tabButton -> {
            if (tabButton != null) {
               // tabButton.setSelected(true);
                toggleGroup.selectToggle(tabButton);
                selectionMarker.setLayoutX(tabButton.getBoundsInParent().getMinX());
                selectionMarker.setPrefWidth(tabButton.getWidth());
            }
        });
        model.getView().addListener(viewChangeListener);

        UIThread.runOnNextRenderFrame(() -> controller.onTabSelected(model.getNavigationTarget()));

        onViewAttached();
    }


    @Override
    void onViewDetachedInternal() {
        super.onViewDetachedInternal();

        line.prefWidthProperty().unbind();
        selectedTabButtonSubscription.unsubscribe();
        model.getTabButtons().forEach(tabButton -> tabButton.setOnAction(null));
        model.getView().removeListener(viewChangeListener);
    }
}
