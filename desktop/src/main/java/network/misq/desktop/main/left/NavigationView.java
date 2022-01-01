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

package network.misq.desktop.main.left;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import network.misq.desktop.NavigationTarget;
import network.misq.desktop.common.view.View;
import network.misq.desktop.components.controls.AutoTooltipToggleButton;
import network.misq.i18n.Res;

public class NavigationView extends View<VBox, NavigationModel, NavigationController> {
    private final ToggleGroup toggleGroup = new ToggleGroup();
    @Getter
    private final NetworkInfoBox networkInfoBox;

    public NavigationView(NavigationModel model, NavigationController controller) {
        super(new VBox(), model, controller);

        root.setMaxWidth(337);
        root.setMinWidth(337);
        root.setPadding(new Insets(0, 0, 0, 20));

        NavigationButton markets = new NavigationButton("Markets", toggleGroup, () -> controller.navigateTo(NavigationTarget.MARKETS));
        NavigationButton offerBook = new NavigationButton("Offerbook", toggleGroup, () -> controller.navigateTo(NavigationTarget.OFFERBOOK));
        NavigationButton createOffer = new NavigationButton("Create offer", toggleGroup, () -> controller.navigateTo(NavigationTarget.CREATE_OFFER));
        NavigationButton settings = new NavigationButton("Settings", toggleGroup, () -> controller.navigateTo(NavigationTarget.SETTINGS));
      /*   NavButton trades = new NavButton(TradesViewController.class, "Trades");
        NavButton funds = new NavButton(FundsViewController.class, "Funds");
        NavButton accounts = new NavButton(AccountsViewController.class, "Accounts");
        */

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        networkInfoBox = new NetworkInfoBox(model, () -> controller.navigateTo(NavigationTarget.NETWORK_INFO));
        root.getChildren().addAll(markets, offerBook, createOffer, settings, spacer, networkInfoBox);
    }

    private static class NavigationButton extends AutoTooltipToggleButton {
        private NavigationButton(String title,
                                 ToggleGroup toggleGroup,
                                 Runnable handler) {
            super(title);
            this.setToggleGroup(toggleGroup);
            this.getStyleClass().add("navigation-button");
            this.selectedProperty().addListener((ov, oldValue, newValue) -> setMouseTransparent(newValue));
            this.setOnAction(e -> handler.run());
        }
    }

    private static class NetworkInfoBox extends VBox {
        private NetworkInfoBox(NavigationModel model, Runnable handler) {
            setSpacing(5);
            setPadding(new Insets(10, 10, 10, 10));
            setOnMouseClicked(e -> handler.run());

            HBox clearNetBox = getTransportTypeBox("clear-net",
                    model.clearNetNumConnections,
                    model.clearNetNumTargetConnections,
                    model.clearNetIsVisible);

            HBox torBox = getTransportTypeBox("tor",
                    model.torNumConnections,
                    model.torNumTargetConnections,
                    model.torIsVisible);

            HBox i2pBox = getTransportTypeBox("i2p",
                    model.i2pNumConnections,
                    model.i2pNumTargetConnections,
                    model.i2pIsVisible);

            getChildren().addAll(clearNetBox, torBox, i2pBox);
        }

        private HBox getTransportTypeBox(String iconId,
                                         StringProperty numConnections,
                                         StringProperty numTargetConnections,
                                         BooleanProperty isVisible) {
            HBox hBox = new HBox();
            hBox.setSpacing(5);
            hBox.managedProperty().bind(isVisible);
            hBox.visibleProperty().bind(isVisible);

            Label peers = new Label(Res.network.get("peers"));

            Label numConnectionsLabel = new Label();
            numConnectionsLabel.textProperty().bind(numConnections);

            Label separator = new Label("|");

            Label numTargetConnectionsLabel = new Label();
            numTargetConnectionsLabel.textProperty().bind(numTargetConnections);

            ImageView icon = new ImageView();
            icon.setId(iconId);

            HBox.setMargin(icon, new Insets(0, 0, 5, 0));

            hBox.getChildren().addAll(peers, numConnectionsLabel, separator, numTargetConnectionsLabel, icon);
            return hBox;
        }
    }
}
