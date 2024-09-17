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
import bisq.common.observable.Observable;
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
        private Pin selectedUserProfilePin, userProfilesPin, chatChannelSelectionPin, navigationPin, isPrivateChannelPin;
        private final ListChangeListener<UserProfileMenuItem> userProfilesListener = change -> updateShouldShowMenu();

        private Controller(ServiceProvider serviceProvider, int iconSize, boolean useMaterialStyle) {
            this.userIdentityService = serviceProvider.getUserService().getUserIdentityService();
            chatChannelSelectionServices = serviceProvider.getChatService().getChatChannelSelectionServices();

            model = new Model();
            view = new View(model, this, iconSize, useMaterialStyle);
        }

        @Override
        public void onActivate() {
            selectedUserProfilePin = FxBindings.subscribe(userIdentityService.getSelectedUserIdentityObservable(),
                    userIdentity -> UIThread.run(() -> model.getSelectedUserIdentity().set(userIdentity)));
            userProfilesPin = FxBindings.<UserIdentity, UserProfileMenuItem>bind(model.getUserProfiles())
                    .map(userIdentity -> {
                        UserProfileMenuItem userProfileMenuItem = new UserProfileMenuItem(userIdentity);
                        userProfileMenuItem.setOnAction(e -> onSelected(userProfileMenuItem));
                        return userProfileMenuItem;
                    })
                    .to(userIdentityService.getUserIdentities());

            navigationPin = Navigation.getCurrentNavigationTarget().addObserver(this::navigationTargetChanged);

            model.getUserProfiles().addListener(userProfilesListener);
            isPrivateChannelPin = FxBindings.subscribe(model.getIsPrivateChannel(), isPrivate -> updateShouldShowMenu());
        }

        @Override
        public void onDeactivate() {
            // Need to clear list otherwise we get issues with binding when multiple 
            // instances are used.
            model.getUserProfiles().forEach(UserProfileMenuItem::dispose);
            model.getUserProfiles().clear();
            model.getUserProfiles().removeListener(userProfilesListener);

            selectedUserProfilePin.unbind();
            userProfilesPin.unbind();
            navigationPin.unbind();
            if (chatChannelSelectionPin != null) {
                chatChannelSelectionPin.unbind();
            }
            isPrivateChannelPin.unbind();
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
                                model.getSelectedUserIdentity().set(null);
                                model.getSelectedUserIdentity().set(selectedUserIdentity);
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
        private final Observable<Boolean> isPrivateChannel = new Observable<>(false);
        private final BooleanProperty shouldShowMenu = new SimpleBooleanProperty(false);
        private final DoubleProperty menuWidth = new SimpleDoubleProperty();
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<Pane, Model, Controller> {
        private final static int DEFAULT_MENU_WIDTH = 200;

        private final UserProfileDisplay userProfileDisplay;
        @Getter
        private final DropdownMenu dropdownMenu;
        private final HBox singleUserProfileHBox;
        private Subscription selectedUserProfilePin, menuWidthPin;

        private View(Model model, Controller controller, int iconSize, boolean useMaterialStyle) {
            super(new Pane(), model, controller);

            userProfileDisplay = new UserProfileDisplay(iconSize);

            dropdownMenu = new DropdownMenu("chevron-drop-menu-grey", "chevron-drop-menu-white", false);
            dropdownMenu.setTooltip(Res.get("user.userProfile.comboBox.description"));
            dropdownMenu.setContent(userProfileDisplay);
            dropdownMenu.useSpaceBetweenContentAndIcon();

            singleUserProfileHBox = new HBox(userProfileDisplay);
            singleUserProfileHBox.getStyleClass().add("single-user-profile");
            singleUserProfileHBox.setFillHeight(true);

            if (useMaterialStyle) {
                root.getStyleClass().add("user-profile-selection-material-design");
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
                model.getUserProfiles().forEach(userProfileMenuItem -> {
                    userProfileMenuItem.updateSelection(selectedUserIdentity.equals(userProfileMenuItem.getUserIdentity()));
                });
            });
            menuWidthPin = EasyBind.subscribe(model.getMenuWidth(), w -> setMenuPrefWidth(w.doubleValue()));
            dropdownMenu.addMenuItems(model.getUserProfiles());
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
        }

        private void setMenuPrefWidth(double width) {
            dropdownMenu.setPrefWidth(width == 0 ? DEFAULT_MENU_WIDTH : width);
        }

        private void setMenuMaxWidth(double width) {
            setMenuPrefWidth(width);
            dropdownMenu.setMaxWidth(width == 0 ? DEFAULT_MENU_WIDTH : width);
        }
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
    @Getter
    public static final class UserProfileMenuItem extends DropdownMenuItem {
        private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");

        @EqualsAndHashCode.Include
        private final UserIdentity userIdentity;

        private UserProfileMenuItem(UserIdentity userIdentity) {
            super("check-white", "check-white", new UserProfileDisplay(userIdentity.getUserProfile()));

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
