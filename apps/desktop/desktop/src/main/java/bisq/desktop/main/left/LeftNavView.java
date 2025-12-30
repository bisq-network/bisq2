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
import bisq.desktop.common.ManagedDuration;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.top.TopPanelView;
import bisq.desktop.navigation.NavigationTarget;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public class LeftNavView extends View<AnchorPane, LeftNavModel, LeftNavController> {
    public final static int EXPANDED_WIDTH = 190;
    private final static int COLLAPSED_WIDTH = 70;
    private final static int MARKER_WIDTH = 3;
    private final static int EXPAND_ICON_SIZE = 18;

    private final ToggleGroup toggleGroup = new ToggleGroup();
    private final Button horizontalExpandIcon, horizontalCollapseIcon;
    private final ImageView logoExpanded, logoCollapsed;
    private final Region selectionMarker;
    private final VBox mainMenuItems, networkInfoRoot;
    private final int menuTop;
    private final LeftNavButton authorizedRole, muSigLeftNavButton, wallet;
    private final Label version;
    private Subscription navigationTargetSubscription, menuExpandedSubscription,
            selectedNavigationButtonPin, newVersionAvailablePin, isMuSigActivatedPin;

    public LeftNavView(LeftNavModel model, LeftNavController controller, VBox networkInfoRoot) {
        super(new AnchorPane(), model, controller);

        this.networkInfoRoot = networkInfoRoot;

        root.getStyleClass().add("bisq-dark-bg");

        menuTop = TopPanelView.HEIGHT;

        mainMenuItems = new VBox();
        mainMenuItems.setSpacing(6);
        mainMenuItems.setPadding(new Insets(0, MARKER_WIDTH, 0, 0));

        LeftNavButton dashBoard = createNavigationButton(Res.get("navigation.dashboard"),
                "nav-community",
                NavigationTarget.DASHBOARD, false);

        LeftNavButton bisqEasy = createNavigationButton(Res.get("navigation.bisqEasy"),
                "nav-bisq-easy",
                NavigationTarget.BISQ_EASY, false);

        muSigLeftNavButton = createNavigationButton(Res.get("navigation.muSig"),
                "nav-musig",
                NavigationTarget.MU_SIG, false);

        LeftNavButton reputation = createNavigationButton(Res.get("navigation.reputation"),
                "nav-reputation",
                NavigationTarget.REPUTATION, false);

        LeftNavButton contactsList = createNavigationButton(Res.get("navigation.contacts"),
                "nav-contacts",
                NavigationTarget.CONTACTS_LIST, false);
        Label contactsListLabel = contactsList.getButtonLabel();
        contactsListLabel.setGraphic(ImageUtil.getImageViewById("new-badge"));
        contactsListLabel.setContentDisplay(ContentDisplay.RIGHT);

        LeftNavButton protocols = createNavigationButton(Res.get("navigation.tradeApps"),
                "nav-trade",
                NavigationTarget.TRADE_PROTOCOLS, false);

        wallet = createNavigationButton(Res.get("navigation.wallet"),
                "nav-wallet",
                NavigationTarget.WALLET, false);

        LeftNavButton learn = createNavigationButton(Res.get("navigation.academy"),
                "nav-learn",
                NavigationTarget.ACADEMY, false);

        LeftNavButton chat = createNavigationButton(Res.get("navigation.chat"),
                "nav-chat",
                NavigationTarget.CHAT, false);

        LeftNavButton support = createNavigationButton(Res.get("navigation.support"),
                "nav-support",
                NavigationTarget.SUPPORT, false);

        LeftNavButton user = createNavigationButton(Res.get("navigation.userOptions"),
                "nav-user",
                NavigationTarget.USER, false);

        LeftNavButton network = createNavigationButton(Res.get("navigation.network"),
                "nav-network",
                NavigationTarget.NETWORK, false);

        LeftNavButton settings = createNavigationButton(Res.get("navigation.settings"),
                "nav-settings",
                NavigationTarget.SETTINGS, false);


        authorizedRole = createNavigationButton(Res.get("navigation.authorizedRole"),
                "nav-authorized-role",
                NavigationTarget.AUTHORIZED_ROLE, false);

        horizontalExpandIcon = BisqIconButton.createIconButton(AwesomeIcon.CHEVRON_SIGN_RIGHT, 16);
        horizontalExpandIcon.setCursor(Cursor.HAND);
        horizontalExpandIcon.setLayoutY(menuTop - 3);
        horizontalExpandIcon.setLayoutX(MARKER_WIDTH + COLLAPSED_WIDTH - EXPAND_ICON_SIZE);
        horizontalExpandIcon.setOpacity(0);
        BisqTooltip tooltip = new BisqTooltip(Res.get("navigation.expandIcon.tooltip"));
        horizontalExpandIcon.setTooltip(tooltip);

        horizontalCollapseIcon = BisqIconButton.createIconButton(AwesomeIcon.CHEVRON_SIGN_LEFT, 16);
        horizontalCollapseIcon.setCursor(Cursor.HAND);
        horizontalCollapseIcon.setLayoutY(menuTop - 3);
        horizontalCollapseIcon.setLayoutX(MARKER_WIDTH + EXPANDED_WIDTH - EXPAND_ICON_SIZE);
        horizontalCollapseIcon.setOpacity(0);
        BisqTooltip tooltip2 = new BisqTooltip(Res.get("navigation.collapseIcon.tooltip"));
        horizontalCollapseIcon.setTooltip(tooltip2);

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

        mainMenuItems.getChildren().addAll(dashBoard, bisqEasy, muSigLeftNavButton, reputation,
                contactsList, protocols, learn, chat, support, user, network, settings, authorizedRole);
        mainMenuItems.getChildren().add(3, wallet);

        mainMenuItems.setLayoutY(menuTop);

        version = new Label(model.getVersion());
        version.setLayoutX(91);
        Pane logoAndVersion = new Pane(logoExpanded, logoCollapsed, version);

        Layout.pinToAnchorPane(mainMenuItems, menuTop, 0, 0, MARKER_WIDTH);
        Layout.pinToAnchorPane(networkInfoRoot, null, null, 40, 0);
        root.getChildren().addAll(logoAndVersion, selectionMarker, mainMenuItems, horizontalExpandIcon, horizontalCollapseIcon, networkInfoRoot);
    }

    @Override
    protected void onViewAttached() {
        wallet.visibleProperty().bind(model.getIsMuSigActivated());
        wallet.managedProperty().bind(model.getIsMuSigActivated());
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

        isMuSigActivatedPin = EasyBind.subscribe(model.getIsMuSigActivated(), isMuSigActivated -> {
            muSigLeftNavButton.setManaged(isMuSigActivated);
            muSigLeftNavButton.setVisible(isMuSigActivated);
        });

        menuExpandedSubscription = EasyBind.subscribe(model.getMenuHorizontalExpanded(), menuExpanded -> {
            int width = menuExpanded ? EXPANDED_WIDTH : COLLAPSED_WIDTH;
            long duration = ManagedDuration.inMillis(400);
            long halfOfDuration = ManagedDuration.inMillis(200);
            long quoterOfDuration = ManagedDuration.inMillis(100);
            if (menuExpanded) {
                UIScheduler.run(() -> model.getLeftNavButtons()
                                .forEach(e -> e.setHorizontalExpanded(true, halfOfDuration)))
                        .after(halfOfDuration);
                Transitions.animateLeftNavigationWidth(mainMenuItems, EXPANDED_WIDTH, duration);
                networkInfoRoot.setPrefWidth(width + MARKER_WIDTH);
                Transitions.fadeIn(networkInfoRoot, duration);

                Transitions.fadeOut(horizontalExpandIcon, quoterOfDuration, () -> {
                    horizontalExpandIcon.setVisible(false);
                    horizontalExpandIcon.setManaged(false);
                });
                UIScheduler.run(() -> {
                    Transitions.fadeIn(logoExpanded, quoterOfDuration, () -> {
                        logoCollapsed.setVisible(false);
                        logoCollapsed.setManaged(false);
                    });
                    logoExpanded.setVisible(true);
                    logoExpanded.setManaged(true);
                    version.setLayoutX(91);
                }).after(halfOfDuration);
                UIScheduler.run(() -> {
                    networkInfoRoot.setOpacity(0);
                    Transitions.fadeIn(networkInfoRoot, quoterOfDuration);
                    networkInfoRoot.setVisible(true);
                    networkInfoRoot.setManaged(true);
                    horizontalCollapseIcon.setOpacity(0);
                    horizontalCollapseIcon.setVisible(true);
                    horizontalCollapseIcon.setManaged(true);
                    Transitions.fadeIn(horizontalCollapseIcon, ManagedDuration.getDefaultDurationMillis(), 0.3, null);
                }).after(ManagedDuration.inMillis(duration + 100));
            } else {
                horizontalExpandIcon.setOpacity(0);
                horizontalExpandIcon.setVisible(true);
                horizontalExpandIcon.setManaged(true);

                model.getLeftNavButtons().forEach(e -> e.setHorizontalExpanded(false, halfOfDuration));
                UIScheduler.run(() -> {
                            Transitions.animateLeftNavigationWidth(mainMenuItems, COLLAPSED_WIDTH, duration);
                            horizontalCollapseIcon.setVisible(false);
                            horizontalCollapseIcon.setManaged(false);
                            horizontalCollapseIcon.setOpacity(0);
                            Transitions.fadeIn(horizontalExpandIcon, 2 * ManagedDuration.getDefaultDurationMillis(), 0.3, null);
                        })
                        .after(quoterOfDuration);
                Transitions.fadeOut(networkInfoRoot, halfOfDuration, () -> {
                    networkInfoRoot.setVisible(false);
                    networkInfoRoot.setManaged(false);
                });
                Transitions.fadeOut(logoExpanded, halfOfDuration, () -> {
                    logoExpanded.setVisible(false);
                    logoExpanded.setManaged(false);
                    version.setLayoutX(53);
                });
                logoCollapsed.setVisible(true);
                logoCollapsed.setManaged(true);
            }
        });

        root.setOnMouseEntered(e -> {
            if (horizontalCollapseIcon.isVisible()) {
                Transitions.fadeIn(horizontalCollapseIcon, ManagedDuration.getDefaultDurationMillis(), 0.3, null);
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

        newVersionAvailablePin = EasyBind.subscribe(model.getIsNewReleaseAvailable(),
                newVersionAvailable -> {
                    if (newVersionAvailable) {
                        version.getStyleClass().remove("bisq-smaller-dimmed-label");
                        version.getStyleClass().addAll("bisq-smaller-label", "text-underline", "hand-cursor");
                        version.setLayoutY(26);
                        version.setOnMouseClicked(e -> controller.onOpenUpdateWindow());
                    } else {
                        version.getStyleClass().add("bisq-smaller-dimmed-label");
                        version.getStyleClass().removeAll("bisq-smaller-label", "text-underline", "hand-cursor");
                        version.setLayoutY(25);
                        version.setOnMouseClicked(null);
                    }
                });
    }

    @Override
    protected void onViewDetached() {
        wallet.visibleProperty().unbind();
        wallet.managedProperty().unbind();
        authorizedRole.visibleProperty().unbind();
        authorizedRole.managedProperty().unbind();
        horizontalExpandIcon.setOnMouseClicked(null);
        horizontalCollapseIcon.setOnMouseClicked(null);
        navigationTargetSubscription.unsubscribe();
        menuExpandedSubscription.unsubscribe();
        selectedNavigationButtonPin.unsubscribe();
        newVersionAvailablePin.unsubscribe();
        isMuSigActivatedPin.unsubscribe();
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

    private LeftNavSubButton createSubmenuNavigationButton(String title,
                                                           NavigationTarget navigationTarget,
                                                           LeftNavButton parentButton) {
        LeftNavSubButton button = new LeftNavSubButton(title, toggleGroup, navigationTarget, parentButton);
        setupButtonHandler(navigationTarget, button);
        VBox.setVgrow(button, Priority.ALWAYS);
        return button;
    }

    private VBox createSubmenu(LeftNavSubButton... items) {
        VBox submenu = new VBox(2);
        VBox.setMargin(submenu, new Insets(-10, 0, 0, 0));
        submenu.setPadding(new Insets(0, 0, 16, 0));
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
    }

    private void verticalExpandCollapseHandler(NavigationTarget navigationTarget) {
        if (model.getSelectedNavigationButton().get() == null) {
            // There is no button selected on startup
            controller.onNavigationTargetSelected(navigationTarget);
        }
        maybeAnimateMark();
    }

    private void maybeAnimateMark() {
        Optional<LeftNavButton> buttonForAnimation = findButtonForAnimation();
        double height = findButtonForAnimation().map(Region::getHeight).orElse(0d);
        if (height > 0) {
            Transitions.animateNavigationButtonMarks(selectionMarker, height, calculateTargetY());
            updateSubmenu();
        } else {
            // If height is zero we try again after a short delay
            UIScheduler.run(this::maybeAnimateMark).after(20);
        }
    }

    private Optional<LeftNavButton> findButtonForAnimation() {
        LeftNavButton selectedLeftNavButton = model.getSelectedNavigationButton().get();
        if (selectedLeftNavButton == null) {
            return Optional.empty();
        }

        if (selectedLeftNavButton instanceof LeftNavSubButton leftNavSubButton) {
            LeftNavButton parentButton = leftNavSubButton.getParentButton();
            if (!parentButton.getIsSubMenuExpanded().get()) {
                return Optional.of(parentButton);
            }
        }

        return Optional.of(selectedLeftNavButton);
    }

    private double calculateTargetY() {
        LeftNavButton selectedLeftNavButton = model.getSelectedNavigationButton().get();
        double targetY = menuTop + selectedLeftNavButton.getBoundsInParent().getMinY();
        if (selectedLeftNavButton instanceof LeftNavSubButton leftNavSubButton) {
            LeftNavButton parentButton = leftNavSubButton.getParentButton();
            if (parentButton.getIsSubMenuExpanded().get()) {
                for (int i = 0; i < mainMenuItems.getChildren().size(); i++) {
                    Node item = mainMenuItems.getChildren().get(i);
                    if (item instanceof VBox submenu) {
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
            if (item instanceof VBox submenu) {
                LeftNavButton parentMenuItem = (LeftNavButton) mainMenuItems.getChildren().get(i - 1);

                parentMenuItem.setHighlighted(submenu.getChildren().contains(selectedLeftNavButton));

                UIThread.runOnNextRenderFrame(parentMenuItem::applyStyle);
                int targetHeight = parentMenuItem.getIsSubMenuExpanded().get() ?
                        (LeftNavSubButton.HEIGHT + 2) * submenu.getChildren().size() : 0;
                Transitions.animatePrefHeight(submenu, targetHeight);
            }
        }
    }
}
