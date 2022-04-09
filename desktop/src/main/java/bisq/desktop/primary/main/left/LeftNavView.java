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

package bisq.desktop.primary.main.left;

import bisq.desktop.common.utils.Icons;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.View;
import bisq.desktop.layout.Layout;
import bisq.desktop.primary.main.top.TopPanelView;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class LeftNavView extends View<AnchorPane, LeftNavModel, LeftNavController> {
    private final static int EXPANDED_WIDTH = 330;
    private final static int COLLAPSED_WIDTH = 110;
    private final static int MARKER_WIDTH = 4;

    private final ToggleGroup toggleGroup = new ToggleGroup();
    @Getter
    private final NetworkInfoBox networkInfoBox;
    private final Map<NavigationTarget, NavigationButton> buttonsMap = new HashMap<>();
    private final Label expandIcon, collapseIcon;
    private final ImageView logoExpanded, logoCollapsed;
    private final Region selectionMarker;
    private final VBox vBox;
    private final int menuTop;
    private Subscription navigationTargetSubscription, menuExpandedSubscription;

    public LeftNavView(LeftNavModel model, LeftNavController controller) {
        super(new AnchorPane(), model, controller);

        root.setStyle("-fx-background-color: -bisq-dark-bg; /*-fx-background-insets: 0 30 0 0*/");

        menuTop = TopPanelView.HEIGHT;

        vBox = new VBox();
        Layout.pinToAnchorPane(vBox, menuTop, 0, 0, MARKER_WIDTH);

        NavigationButton trade = createNavigationButton(Res.get("trade"),
                ImageUtil.getImageViewById("sell"),  //todo missing icon
                NavigationTarget.TRADE);
        NavigationButton portfolio = createNavigationButton(Res.get("portfolio"),
                ImageUtil.getImageViewById("portfolio"),
                NavigationTarget.PORTFOLIO);
        NavigationButton education = createNavigationButton(Res.get("education"),
                ImageUtil.getImageViewById("support"), //todo missing icon
                NavigationTarget.EDUCATION);
        NavigationButton social = createNavigationButton(Res.get("social"),
                ImageUtil.getImageViewById("governance"),//todo missing icon
                NavigationTarget.SOCIAL);
        NavigationButton events = createNavigationButton(Res.get("events"),
                ImageUtil.getImageViewById("home"),//todo missing icon
                NavigationTarget.EVENTS);
        NavigationButton markets = createNavigationButton(Res.get("markets"),
                ImageUtil.getImageViewById("market"),
                NavigationTarget.MARKETS);
        NavigationButton wallet = createNavigationButton(Res.get("wallet"),
                ImageUtil.getImageViewById("wallet"),
                NavigationTarget.WALLET);
        NavigationButton settings = createNavigationButton(Res.get("settings"),
                ImageUtil.getImageViewById("settings"),
                NavigationTarget.SETTINGS);

         /*  social.setOnAction(() -> {
            controller.onNavigationTargetSelected(NavigationTarget.SOCIAL);
            if (model.getMenuExpanded().get()) {
                controller.onToggleExpandMenu();
            }
        });*/

        networkInfoBox = new NetworkInfoBox(model,
                () -> controller.onNavigationTargetSelected(NavigationTarget.NETWORK_INFO));
       // Layout.pinToAnchorPane(networkInfoBox, null, null, 26, 20);
        Layout.pinToAnchorPane(networkInfoBox, null, null, 0, 0);

        model.addNavigationTarget(NavigationTarget.NETWORK_INFO);

        expandIcon = Icons.getIcon(AwesomeIcon.CHEVRON_SIGN_RIGHT, "22");
        expandIcon.setCursor(Cursor.HAND);
        collapseIcon = Icons.getIcon(AwesomeIcon.CHEVRON_SIGN_LEFT, "22");
        collapseIcon.setCursor(Cursor.HAND);

        int iconSize = 28;
        collapseIcon.setLayoutY(menuTop - iconSize);
        collapseIcon.setLayoutX(MARKER_WIDTH + EXPANDED_WIDTH - iconSize);
        collapseIcon.setOpacity(0);
        expandIcon.setLayoutY(menuTop - iconSize);
        expandIcon.setLayoutX(MARKER_WIDTH + COLLAPSED_WIDTH - iconSize);
        expandIcon.setOpacity(0);

        logoExpanded = ImageUtil.getImageViewById("bisq-logo");
        logoCollapsed = ImageUtil.getImageViewById("bisq-logo-mark");
        VBox.setMargin(logoExpanded, new Insets(0, 0, 0, 11));
        VBox.setMargin(logoCollapsed, new Insets(0, 0, 0, 11));
        logoExpanded.setLayoutX(47);
        logoCollapsed.setLayoutX(logoExpanded.getLayoutX());
        logoExpanded.setLayoutY(26);
        logoCollapsed.setLayoutY(logoExpanded.getLayoutY());

        selectionMarker = new Region();
        selectionMarker.setStyle("-fx-background-color: -fx-accent;");
        selectionMarker.setPrefWidth(4.5);
        selectionMarker.setPrefHeight(NavigationButton.HEIGHT);

        selectionMarker.setLayoutY(menuTop);
        vBox.getChildren().addAll(trade, portfolio, education, social, events, markets, wallet, settings);
        vBox.setLayoutY(menuTop);
        root.getChildren().addAll(logoExpanded, logoCollapsed, selectionMarker, vBox, collapseIcon, expandIcon, networkInfoBox);
    }

    @Override
    protected void onViewAttached() {
        expandIcon.setOnMouseClicked(e -> controller.onToggleExpandMenu());
        collapseIcon.setOnMouseClicked(e -> controller.onToggleExpandMenu());
        navigationTargetSubscription = EasyBind.subscribe(model.getNavigationTarget(), navigationTarget -> {
            if (navigationTarget != null) {
                Optional.ofNullable(buttonsMap.get(navigationTarget)).ifPresent(toggleGroup::selectToggle);
            }
        });
        menuExpandedSubscription = EasyBind.subscribe(model.getMenuExpanded(), menuExpanded -> {
            int width = menuExpanded ? EXPANDED_WIDTH : COLLAPSED_WIDTH;
            vBox.setPrefWidth(width);
           // vBox.setMinWidth(width);

            root.setPrefWidth(width + MARKER_WIDTH);
          //  root.setMinWidth(width + MARKER_WIDTH);

            buttonsMap.values().forEach(e -> e.setMenuExpanded(menuExpanded, width));

            networkInfoBox.setPrefWidth(width+ MARKER_WIDTH);
           // networkInfoBox.setMinWidth(width+ MARKER_WIDTH);
            
            networkInfoBox.setVisible(menuExpanded);
            networkInfoBox.setManaged(menuExpanded);

            logoExpanded.setVisible(menuExpanded);
            logoExpanded.setManaged(menuExpanded);
            logoCollapsed.setVisible(!menuExpanded);
            logoCollapsed.setManaged(!menuExpanded);

            collapseIcon.setVisible(menuExpanded);
            collapseIcon.setManaged(menuExpanded);
            expandIcon.setVisible(!menuExpanded);
            expandIcon.setManaged(!menuExpanded);
        });

        root.setOnMouseEntered(e -> {
            Transitions.fadeIn(collapseIcon);
            Transitions.fadeIn(expandIcon);
        });
        root.setOnMouseExited(e -> {
            Transitions.fadeOut(expandIcon);
            Transitions.fadeOut(collapseIcon);
        });
    }

    @Override
    protected void onViewDetached() {
        expandIcon.setOnMouseClicked(null);
        collapseIcon.setOnMouseClicked(null);
        navigationTargetSubscription.unsubscribe();
        menuExpandedSubscription.unsubscribe();
        root.setOnMouseEntered(null);
        root.setOnMouseExited(null);
    }

    private NavigationButton createNavigationButton(String title, ImageView icon, NavigationTarget navigationTarget) {
        NavigationButton button = new NavigationButton(title, icon, toggleGroup);
        button.setOnAction(() -> {
            controller.onNavigationTargetSelected(navigationTarget);
            selectionMarker.setLayoutY(menuTop + button.getBoundsInParent().getMinY());
        });
        buttonsMap.put(navigationTarget, button);
        model.addNavigationTarget(navigationTarget);
        return button;
    }

    private static class NetworkInfoBox extends VBox {
        private NetworkInfoBox(LeftNavModel model, Runnable handler) {
            setSpacing(5);
           
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

            Region line = new Region();
            line.setMinHeight(1);
            line.setStyle("-fx-background-color: -bisq-controls-bg-disabled");
            setMinHeight(NavigationButton.HEIGHT);
            setMaxHeight(NavigationButton.HEIGHT);
            Insets insets = new Insets(18, 0, 0, 44);
            VBox.setMargin(clearNetBox, insets);
            VBox.setMargin(torBox, insets);
            VBox.setMargin(i2pBox, insets);
            getChildren().addAll(line,clearNetBox, torBox, i2pBox);
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
            String style = "-fx-text-fill: -bisq-text-medium; -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 1.2em";
            peers.setStyle(style);
           
            Label numConnectionsLabel = new Label();
            peers.setStyle(style);
            numConnectionsLabel.textProperty().bind(numConnections);

            Label separator = new Label("|");
            separator.setStyle(style);
            
            Label numTargetConnectionsLabel = new Label();
            numTargetConnectionsLabel.setStyle(style);
            numTargetConnectionsLabel.textProperty().bind(numTargetConnections);

            ImageView icon = new ImageView();
            icon.setId(iconId);

            HBox.setMargin(icon, iconMargin);

            hBox.getChildren().addAll(peers, numConnectionsLabel, separator, numTargetConnectionsLabel, icon);
            return hBox;
        }
    }
}
