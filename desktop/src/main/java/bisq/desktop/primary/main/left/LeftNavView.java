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
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.primary.main.top.TopPanelView;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class LeftNavView extends View<AnchorPane, LeftNavModel, LeftNavController> {
    public final static int EXPANDED_WIDTH = 220;
    private final static int COLLAPSED_WIDTH = 70;
    private final static int MARKER_WIDTH = 3;
    private final static int EXPAND_ICON_SIZE = 18;

    private final ToggleGroup toggleGroup = new ToggleGroup();
    @Getter
    private final NetworkInfoBox networkInfoBox;
    private final Button expandIcon, collapseIcon;
    private final ImageView logoExpanded, logoCollapsed;
    private final Region selectionMarker;
    private final VBox mainMenuItems;
    private final int menuTop;
    private Subscription navigationTargetSubscription, menuExpandedSubscription, selectedNavigationButtonPin;

    public LeftNavView(LeftNavModel model, LeftNavController controller) {
        super(new AnchorPane(), model, controller);

        root.getStyleClass().add("bisq-dark-bg");

        menuTop = TopPanelView.HEIGHT;

        mainMenuItems = new VBox();
        mainMenuItems.setSpacing(6);
        Layout.pinToAnchorPane(mainMenuItems, menuTop, 0, 0, MARKER_WIDTH);

        LeftNavButton dashBoard = createNavigationButton(Res.get("dashboard"),
                "nav-community",
                NavigationTarget.DASHBOARD, false);

        LeftNavButton trade = createNavigationButton(Res.get("trade"),
                "nav-trade",
                NavigationTarget.TRADE_OVERVIEW, true);

        VBox tradeSubMenuItems = createSubmenu(
                createSubmenuNavigationButton(Res.get("bisqEasy"), NavigationTarget.BISQ_EASY, trade),
                createSubmenuNavigationButton(Res.get("liquidSwap"), NavigationTarget.LIQUID_SWAP, trade),
                // createSubmenuNavigationButton(Res.get("moneroSwap"), NavigationTarget.MONERO_SWAP, trade),
                createSubmenuNavigationButton(Res.get("multiSig"), NavigationTarget.BISQ_MULTISIG, trade),
                // createSubmenuNavigationButton(Res.get("lightning"), NavigationTarget.LIGHTNING_X, trade),
                createSubmenuNavigationButton(Res.get("bsqSwap"), NavigationTarget.BSQ_SWAP, trade)
        );

        LeftNavButton wallet = createNavigationButton(Res.get("wallet"),
                "nav-wallet",
                NavigationTarget.WALLET, false);

        LeftNavButton learn = createNavigationButton(Res.get("learn"),
                "nav-learn",
                NavigationTarget.ACADEMY_OVERVIEW, true);

        VBox learnSubMenuItems = createSubmenu(
                createSubmenuNavigationButton(Res.get("academy.bisq"), NavigationTarget.BISQ_ACADEMY, learn),
                createSubmenuNavigationButton(Res.get("academy.bitcoin"), NavigationTarget.BITCOIN_ACADEMY, learn),
                createSubmenuNavigationButton(Res.get("academy.security"), NavigationTarget.SECURITY_ACADEMY, learn),
                createSubmenuNavigationButton(Res.get("academy.privacy"), NavigationTarget.PRIVACY_ACADEMY, learn),
                createSubmenuNavigationButton(Res.get("academy.wallets"), NavigationTarget.WALLETS_ACADEMY, learn),
                createSubmenuNavigationButton(Res.get("academy.foss"), NavigationTarget.FOSS_ACADEMY, learn)
        );

        LeftNavButton chat = createNavigationButton(Res.get("discussion"),
                "nav-chat",
                NavigationTarget.DISCUSS, false);

        LeftNavButton events = createNavigationButton(Res.get("events"),
                "nav-events",
                NavigationTarget.EVENTS, false);

        LeftNavButton support = createNavigationButton(Res.get("support"),
                "nav-support",
                NavigationTarget.SUPPORT, false);


        //todo lower priority menu add design

        LeftNavButton settings = createNavigationButton(Res.get("settings"),
                "nav-settings",
                NavigationTarget.SETTINGS, false);

        networkInfoBox = new NetworkInfoBox(model,
                () -> controller.onNavigationTargetSelected(NavigationTarget.NETWORK_INFO));
        Layout.pinToAnchorPane(networkInfoBox, null, null, 18, 0);

        expandIcon = BisqIconButton.createIconButton(AwesomeIcon.CHEVRON_SIGN_RIGHT, "16");
        expandIcon.setCursor(Cursor.HAND);
        expandIcon.setLayoutY(menuTop - 3);
        expandIcon.setLayoutX(MARKER_WIDTH + COLLAPSED_WIDTH - EXPAND_ICON_SIZE);
        expandIcon.setOpacity(0);
        Tooltip.install(expandIcon, new BisqTooltip(Res.get("navigation.expandIcon.tooltip")));

        collapseIcon = BisqIconButton.createIconButton(AwesomeIcon.CHEVRON_SIGN_LEFT, "16");
        collapseIcon.setCursor(Cursor.HAND);
        collapseIcon.setLayoutY(menuTop - 3);
        collapseIcon.setLayoutX(MARKER_WIDTH + EXPANDED_WIDTH - EXPAND_ICON_SIZE);
        collapseIcon.setOpacity(0);
        Tooltip.install(collapseIcon, new BisqTooltip(Res.get("navigation.collapseIcon.tooltip")));

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

        mainMenuItems.getChildren().addAll(dashBoard, trade, tradeSubMenuItems,
                learn, learnSubMenuItems,
                chat, events, support,
                settings);
        if (model.isWalletEnabled()) {
            mainMenuItems.getChildren().add(3, wallet);
        }

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

        menuExpandedSubscription = EasyBind.subscribe(model.getMenuExpanded(), menuExpanded -> {
            int width = menuExpanded ? EXPANDED_WIDTH : COLLAPSED_WIDTH;
            AtomicInteger duration = new AtomicInteger(400);
            if (menuExpanded) {
                UIScheduler.run(() -> model.getLeftNavButtons()
                                .forEach(e -> e.setMenuExpanded(true, duration.get() / 2)))
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
                    Transitions.fadeIn(collapseIcon, Transitions.DEFAULT_DURATION, 0.3, null);
                }).after(duration.get() + 100);
            } else {
                expandIcon.setOpacity(0);
                expandIcon.setVisible(true);
                expandIcon.setManaged(true);

                model.getLeftNavButtons().forEach(e -> e.setMenuExpanded(false, duration.get() / 2));
                UIScheduler.run(() -> {
                            Transitions.animateLeftNavigationWidth(mainMenuItems, COLLAPSED_WIDTH, duration.get());
                            collapseIcon.setVisible(false);
                            collapseIcon.setManaged(false);
                            collapseIcon.setOpacity(0);
                            Transitions.fadeIn(expandIcon, 2 * Transitions.DEFAULT_DURATION, 0.3, null);
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
                Transitions.fadeIn(collapseIcon, Transitions.DEFAULT_DURATION, 0.3, null);
            }
        });
        root.setOnMouseExited(e -> {
            if (collapseIcon.isVisible()) {
                Transitions.fadeOut(collapseIcon);
            }
        });

        selectedNavigationButtonPin = EasyBind.subscribe(model.getSelectedNavigationButton(), button -> {
            if (button != null) {
                maybeAnimateMark();
                selectedNavigationButtonPin.unsubscribe();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        expandIcon.setOnMouseClicked(null);
        collapseIcon.setOnMouseClicked(null);
        navigationTargetSubscription.unsubscribe();
        menuExpandedSubscription.unsubscribe();
        selectedNavigationButtonPin.unsubscribe();
        root.setOnMouseEntered(null);
        root.setOnMouseExited(null);
    }

    private LeftNavButton createNavigationButton(String title,
                                                 String iconId,
                                                 NavigationTarget navigationTarget,
                                                 boolean hasSubmenu) {
        LeftNavButton button = new LeftNavButton(title, iconId, toggleGroup, navigationTarget, hasSubmenu);
        setupButtonHandler(navigationTarget, button);
        return button;
    }

    private LeftNavSubButton createSubmenuNavigationButton(String title, NavigationTarget navigationTarget, LeftNavButton parentButton) {
        LeftNavSubButton button = new LeftNavSubButton(title, toggleGroup, navigationTarget, parentButton);
        setupButtonHandler(navigationTarget, button);
        VBox.setVgrow(button, Priority.ALWAYS);
        return button;
    }

    private VBox createSubmenu(LeftNavSubButton... items) {
        VBox submenu = new VBox(2);
        VBox.setMargin(submenu, new Insets(-10, 0, 0, 0));
        submenu.setPadding(new Insets(0, 0, 16, 0));
        submenu.setMinHeight(0);
        submenu.setPrefHeight(0);
        submenu.getChildren().setAll(items);
        return submenu;
    }

    private void setupButtonHandler(NavigationTarget navigationTarget, LeftNavButton button) {
        button.setOnAction(() -> {
            controller.onNavigationTargetSelected(navigationTarget);
            maybeAnimateMark();
        });
        controller.onNavigationButtonCreated(button);
    }

    private void maybeAnimateMark() {
        LeftNavButton selectedLeftNavButton = model.getSelectedNavigationButton().get();
        UIThread.runOnNextRenderFrame(() -> {
            double targetY = menuTop + selectedLeftNavButton.getBoundsInParent().getMinY();
            if (selectedLeftNavButton instanceof LeftNavSubButton) {
                for (int i = 0; i < mainMenuItems.getChildren().size(); i++) {
                    Node item = mainMenuItems.getChildren().get(i);
                    if (item instanceof VBox) {
                        VBox submenu = (VBox) item;
                        if (submenu.getChildren().contains(selectedLeftNavButton)) {
                            targetY += submenu.getLayoutY();
                            break;
                        }
                    }
                }
            }
            Transitions.animateNavigationButtonMarks(selectionMarker, selectedLeftNavButton.getHeight(), targetY);
            updateSubmenu();
        });
    }

    private void updateSubmenu() {
        LeftNavButton selectedLeftNavButton = model.getSelectedNavigationButton().get();
        for (int i = 0; i < mainMenuItems.getChildren().size(); i++) {
            Node item = mainMenuItems.getChildren().get(i);
            if (item instanceof VBox) {
                VBox submenu = (VBox) item;
                LeftNavButton parentMenuItem = (LeftNavButton) mainMenuItems.getChildren().get(i - 1);
                boolean isSubmenuActive = submenu.getChildren().contains(selectedLeftNavButton)
                        || selectedLeftNavButton.navigationTarget == parentMenuItem.navigationTarget;
                parentMenuItem.setHighlighted(isSubmenuActive);

                if (isSubmenuActive) {
                    parentMenuItem.getWasSelected().set(true);
                }
                UIThread.runOnNextRenderFrame(parentMenuItem::applyStyle);
                int targetHeight = isSubmenuActive || (parentMenuItem.getWasSelected().get()) ?
                        (LeftNavSubButton.HEIGHT + 2) * submenu.getChildren().size() : 0;
                Transitions.animateHeight(submenu, targetHeight);
            }
        }
    }

    private static class NetworkInfoBox extends HBox {
        private NetworkInfoBox(LeftNavModel model, Runnable handler) {
            setMinHeight(53);
            setMaxHeight(53);
            setPadding(new Insets(26, 24, 0, 24));

            setOnMouseClicked(e -> handler.run());

            getChildren().addAll(
                    getNetworkBox(
                            Res.get("peers"),
                            "tor",
                            model.getTorNumConnections(),
                            model.getTorNumTargetConnections(),
                            model.getTorEnabled()
                    ),
                    Spacer.fillHBox(),
                    getNetworkBox(
                            Res.get("i2p"),
                            "i2p",
                            model.getI2pNumConnections(),
                            model.getI2pNumTargetConnections(),
                            model.getI2pEnabled()
                    )
            );
        }

        private HBox getNetworkBox(String title,
                                   String imageId,
                                   StringProperty numConnections,
                                   StringProperty numTargetConnections,
                                   BooleanProperty networkEnabled) {

            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("bisq-small-dimmed-label");

            Label numConnectionsLabel = new Label();
            numConnectionsLabel.getStyleClass().add("bisq-smaller-label");
            numConnectionsLabel.textProperty().bind(numConnections);

            Label separator = new Label("|");
            separator.getStyleClass().add("bisq-small-dimmed-label");

            Label numTargetConnectionsLabel = new Label();
            numTargetConnectionsLabel.getStyleClass().add("bisq-smaller-label");
            numTargetConnectionsLabel.textProperty().bind(numTargetConnections);

            ImageView icon = ImageUtil.getImageViewById(imageId);
            EasyBind.subscribe(networkEnabled, value -> icon.setOpacity(value ? 1 : 0.5));
            HBox.setMargin(icon, new Insets(0, 0, 0, 2));

            return new HBox(5, titleLabel, numConnectionsLabel, separator, numTargetConnectionsLabel, icon);
        }
    }
}
