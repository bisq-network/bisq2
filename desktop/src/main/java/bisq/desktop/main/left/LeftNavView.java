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

package bisq.desktop.main.left;

import bisq.desktop.common.Layout;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.top.TopPanelView;
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
    private final Button horizontalExpandIcon, horizontalCollapseIcon;
    private final ImageView logoExpanded, logoCollapsed;
    private final Region selectionMarker;
    private final VBox mainMenuItems;
    private final int menuTop;
    private final LeftNavButton tradeAppsButton, learnButton, authorizedRole;
    private Subscription navigationTargetSubscription, menuExpandedSubscription, selectedNavigationButtonPin, tradeAppsSubMenuExpandedPin, learnsSubMenuExpandedPin;

    public LeftNavView(LeftNavModel model, LeftNavController controller) {
        super(new AnchorPane(), model, controller);

        root.getStyleClass().add("bisq-dark-bg");

        menuTop = TopPanelView.HEIGHT;

        mainMenuItems = new VBox();
        mainMenuItems.setSpacing(6);
        Layout.pinToAnchorPane(mainMenuItems, menuTop, 0, 0, MARKER_WIDTH);

        LeftNavButton dashBoard = createNavigationButton(Res.get("navigation.dashboard"),
                "nav-community",
                NavigationTarget.DASHBOARD, false);

        tradeAppsButton = createNavigationButton(Res.get("navigation.tradeApps"),
                "nav-trade",
                NavigationTarget.TRADE_OVERVIEW, true);

        VBox tradeSubMenuItems = createSubmenu(
                createSubmenuNavigationButton(Res.get("navigation.tradeApps.bisqEasy"), NavigationTarget.BISQ_EASY, tradeAppsButton),
                createSubmenuNavigationButton(Res.get("navigation.tradeApps.multiSig"), NavigationTarget.MULTISIG, tradeAppsButton),
                createSubmenuNavigationButton(Res.get("navigation.tradeApps.submarine"), NavigationTarget.SUBMARINE, tradeAppsButton),
                createSubmenuNavigationButton(Res.get("navigation.tradeApps.liquidMultisig"), NavigationTarget.LIQUID_MULTISIG, tradeAppsButton),
                createSubmenuNavigationButton(Res.get("navigation.tradeApps.lightningFiat"), NavigationTarget.LIGHTNING_FIAT, tradeAppsButton),
                createSubmenuNavigationButton(Res.get("navigation.tradeApps.lightningEscrow"), NavigationTarget.LIGHTNING_ESCROW, tradeAppsButton),
                createSubmenuNavigationButton(Res.get("navigation.tradeApps.moneroSwap"), NavigationTarget.MONERO_SWAP, tradeAppsButton),
                createSubmenuNavigationButton(Res.get("navigation.tradeApps.liquidSwap"), NavigationTarget.LIQUID_SWAP, tradeAppsButton),
                createSubmenuNavigationButton(Res.get("navigation.tradeApps.bsqSwap"), NavigationTarget.BSQ_SWAP, tradeAppsButton)
        );

        LeftNavButton wallet = createNavigationButton(Res.get("navigation.wallet"),
                "nav-wallet",
                NavigationTarget.WALLET, false);

        learnButton = createNavigationButton(Res.get("navigation.academy"),
                "nav-learn",
                NavigationTarget.ACADEMY_OVERVIEW, true);

        VBox learnSubMenuItems = createSubmenu(
                createSubmenuNavigationButton(Res.get("navigation.academy.bisq"), NavigationTarget.BISQ_ACADEMY, learnButton),
                createSubmenuNavigationButton(Res.get("navigation.academy.bitcoin"), NavigationTarget.BITCOIN_ACADEMY, learnButton),
                createSubmenuNavigationButton(Res.get("navigation.academy.security"), NavigationTarget.SECURITY_ACADEMY, learnButton),
                createSubmenuNavigationButton(Res.get("navigation.academy.privacy"), NavigationTarget.PRIVACY_ACADEMY, learnButton),
                createSubmenuNavigationButton(Res.get("navigation.academy.wallets"), NavigationTarget.WALLETS_ACADEMY, learnButton),
                createSubmenuNavigationButton(Res.get("navigation.academy.foss"), NavigationTarget.FOSS_ACADEMY, learnButton)
        );

        LeftNavButton chat = createNavigationButton(Res.get("navigation.discussion"),
                "nav-chat",
                NavigationTarget.DISCUSSION, false);

        LeftNavButton events = createNavigationButton(Res.get("navigation.events"),
                "nav-events",
                NavigationTarget.EVENTS, false);

        LeftNavButton support = createNavigationButton(Res.get("navigation.support"),
                "nav-support",
                NavigationTarget.SUPPORT, false);


        //todo lower priority menu add design

        LeftNavButton user = createNavigationButton(Res.get("navigation.userOptions"),
                "nav-user",
                NavigationTarget.USER, false);
        LeftNavButton settings = createNavigationButton(Res.get("navigation.settings"),
                "nav-settings",
                NavigationTarget.SETTINGS, false);


        authorizedRole = createNavigationButton(Res.get("navigation.authorizedRole"),
                "nav-authorized-role",
                NavigationTarget.AUTHORIZED_ROLE, false);

        networkInfoBox = new NetworkInfoBox(model,
                () -> controller.onNavigationTargetSelected(NavigationTarget.NETWORK_INFO));
        Layout.pinToAnchorPane(networkInfoBox, null, null, 18, 0);

        horizontalExpandIcon = BisqIconButton.createIconButton(AwesomeIcon.CHEVRON_SIGN_RIGHT, "16");
        horizontalExpandIcon.setCursor(Cursor.HAND);
        horizontalExpandIcon.setLayoutY(menuTop - 3);
        horizontalExpandIcon.setLayoutX(MARKER_WIDTH + COLLAPSED_WIDTH - EXPAND_ICON_SIZE);
        horizontalExpandIcon.setOpacity(0);
        Tooltip.install(horizontalExpandIcon, new BisqTooltip(Res.get("navigation.expandIcon.tooltip")));

        horizontalCollapseIcon = BisqIconButton.createIconButton(AwesomeIcon.CHEVRON_SIGN_LEFT, "16");
        horizontalCollapseIcon.setCursor(Cursor.HAND);
        horizontalCollapseIcon.setLayoutY(menuTop - 3);
        horizontalCollapseIcon.setLayoutX(MARKER_WIDTH + EXPANDED_WIDTH - EXPAND_ICON_SIZE);
        horizontalCollapseIcon.setOpacity(0);
        Tooltip.install(horizontalCollapseIcon, new BisqTooltip(Res.get("navigation.collapseIcon.tooltip")));

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

        mainMenuItems.getChildren().addAll(dashBoard, tradeAppsButton, tradeSubMenuItems,
                learnButton, learnSubMenuItems,
                chat, events, support,
                user, settings, authorizedRole);
        if (model.isWalletEnabled()) {
            mainMenuItems.getChildren().add(3, wallet);
        }

        mainMenuItems.setLayoutY(menuTop);
        root.getChildren().addAll(logoExpanded, logoCollapsed, selectionMarker, mainMenuItems, horizontalExpandIcon, horizontalCollapseIcon, networkInfoBox);
    }

    @Override
    protected void onViewAttached() {
        authorizedRole.visibleProperty().bind(model.getAuthorizedRoleVisible());
        authorizedRole.managedProperty().bind(model.getAuthorizedRoleVisible());

        horizontalExpandIcon.setOnMouseClicked(e -> controller.onToggleHorizontalExpandState());
        horizontalCollapseIcon.setOnMouseClicked(e -> controller.onToggleHorizontalExpandState());
        navigationTargetSubscription = EasyBind.subscribe(model.getSelectedNavigationTarget(), navigationTarget -> {
            if (navigationTarget != null) {
                controller.findNavButton(navigationTarget).ifPresent(toggleGroup::selectToggle);
                maybeAnimateMark();
            }
        });
        horizontalExpandIcon.setVisible(false);
        horizontalExpandIcon.setManaged(false);

        menuExpandedSubscription = EasyBind.subscribe(model.getMenuHorizontalExpanded(), menuExpanded -> {
            int width = menuExpanded ? EXPANDED_WIDTH : COLLAPSED_WIDTH;
            AtomicInteger duration = new AtomicInteger(400);
            if (menuExpanded) {
                UIScheduler.run(() -> model.getLeftNavButtons()
                                .forEach(e -> e.setHorizontalExpanded(true, duration.get() / 2)))
                        .after(duration.get() / 2);
                Transitions.animateLeftNavigationWidth(mainMenuItems, EXPANDED_WIDTH, duration.get());
                networkInfoBox.setPrefWidth(width + MARKER_WIDTH);
                Transitions.fadeIn(networkInfoBox, duration.get());

                Transitions.fadeOut(horizontalExpandIcon, duration.get() / 4, () -> {
                    horizontalExpandIcon.setVisible(false);
                    horizontalExpandIcon.setManaged(false);
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
                    horizontalCollapseIcon.setOpacity(0);
                    horizontalCollapseIcon.setVisible(true);
                    horizontalCollapseIcon.setManaged(true);
                    Transitions.fadeIn(horizontalCollapseIcon, Transitions.DEFAULT_DURATION, 0.3, null);
                }).after(duration.get() + 100);
            } else {
                horizontalExpandIcon.setOpacity(0);
                horizontalExpandIcon.setVisible(true);
                horizontalExpandIcon.setManaged(true);

                model.getLeftNavButtons().forEach(e -> e.setHorizontalExpanded(false, duration.get() / 2));
                UIScheduler.run(() -> {
                            Transitions.animateLeftNavigationWidth(mainMenuItems, COLLAPSED_WIDTH, duration.get());
                            horizontalCollapseIcon.setVisible(false);
                            horizontalCollapseIcon.setManaged(false);
                            horizontalCollapseIcon.setOpacity(0);
                            Transitions.fadeIn(horizontalExpandIcon, 2 * Transitions.DEFAULT_DURATION, 0.3, null);
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
            if (horizontalCollapseIcon.isVisible()) {
                Transitions.fadeIn(horizontalCollapseIcon, Transitions.DEFAULT_DURATION, 0.3, null);
            }
        });
        root.setOnMouseExited(e -> {
            if (horizontalCollapseIcon.isVisible()) {
                Transitions.fadeOut(horizontalCollapseIcon);
            }
        });

        selectedNavigationButtonPin = EasyBind.subscribe(model.getSelectedNavigationButton(), button -> {
            if (button != null) {
                maybeAnimateMark();
                selectedNavigationButtonPin.unsubscribe();
            }
        });

        tradeAppsSubMenuExpandedPin = EasyBind.subscribe(model.getTradeAppsSubMenuExpanded(),
                tradeAppsButton::setVerticalExpanded);
        learnsSubMenuExpandedPin = EasyBind.subscribe(model.getLearnsSubMenuExpanded(),
                learnButton::setVerticalExpanded);
    }

    @Override
    protected void onViewDetached() {
        authorizedRole.visibleProperty().unbind();
        authorizedRole.managedProperty().unbind();
        horizontalExpandIcon.setOnMouseClicked(null);
        horizontalCollapseIcon.setOnMouseClicked(null);
        navigationTargetSubscription.unsubscribe();
        menuExpandedSubscription.unsubscribe();
        selectedNavigationButtonPin.unsubscribe();
        tradeAppsSubMenuExpandedPin.unsubscribe();
        learnsSubMenuExpandedPin.unsubscribe();
        root.setOnMouseEntered(null);
        root.setOnMouseExited(null);
    }

    private LeftNavButton createNavigationButton(String title,
                                                 String iconId,
                                                 NavigationTarget navigationTarget,
                                                 boolean hasSubmenu) {
        LeftNavButton button = new LeftNavButton(title,
                iconId,
                toggleGroup,
                navigationTarget,
                hasSubmenu,
                this::verticalExpandCollapseHandler);
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
            if (button.isHasSubmenu()) {
                // Always expand menu when header button is clicked
                button.getIsSubMenuExpanded().set(true);
            }
            maybeAnimateMark();
        });
        controller.onNavigationButtonCreated(button);

        EasyBind.subscribe(button.getIsSubMenuExpanded(), expanded -> {
            if (button == tradeAppsButton) {
                controller.onTradeAppsSubMenuExpanded(expanded);
            } else if (button == learnButton) {
                controller.onLearSubMenuExpanded(expanded);
            }
        });
    }

    private void verticalExpandCollapseHandler(NavigationTarget navigationTarget) {
        if (model.getSelectedNavigationButton().get() == null) {
            // There is no button selected on startup
            controller.onNavigationTargetSelected(navigationTarget);
        }
        maybeAnimateMark();
    }

    private void maybeAnimateMark() {
        LeftNavButton selectedLeftNavButton = model.getSelectedNavigationButton().get();
        if (selectedLeftNavButton == null) return;

        LeftNavButton buttonForHeight;
        if (selectedLeftNavButton instanceof LeftNavSubButton) {
            LeftNavSubButton leftNavSubButton = (LeftNavSubButton) selectedLeftNavButton;
            LeftNavButton parentButton = leftNavSubButton.getParentButton();
            if (!parentButton.getIsSubMenuExpanded().get()) {
                buttonForHeight = parentButton;
            } else {
                buttonForHeight = selectedLeftNavButton;
            }
        } else {
            buttonForHeight = selectedLeftNavButton;
        }

        if (buttonForHeight.getHeight() > 0) {
            Transitions.animateNavigationButtonMarks(selectionMarker,
                    buttonForHeight.getHeight(),
                    calculateTargetY());
        } else {
            // Duration for animation for opening submenu is Transitions.DEFAULT_DURATION / 2.
            // We only know target position after the initial animation is done.
            LeftNavButton finalButtonForHeight = buttonForHeight;
            UIScheduler.run(() -> Transitions.animateNavigationButtonMarks(selectionMarker,
                            finalButtonForHeight.getHeight(),
                            calculateTargetY()))
                    .after(Transitions.getDuration(Transitions.DEFAULT_DURATION / 2));
        }
        updateSubmenu();
    }

    private double calculateTargetY() {
        LeftNavButton selectedLeftNavButton = model.getSelectedNavigationButton().get();
        double targetY = menuTop + selectedLeftNavButton.getBoundsInParent().getMinY();
        if (selectedLeftNavButton instanceof LeftNavSubButton) {
            LeftNavSubButton leftNavSubButton = (LeftNavSubButton) selectedLeftNavButton;
            LeftNavButton parentButton = leftNavSubButton.getParentButton();
            if (parentButton.getIsSubMenuExpanded().get()) {
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
            } else {
                targetY = menuTop + parentButton.getBoundsInParent().getMinY();
            }
        }
        return targetY;
    }

    private void updateSubmenu() {
        LeftNavButton selectedLeftNavButton = model.getSelectedNavigationButton().get();
        for (int i = 0; i < mainMenuItems.getChildren().size(); i++) {
            Node item = mainMenuItems.getChildren().get(i);
            if (item instanceof VBox) {
                VBox submenu = (VBox) item;
                LeftNavButton parentMenuItem = (LeftNavButton) mainMenuItems.getChildren().get(i - 1);

                parentMenuItem.setHighlighted(submenu.getChildren().contains(selectedLeftNavButton));

                UIThread.runOnNextRenderFrame(parentMenuItem::applyStyle);
                int targetHeight = parentMenuItem.getIsSubMenuExpanded().get() ?
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
                            Res.get("navigation.network.info.tor"),
                            "tor",
                            model.getTorNumConnections(),
                            model.getTorNumTargetConnections(),
                            model.getTorEnabled()
                    ),
                    Spacer.fillHBox(),
                    getNetworkBox(
                            Res.get("navigation.network.info.i2p"),
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
