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

import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
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

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class LeftNavView extends View<AnchorPane, LeftNavModel, LeftNavController> {
    private final static int EXPANDED_WIDTH = 330;
    private final static int COLLAPSED_WIDTH = 110;
    private final static int MARKER_WIDTH = 4;
    private final static int EXPAND_ICON_SIZE = 28;

    private final ToggleGroup toggleGroup = new ToggleGroup();
    @Getter
    private final NetworkInfoBox networkInfoBox;
    private final Label expandIcon, collapseIcon;
    private final ImageView logoExpanded, logoCollapsed;
    private final Region selectionMarker;
    private final VBox vBox;
    private final int menuTop;
    private Subscription navigationTargetSubscription, menuExpandedSubscription;

    public LeftNavView(LeftNavModel model, LeftNavController controller) {
        super(new AnchorPane(), model, controller);

        root.setStyle("-fx-background-color: -bisq-grey-1;");

        menuTop = TopPanelView.HEIGHT;

        vBox = new VBox();
        Layout.pinToAnchorPane(vBox, menuTop, 0, 0, MARKER_WIDTH);

        NavigationButton social = createNavigationButton(Res.get("social"),
                ImageUtil.getImageViewById("home"),//todo missing icon
                NavigationTarget.SOCIAL);
        NavigationButton trade = createNavigationButton(Res.get("trade"),
                ImageUtil.getImageViewById("sell"),  //todo missing icon
                NavigationTarget.TRADE);
        NavigationButton portfolio = createNavigationButton(Res.get("portfolio"),
                ImageUtil.getImageViewById("portfolio"),
                NavigationTarget.PORTFOLIO);
        NavigationButton markets = createNavigationButton(Res.get("markets"),
                ImageUtil.getImageViewById("market"),
                NavigationTarget.MARKETS);

        //todo lower prio menu add design
        NavigationButton wallet = createNavigationButton(Res.get("wallet"),
                ImageUtil.getImageViewById("wallet"),
                NavigationTarget.WALLET);
        NavigationButton support = createNavigationButton(Res.get("support"),
                ImageUtil.getImageViewById("support"),
                NavigationTarget.SUPPORT);
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
        Layout.pinToAnchorPane(networkInfoBox, null, null, 0, 0);

        // controller.onNavigationButtonCreated(NavigationTarget.NETWORK_INFO);

        expandIcon = Icons.getIcon(AwesomeIcon.CHEVRON_SIGN_RIGHT, "22");
        expandIcon.setCursor(Cursor.HAND);
        collapseIcon = Icons.getIcon(AwesomeIcon.CHEVRON_SIGN_LEFT, "22");
        collapseIcon.setCursor(Cursor.HAND);

        collapseIcon.setLayoutY(menuTop - EXPAND_ICON_SIZE);
        collapseIcon.setLayoutX(MARKER_WIDTH + EXPANDED_WIDTH - EXPAND_ICON_SIZE);
        collapseIcon.setOpacity(0);
        expandIcon.setLayoutY(menuTop - EXPAND_ICON_SIZE);
        expandIcon.setLayoutX(MARKER_WIDTH + COLLAPSED_WIDTH - EXPAND_ICON_SIZE);
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
        vBox.getChildren().addAll(social, trade, portfolio, markets, wallet, support, settings);
        vBox.setLayoutY(menuTop);
        root.getChildren().addAll(logoExpanded, logoCollapsed, selectionMarker, vBox, expandIcon, collapseIcon, networkInfoBox);
    }

    @Override
    protected void onViewAttached() {
        expandIcon.setOnMouseClicked(e -> controller.onToggleExpandMenu());
        collapseIcon.setOnMouseClicked(e -> controller.onToggleExpandMenu());
        navigationTargetSubscription = EasyBind.subscribe(model.getSelectedNavigationTarget(), navigationTarget -> {
            if (navigationTarget != null) {
                controller.findTabButton(navigationTarget).ifPresent(toggleGroup::selectToggle);
                maybeAnimateMark();
            }
        });
        expandIcon.setVisible(false);
        expandIcon.setManaged(false);

        menuExpandedSubscription = EasyBind.subscribe(model.getMenuExpanded(), menuExpanding -> {
            int width = menuExpanding ? EXPANDED_WIDTH : COLLAPSED_WIDTH;
            //   vBox.setPrefWidth(width);
            // root.setPrefWidth(width + MARKER_WIDTH);


            AtomicInteger duration = new AtomicInteger(400);
            if (menuExpanding) {
                UIScheduler.run(() -> model.getNavigationButtons()
                                .forEach(e -> e.setMenuExpanded(menuExpanding, width, duration.get() / 2)))
                        .after(duration.get() / 2);
                Transitions.animateLeftNavigationWidth(vBox, EXPANDED_WIDTH, duration.get());
                networkInfoBox.setPrefWidth(width + MARKER_WIDTH);
                Transitions.fadeIn(networkInfoBox, duration.get());

                Transitions.fadeOut(expandIcon, duration.get() / 4, () -> {
                    expandIcon.setVisible(false);
                    expandIcon.setManaged(false);
                });
                UIScheduler.run(() -> {
                    Transitions.fadeIn(logoExpanded, duration.get() / 4, () -> {
                        logoCollapsed.setVisible(false);
                        logoCollapsed.setManaged(false);
                    });
                    logoExpanded.setVisible(true);
                    logoExpanded.setManaged(true);
                }).after(duration.get() / 2);
                UIScheduler.run(() -> {
                    networkInfoBox.setOpacity(0);
                    Transitions.fadeIn(networkInfoBox, duration.get() / 4);
                    networkInfoBox.setVisible(true);
                    networkInfoBox.setManaged(true);
                    collapseIcon.setOpacity(0);
                    collapseIcon.setVisible(true);
                    collapseIcon.setManaged(true);
                }).after(duration.get() + 100);
            } else {
                expandIcon.setOpacity(0);
                expandIcon.setVisible(true);
                expandIcon.setManaged(true);
                model.getNavigationButtons().forEach(e -> e.setMenuExpanded(menuExpanding, width, duration.get() / 2));
                UIScheduler.run(() -> {
                            Transitions.animateLeftNavigationWidth(vBox, COLLAPSED_WIDTH, duration.get());
                            collapseIcon.setVisible(false);
                            collapseIcon.setManaged(false);
                            collapseIcon.setOpacity(0);
                        })
                        .after(duration.get() / 4);
                Transitions.fadeOut(networkInfoBox, duration.get() / 2, () -> {
                    networkInfoBox.setVisible(false);
                    networkInfoBox.setManaged(false);
                });
                Transitions.fadeOut(logoExpanded, duration.get() / 2, () -> {
                    logoExpanded.setVisible(false);
                    logoExpanded.setManaged(false);
                });
                logoCollapsed.setVisible(true);
                logoCollapsed.setManaged(true);
            }
        });

        root.setOnMouseEntered(e -> {
            if (collapseIcon.isVisible()) {
                Transitions.fadeIn(collapseIcon, Transitions.DEFAULT_DURATION, 0.1, null);
            }
            if (expandIcon.isVisible()) {
                Transitions.fadeIn(expandIcon, Transitions.DEFAULT_DURATION, 0.1, null);
            }
        });
        root.setOnMouseExited(e -> {
            if (collapseIcon.isVisible()) {
                Transitions.fadeOut(collapseIcon);
            }
            if (expandIcon.isVisible()) {
                Transitions.fadeOut(expandIcon);
            }
        });

        maybeAnimateMark();
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
        NavigationButton button = new NavigationButton(title, icon, toggleGroup, navigationTarget);
        button.setOnAction(() -> {
            controller.onNavigationTargetSelected(navigationTarget);
            maybeAnimateMark();
        });
        controller.onNavigationButtonCreated(button);
        return button;
    }

    private void maybeAnimateMark() {
        NavigationButton selectedNavigationButton = model.getSelectedNavigationButton().get();
        if (selectedNavigationButton == null) {
            return;
        }
        UIThread.runOnNextRenderFrame(() -> {
            Transitions.animateNavigationButtonMarks(selectionMarker, selectedNavigationButton.getHeight(),
                    menuTop + selectedNavigationButton.getBoundsInParent().getMinY());
        });
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
            line.setStyle("-fx-background-color: -bisq-grey-6");
            setMinHeight(NavigationButton.HEIGHT);
            setMaxHeight(NavigationButton.HEIGHT);
            Insets insets = new Insets(18, 0, 0, 44);
            VBox.setMargin(clearNetBox, insets);
            VBox.setMargin(torBox, insets);
            VBox.setMargin(i2pBox, insets);
            getChildren().addAll(line, clearNetBox, torBox, i2pBox);
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
            String style = "-fx-text-fill: -bisq-grey-9; -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 1.2em";
            peers.setStyle(style);

            Label numConnectionsLabel = new Label();
            numConnectionsLabel.setStyle("-fx-text-fill: -fx-light-text-color; -fx-font-family: \"IBM Plex Sans Light\"; -fx-font-size: 1.2em");
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
