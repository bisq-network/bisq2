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
    private final static int EXPANDED_WIDTH = 220;
    private final static int COLLAPSED_WIDTH = 70;
    private final static int MARKER_WIDTH = 3;
    private final static int EXPAND_ICON_SIZE = 18;

    private final ToggleGroup toggleGroup = new ToggleGroup();
    @Getter
    private final NetworkInfoBox networkInfoBox;
    private final Label expandIcon, collapseIcon;
    private final ImageView logoExpanded, logoCollapsed;
    private final Region selectionMarker;
    private final VBox mainMenuItems, tradeSubMenuItems;
    private final int menuTop;
    private Subscription navigationTargetSubscription, menuExpandedSubscription;

    public LeftNavView(LeftNavModel model, LeftNavController controller) {
        super(new AnchorPane(), model, controller);

        root.getStyleClass().add("bisq-darkest-bg");

        menuTop = TopPanelView.HEIGHT;

        mainMenuItems = new VBox();
        mainMenuItems.setSpacing(6);
        Layout.pinToAnchorPane(mainMenuItems, menuTop, 0, 0, MARKER_WIDTH);

        tradeSubMenuItems = new VBox();
        tradeSubMenuItems.setSpacing(6);

        LeftNavButton social = createNavigationButton(Res.get("social"),
                ImageUtil.getImageViewById("home"),
                NavigationTarget.SOCIAL);
        LeftNavButton trade = createNavigationButton(Res.get("trade"),
                ImageUtil.getImageViewById("sell"),
                NavigationTarget.TRADE);

        LeftNavButton markets = createNavigationButton(Res.get("markets"),
                ImageUtil.getImageViewById("market"),
                NavigationTarget.MARKETS);

        //todo lower prio menu add design
        LeftNavButton wallet = createNavigationButton(Res.get("wallet"),
                ImageUtil.getImageViewById("wallet"),
                NavigationTarget.WALLET);
        LeftNavButton support = createNavigationButton(Res.get("support"),
                ImageUtil.getImageViewById("support"),
                NavigationTarget.SUPPORT);
        LeftNavButton settings = createNavigationButton(Res.get("settings"),
                ImageUtil.getImageViewById("settings"),
                NavigationTarget.SETTINGS);

        LeftNavSubButton satoshiSquare = createSecondaryNavigationButton(Res.get("satoshiSquare"),
                NavigationTarget.SATOSHI_SQUARE);
        LeftNavSubButton liquidSwap = createSecondaryNavigationButton(Res.get("liquidSwap"),
                NavigationTarget.LIQUID_SWAP);
        LeftNavSubButton multiSig = createSecondaryNavigationButton(Res.get("multiSig"),
                NavigationTarget.BISQ_MULTI_SIG);
        LeftNavSubButton xmrSwap = createSecondaryNavigationButton(Res.get("xmrSwap"),
                NavigationTarget.ATOMIC_CROSS_CHAIN_SWAP);
        LeftNavSubButton lightning = createSecondaryNavigationButton(Res.get("lightning"),
                NavigationTarget.LN_3_PARTY);
        LeftNavSubButton bsqSwap = createSecondaryNavigationButton(Res.get("bsqSwap"),
                NavigationTarget.BSQ_SWAP);
        tradeSubMenuItems.getChildren().addAll(satoshiSquare, liquidSwap, multiSig, xmrSwap, lightning, bsqSwap);
         /*  social.setOnAction(() -> {
            controller.onNavigationTargetSelected(NavigationTarget.SOCIAL);
            if (model.getMenuExpanded().get()) {
                controller.onToggleExpandMenu();
            }
        });*/

        networkInfoBox = new NetworkInfoBox(model,
                () -> controller.onNavigationTargetSelected(NavigationTarget.NETWORK_INFO));
        Layout.pinToAnchorPane(networkInfoBox, null, null, 18, 0);

        // controller.onNavigationButtonCreated(NavigationTarget.NETWORK_INFO);

        expandIcon = Icons.getIcon(AwesomeIcon.CHEVRON_SIGN_RIGHT, "16");
        expandIcon.setCursor(Cursor.HAND);
        expandIcon.setLayoutY(menuTop - EXPAND_ICON_SIZE - 3);
        expandIcon.setLayoutX(MARKER_WIDTH + COLLAPSED_WIDTH - EXPAND_ICON_SIZE);
        expandIcon.setOpacity(0);

        collapseIcon = Icons.getIcon(AwesomeIcon.CHEVRON_SIGN_LEFT, "16");
        collapseIcon.setCursor(Cursor.HAND);
        collapseIcon.setLayoutY(menuTop - EXPAND_ICON_SIZE - 3);
        collapseIcon.setLayoutX(MARKER_WIDTH + EXPANDED_WIDTH - EXPAND_ICON_SIZE);
        collapseIcon.setOpacity(0);


        logoExpanded = ImageUtil.getImageViewById("logo-grey");
        VBox.setMargin(logoExpanded, new Insets(0, 0, 0, 11));
        logoExpanded.setLayoutX(28);
        logoExpanded.setLayoutY(20);

        logoCollapsed = ImageUtil.getImageViewById("logo-mark-grey");
        VBox.setMargin(logoCollapsed, new Insets(0, 0, 0, 11));
        logoCollapsed.setLayoutX(logoExpanded.getLayoutX());
        logoCollapsed.setLayoutY(logoExpanded.getLayoutY());

        selectionMarker = new Region();
        selectionMarker.getStyleClass().add("bisq-green-line");
        selectionMarker.setPrefWidth(3);
        selectionMarker.setPrefHeight(LeftNavButton.HEIGHT);

        mainMenuItems.getChildren().addAll(social, trade, tradeSubMenuItems, markets, wallet, support, settings);
        mainMenuItems.setLayoutY(menuTop);
        root.getChildren().addAll(logoExpanded, logoCollapsed, selectionMarker, mainMenuItems, expandIcon, collapseIcon, networkInfoBox);
    }

    @Override
    protected void onViewAttached() {
        expandIcon.setOnMouseClicked(e -> controller.onToggleExpandMenu());
        collapseIcon.setOnMouseClicked(e -> controller.onToggleExpandMenu());
        navigationTargetSubscription = EasyBind.subscribe(model.getSelectedNavigationTarget(), navigationTarget -> {
            if (navigationTarget != null) {
                controller.findNavButton(navigationTarget).ifPresent(toggleGroup::selectToggle);
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
                UIScheduler.run(() -> model.getLeftNavButtons()
                                .forEach(e -> e.setMenuExpanded(menuExpanding, duration.get() / 2)))
                        .after(duration.get() / 2);
                Transitions.animateLeftNavigationWidth(mainMenuItems, EXPANDED_WIDTH, duration.get());
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
                model.getLeftNavButtons().forEach(e -> e.setMenuExpanded(menuExpanding, duration.get() / 2));
                UIScheduler.run(() -> {
                            Transitions.animateLeftNavigationWidth(mainMenuItems, COLLAPSED_WIDTH, duration.get());
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

    private LeftNavButton createNavigationButton(String title, ImageView icon, NavigationTarget navigationTarget) {
        LeftNavButton button = new LeftNavButton(title, icon, toggleGroup, navigationTarget);
        setupButtonHandler(navigationTarget, button);
        return button;
    }

    private LeftNavSubButton createSecondaryNavigationButton(String title, NavigationTarget navigationTarget) {
        LeftNavSubButton button = new LeftNavSubButton(title, toggleGroup, navigationTarget);
        setupButtonHandler(navigationTarget, button);
        return button;
    }

    private void setupButtonHandler(NavigationTarget navigationTarget, LeftNavButton button) {
        button.setOnAction(() -> {
            if (button.isSelected()) {
                controller.onToggleExpandMenu();
            } else {
                controller.onNavigationTargetSelected(navigationTarget);
                maybeAnimateMark();
            }
        });
        controller.onNavigationButtonCreated(button);
    }

    private void maybeAnimateMark() {
        LeftNavButton selectedLeftNavButton = model.getSelectedNavigationButton().get();
        if (selectedLeftNavButton == null) {
            return;
        }
        UIThread.runOnNextRenderFrame(() -> {
            double targetY = menuTop + selectedLeftNavButton.getBoundsInParent().getMinY();
            if (selectedLeftNavButton instanceof LeftNavSubButton) {
                targetY += tradeSubMenuItems.getLayoutY();
            }
            Transitions.animateNavigationButtonMarks(selectionMarker, selectedLeftNavButton.getHeight(),
                    targetY);
        });
    }

    private static class NetworkInfoBox extends VBox {
        private NetworkInfoBox(LeftNavModel model, Runnable handler) {
            setOnMouseClicked(e -> handler.run());

            HBox clearNetBox = getTransportTypeBox(
                    model.getTorNumConnections(),
                    model.getI2pNumConnections(),
                    model.getTorEnabled(),
                    model.getI2pEnabled()
            );

            Region line = new Region();
            line.setMinHeight(1);
            line.getStyleClass().add("bisq-grey-line");
            setMinHeight(LeftNavButton.HEIGHT);
            setMaxHeight(LeftNavButton.HEIGHT);
            Insets insets = new Insets(21, 0, 0, 35);
            VBox.setMargin(clearNetBox, insets);
            getChildren().addAll(line, clearNetBox);
        }

        private HBox getTransportTypeBox(StringProperty numConnectionsTor,
                                         StringProperty numConnectionsI2p,
                                         BooleanProperty torEnabled,
                                         BooleanProperty i2pEnabled) {
            HBox hBox = new HBox();
            hBox.setSpacing(5);
            hBox.setMinHeight(20);
            hBox.setMaxHeight(hBox.getMinHeight());

            Label peers = new Label(Res.get("peers"));
            peers.getStyleClass().add("bisq-small-dimmed-label");

            Label numConnectionsTorLabel = new Label();
            numConnectionsTorLabel.getStyleClass().add("bisq-smaller-label");
            numConnectionsTorLabel.textProperty().bind(numConnectionsTor);

            Label separator = new Label("|");
            separator.getStyleClass().add("bisq-small-dimmed-label");
            Label separator2 = new Label("|");
            separator2.getStyleClass().add("bisq-small-dimmed-label");

            Label numConnectionsI2pLabel = new Label();
            numConnectionsI2pLabel.getStyleClass().add("bisq-smaller-label");
            numConnectionsI2pLabel.textProperty().bind(numConnectionsI2p);

            ImageView torIcon = new ImageView();
            torIcon.setId("tor");
            ImageView i2pIcon = new ImageView();
            i2pIcon.setId("i2p");

            EasyBind.subscribe(torEnabled, value -> {
                torIcon.setOpacity(value ? 1 : 0.15);
            });
            EasyBind.subscribe(i2pEnabled, value -> {
                i2pIcon.setOpacity(value ? 0.7 : 0.1);
            });

            HBox.setMargin(torIcon, new Insets(0, 0, 0, 10));
            hBox.getChildren().addAll(peers, numConnectionsTorLabel, separator, numConnectionsI2pLabel, torIcon, separator2, i2pIcon);
            return hBox;
        }
    }
}
