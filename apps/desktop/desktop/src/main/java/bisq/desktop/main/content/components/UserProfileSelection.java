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

package bisq.desktop.main.content.components;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatChannelSelectionService;
import bisq.chat.ChatMessage;
import bisq.chat.priv.PrivateChatChannel;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.controls.DropdownMenu;
import bisq.desktop.components.controls.DropdownMenuItem;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Map;

@Slf4j
public class UserProfileSelection {
    private final Controller controller;

    public UserProfileSelection(ServiceProvider serviceProvider) {
        controller = new Controller(serviceProvider, 30, false);
    }

    public UserProfileSelection(ServiceProvider serviceProvider, int iconSize, boolean useMaterialStyle) {
        controller = new Controller(serviceProvider, iconSize, useMaterialStyle);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void setMaxComboBoxWidth(int width) {
        controller.view.setMenuMaxWidth(width);
    }

    public boolean isFocused() {
        return controller.isFocused();
    }

    public ReadOnlyBooleanProperty focusedProperty() {
        return controller.focusedProperty();
    }

    public void requestFocus() {
        controller.requestFocus();
    }

    public void setPrefWidth(double value) {
        controller.setPrefWidth(value);
    }

    public void openMenuUpwards() {
        controller.view.getDropdownMenu().setOpenUpwards(true);
    }

    public void openMenuToTheRight() {
        controller.view.getDropdownMenu().setOpenToTheRight(true);
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final UserIdentityService userIdentityService;
        private final Map<ChatChannelDomain, ChatChannelSelectionService> chatChannelSelectionServices;
        private final ReputationService reputationService;
        private Pin selectedUserProfilePin, chatChannelSelectionPin, navigationPin, userIdentitiesPin;
        private Subscription isPrivateChannelPin;

        private Controller(ServiceProvider serviceProvider, int iconSize, boolean useMaterialStyle) {
            this.userIdentityService = serviceProvider.getUserService().getUserIdentityService();
            chatChannelSelectionServices = serviceProvider.getChatService().getChatChannelSelectionServices();
            reputationService = serviceProvider.getUserService().getReputationService();

            model = new Model();
            view = new View(model, this, iconSize, useMaterialStyle);
        }

        @Override
        public void onActivate() {
            selectedUserProfilePin = FxBindings.subscribe(userIdentityService.getSelectedUserIdentityObservable(),
                    userIdentity -> UIThread.run(() -> {
                        model.setUserReputationScore(reputationService.getReputationScore(userIdentity.getUserProfile()));
                        model.getSelectedUserIdentity().set(userIdentity);
                    }));
            userIdentitiesPin = userIdentityService.getUserIdentities().addObserver(() -> UIThread.run(this::updateUserProfiles));
            navigationPin = Navigation.getCurrentNavigationTarget().addObserver(this::navigationTargetChanged);
            isPrivateChannelPin = EasyBind.subscribe(model.getIsPrivateChannel(), isPrivate -> updateShouldShowMenu());
        }

        @Override
        public void onDeactivate() {
            selectedUserProfilePin.unbind();
            navigationPin.unbind();
            if (chatChannelSelectionPin != null) {
                chatChannelSelectionPin.unbind();
            }
            userIdentitiesPin.unbind();
            isPrivateChannelPin.unsubscribe();
        }

        private void updateUserProfiles() {
            model.getUserProfiles().forEach(UserProfileMenuItem::dispose);
            model.getUserProfiles().clear();
            userIdentityService.getUserIdentities().forEach(userIdentity -> {
                UserProfileMenuItem userProfileMenuItem = new UserProfileMenuItem(
                        userIdentity, reputationService.getReputationScore(userIdentity.getUserProfile()));
                userProfileMenuItem.setOnAction(e -> onSelected(userProfileMenuItem));
                model.getUserProfiles().add(userProfileMenuItem);
            });
            updateShouldShowMenu();
        }

        private void onSelected(UserProfileMenuItem selectedItem) {
            if (selectedItem != null) {
                UserIdentity selectedUserIdentity = userIdentityService.getSelectedUserIdentity();
                // To make sure a different user is never selected for a private channel it's safest to keep this check
                // even though the combobox should be disabled
                if (model.getIsPrivateChannel().get()) {
                    new Popup().warning(Res.get("chat.privateChannel.changeUserProfile.warn",
                                    selectedUserIdentity.getUserProfile().getUserName()))
                            .onClose(() -> {
                                UIThread.run(() -> {
                                    model.setUserReputationScore(reputationService.getReputationScore(selectedUserIdentity.getUserProfile()));
                                    model.getSelectedUserIdentity().set(null);
                                    model.getSelectedUserIdentity().set(selectedUserIdentity);
                                });
                            })
                            .show();
                } else {
                    userIdentityService.selectChatUserIdentity(selectedItem.getUserIdentity());
                }
            }
        }

        private boolean isFocused() {
            return view.getDropdownMenu().isFocused();
        }

        private ReadOnlyBooleanProperty focusedProperty() {
            return view.getDropdownMenu().focusedProperty();
        }

        private void requestFocus() {
            view.getDropdownMenu().requestFocus();
        }

        private void setPrefWidth(double value) {
            view.setMenuPrefWidth(value);
        }

        private void navigationTargetChanged(NavigationTarget navigationTarget) {
            if (chatChannelSelectionPin != null) {
                chatChannelSelectionPin.unbind();
            }
            model.getIsPrivateChannel().set(false);
            switch (navigationTarget) {
                case BISQ_EASY_OFFERBOOK:
                    selectionServiceChanged(chatChannelSelectionServices.get(ChatChannelDomain.BISQ_EASY_OFFERBOOK));
                    return;
                case BISQ_EASY_OPEN_TRADES:
                    selectionServiceChanged(chatChannelSelectionServices.get(ChatChannelDomain.BISQ_EASY_OPEN_TRADES));
                    return;
                case CHAT:
                    selectionServiceChanged(chatChannelSelectionServices.get(ChatChannelDomain.DISCUSSION));
                    return;
                case SUPPORT:
                    selectionServiceChanged(chatChannelSelectionServices.get(ChatChannelDomain.SUPPORT));
            }
        }

        private void selectionServiceChanged(ChatChannelSelectionService chatChannelSelectionService) {
            chatChannelSelectionPin = chatChannelSelectionService.getSelectedChannel().addObserver(this::selectedChannelChanged);
        }

        private void selectedChannelChanged(ChatChannel<? extends ChatMessage> channel) {
            UIThread.run(() -> model.getIsPrivateChannel().set(channel instanceof PrivateChatChannel));
        }

        private void updateShouldShowMenu() {
            model.getShouldShowMenu().set(!model.getIsPrivateChannel().get() && model.getUserProfiles().size() > 1);
        }
    }

    @Slf4j
    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<UserIdentity> selectedUserIdentity = new SimpleObjectProperty<>();
        private final ObservableList<UserProfileMenuItem> userProfiles = FXCollections.observableArrayList();
        private final BooleanProperty isPrivateChannel = new SimpleBooleanProperty(false);
        private final BooleanProperty shouldShowMenu = new SimpleBooleanProperty(false);
        private final DoubleProperty menuWidth = new SimpleDoubleProperty();
        @Setter
        private ReputationScore userReputationScore;
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<Pane, Model, Controller> {
        private final static int DEFAULT_MENU_WIDTH = 200;

        private final UserProfileDisplay userProfileDisplay;
        private final UserProfileDisplay singleUserProfileDisplay;
        @Getter
        private final DropdownMenu dropdownMenu;
        private final HBox singleUserProfileHBox;
        private final ListChangeListener<UserProfileMenuItem> userProfilesListener = change -> updateMenuItems();
        private Subscription selectedUserProfilePin, menuWidthPin;

        private View(Model model, Controller controller, int iconSize, boolean useMaterialStyle) {
            super(new Pane(), model, controller);

            userProfileDisplay = new UserProfileDisplay(iconSize);
            dropdownMenu = new DropdownMenu("chevron-drop-menu-grey", "chevron-drop-menu-white", false);
            dropdownMenu.setContent(userProfileDisplay);
            dropdownMenu.useSpaceBetweenContentAndIcon();

            singleUserProfileDisplay = new UserProfileDisplay(iconSize);
            singleUserProfileHBox = new HBox(singleUserProfileDisplay);
            singleUserProfileHBox.getStyleClass().add("single-user-profile");
            singleUserProfileHBox.setFillHeight(true);

            if (useMaterialStyle) {
                root.getStyleClass().add("user-profile-selection-material-design");
                dropdownMenu.setOpenToTheRight(true);
            } else {
                root.getStyleClass().add("user-profile-selection");
            }
            root.getChildren().setAll(dropdownMenu, singleUserProfileHBox);
            root.setPrefHeight(60);
        }

        @Override
        protected void onViewAttached() {
            dropdownMenu.visibleProperty().bind(model.getShouldShowMenu());
            dropdownMenu.managedProperty().bind(model.getShouldShowMenu());
            singleUserProfileHBox.visibleProperty().bind(model.getShouldShowMenu().not());
            singleUserProfileHBox.managedProperty().bind(model.getShouldShowMenu().not());

            selectedUserProfilePin = EasyBind.subscribe(model.getSelectedUserIdentity(), selectedUserIdentity -> {
                userProfileDisplay.setUserProfile(selectedUserIdentity.getUserProfile());
                userProfileDisplay.setReputationScore(model.getUserReputationScore());
                singleUserProfileDisplay.setUserProfile(selectedUserIdentity.getUserProfile());
                singleUserProfileDisplay.setReputationScore(model.getUserReputationScore());
                model.getUserProfiles().forEach(userProfileMenuItem ->
                        userProfileMenuItem.updateSelection(selectedUserIdentity.equals(userProfileMenuItem.getUserIdentity())));
            });
            menuWidthPin = EasyBind.subscribe(model.getMenuWidth(), w -> setMenuPrefWidth(w.doubleValue()));

            model.getUserProfiles().addListener(userProfilesListener);
            updateMenuItems();
        }

        @Override
        protected void onViewDetached() {
            dropdownMenu.visibleProperty().unbind();
            dropdownMenu.managedProperty().unbind();
            singleUserProfileHBox.visibleProperty().unbind();
            singleUserProfileHBox.managedProperty().unbind();

            selectedUserProfilePin.unsubscribe();
            menuWidthPin.unsubscribe();

            dropdownMenu.clearMenuItems();
            model.getUserProfiles().removeListener(userProfilesListener);
        }

        private void setMenuPrefWidth(double width) {
            dropdownMenu.setPrefWidth(width == 0 ? DEFAULT_MENU_WIDTH : width);
        }

        private void setMenuMaxWidth(double width) {
            setMenuPrefWidth(width);
            dropdownMenu.setMaxWidth(width == 0 ? DEFAULT_MENU_WIDTH : width);
        }

        private void updateMenuItems() {
            dropdownMenu.clearMenuItems();
            dropdownMenu.addMenuItems(model.getUserProfiles());
        }
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
    @Getter
    public static final class UserProfileMenuItem extends DropdownMenuItem {
        private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");

        @EqualsAndHashCode.Include
        private final UserIdentity userIdentity;

        private UserProfileMenuItem(UserIdentity userIdentity, ReputationScore reputationScore) {
            super("check-white", "check-white", new UserProfileDisplay(userIdentity.getUserProfile(), reputationScore));

            this.userIdentity = userIdentity;
            getStyleClass().add("dropdown-menu-item");
            updateSelection(false);
            initialize();
        }

        public void initialize() {
        }

        public void dispose() {
            setOnAction(null);
        }

        void updateSelection(boolean isSelected) {
            getContent().pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, isSelected);
        }

        boolean isSelected() {
            return getContent().getPseudoClassStates().contains(SELECTED_PSEUDO_CLASS);
        }

        @Override
        public String toString() {
            return userIdentity != null ? userIdentity.getUserName() : "";
        }
    }
}
