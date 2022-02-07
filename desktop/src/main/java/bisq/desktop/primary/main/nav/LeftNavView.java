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

package bisq.desktop.primary.main.nav;

import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import com.jfoenix.controls.JFXButton;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LeftNavView extends View<VBox, LeftNavModel, LeftNavController> {
    private final ToggleGroup toggleGroup = new ToggleGroup();
    @Getter
    private final NetworkInfoBox networkInfoBox;
    private final ChangeListener<NavigationTarget> navigationTargetChangeListener;
    private final Map<NavigationTarget, NavigationButton> map = new HashMap<>();

    public LeftNavView(LeftNavModel model, LeftNavController controller) {
        super(new VBox(), model, controller);

        root.setMaxWidth(240);
        root.setMinWidth(240);
        root.setSpacing(5);
        root.setPadding(new Insets(0, 20, 20, 20));

        NavigationButton trade = createNavigationButton(Res.get("trade"), NavigationTarget.TRADE);
        NavigationButton portfolio = createNavigationButton(Res.get("portfolio"), NavigationTarget.PORTFOLIO);
        NavigationButton social = createNavigationButton(Res.get("social"), NavigationTarget.SOCIAL);
        NavigationButton markets = createNavigationButton(Res.get("markets"), NavigationTarget.MARKETS);
        NavigationButton wallet = createNavigationButton(Res.get("wallet"), NavigationTarget.WALLET);
        NavigationButton settings = createNavigationButton(Res.get("settings"), NavigationTarget.SETTINGS);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        networkInfoBox = new NetworkInfoBox(model, () -> controller.select(NavigationTarget.NETWORK_INFO));
        model.addNavigationTarget(NavigationTarget.NETWORK_INFO);
        root.getChildren().addAll(trade, portfolio, social, markets, wallet, settings, spacer, networkInfoBox);

        navigationTargetChangeListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                Optional.ofNullable(map.get(newValue)).ifPresent(toggleGroup::selectToggle);
            }
        };
    }

    @Override
    public void onViewAttached() {
        model.getNavigationTarget().addListener(navigationTargetChangeListener);
        Optional.ofNullable(map.get(model.getNavigationTarget().get())).ifPresent(toggleGroup::selectToggle);
    }

    private NavigationButton createNavigationButton(String title, NavigationTarget navigationTarget) {
        NavigationButton button = new NavigationButton(title, toggleGroup, () -> controller.select(navigationTarget));
        map.put(navigationTarget, button);
        model.addNavigationTarget(navigationTarget);
        return button;
    }

    private static class NavigationButton extends JFXButton implements Toggle {
        private final ObjectProperty<ToggleGroup> toggleGroupProperty = new SimpleObjectProperty<>();
        private final BooleanProperty selectedProperty = new SimpleBooleanProperty();

        private NavigationButton(String title, ToggleGroup toggleGroup, Runnable handler) {
            super(title.toUpperCase());

            setPrefHeight(40);
            setPrefWidth(240);
            setAlignment(Pos.CENTER_LEFT);

            this.setToggleGroup(toggleGroup);
            toggleGroup.getToggles().add(this);

            this.selectedProperty().addListener((ov, oldValue, newValue) -> setMouseTransparent(newValue));
            this.setOnAction(e -> handler.run());
        }

        ///////////////////////////////////////////////////////////////////////////////////////////
        // Toggle implementation
        ///////////////////////////////////////////////////////////////////////////////////////////

        @Override
        public ToggleGroup getToggleGroup() {
            return toggleGroupProperty.get();
        }

        @Override
        public void setToggleGroup(ToggleGroup toggleGroup) {
            toggleGroupProperty.set(toggleGroup);
        }

        @Override
        public ObjectProperty<ToggleGroup> toggleGroupProperty() {
            return toggleGroupProperty;
        }

        @Override
        public boolean isSelected() {
            return selectedProperty.get();
        }

        @Override
        public BooleanProperty selectedProperty() {
            return selectedProperty;
        }

        @Override
        public void setSelected(boolean selected) {
            selectedProperty.set(selected);
            if (selected) {
                getStyleClass().add("action-button");
            } else {
                getStyleClass().remove("action-button");
            }
        }
    }

    private static class NetworkInfoBox extends VBox {
        private NetworkInfoBox(LeftNavModel model, Runnable handler) {
            setSpacing(5);
            setPadding(new Insets(10, 10, 10, 10));
            setOnMouseClicked(e -> handler.run());

            HBox clearNetBox = getTransportTypeBox("clear-net",
                    model.getClearNetNumConnections(),
                    model.getClearNetNumTargetConnections(),
                    model.getClearNetIsVisible(),
                    new Insets(1, 0, 5, 3));

            HBox torBox = getTransportTypeBox("tor",
                    model.getTorNumConnections(),
                    model.getTorNumTargetConnections(),
                    model.getTorIsVisible(),
                    new Insets(-6, 0, 5, 1));

            HBox i2pBox = getTransportTypeBox("i2p",
                    model.getI2pNumConnections(),
                    model.getI2pNumTargetConnections(),
                    model.getI2pIsVisible(),
                    new Insets(0, 0, 5, 0));

            getChildren().addAll(clearNetBox, torBox, i2pBox);
        }

        private HBox getTransportTypeBox(String iconId,
                                         StringProperty numConnections,
                                         StringProperty numTargetConnections,
                                         BooleanProperty isVisible,
                                         Insets iconMargin) {
            HBox hBox = new HBox();
            hBox.setSpacing(5);
            hBox.setMinHeight(20);
            hBox.setMaxHeight(hBox.getMinHeight());
            hBox.managedProperty().bind(isVisible);
            hBox.visibleProperty().bind(isVisible);

            Label peers = new Label(Res.get("peers"));

            Label numConnectionsLabel = new Label();
            numConnectionsLabel.textProperty().bind(numConnections);

            Label separator = new Label("|");

            Label numTargetConnectionsLabel = new Label();
            numTargetConnectionsLabel.textProperty().bind(numTargetConnections);

            ImageView icon = new ImageView();
            icon.setId(iconId);

            HBox.setMargin(icon, iconMargin);

            hBox.getChildren().addAll(peers, numConnectionsLabel, separator, numTargetConnectionsLabel, icon);
            return hBox;
        }
    }
}
