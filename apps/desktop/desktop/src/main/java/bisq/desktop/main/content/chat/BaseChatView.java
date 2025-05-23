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

package bisq.desktop.main.content.chat;

import bisq.chat.notifications.ChatChannelNotificationType;
import bisq.common.data.Pair;
import bisq.common.observable.ReadOnlyObservable;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.NavigationView;
import bisq.desktop.components.controls.DropdownBisqMenuItem;
import bisq.desktop.components.controls.DropdownMenu;
import bisq.desktop.components.controls.DropdownMenuItem;
import bisq.desktop.components.controls.DropdownTitleMenuItem;
import bisq.desktop.components.controls.SearchBox;
import bisq.i18n.Res;
import bisq.settings.ChatNotificationType;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public abstract class BaseChatView extends NavigationView<ScrollPane, BaseChatModel, BaseChatController<?, ?>> {
    public final static double HEADER_HEIGHT = 61;

    protected final Label channelTitle = new Label();
    protected final Label channelDescription = new Label();
    protected final Label channelIcon = new Label();
    protected final VBox sideBar = new VBox();
    protected final VBox centerVBox = new VBox();
    protected final HBox titleHBox = new HBox(10);
    protected final HBox containerHBox = new HBox();
    protected final Pane channelSidebar, chatMessagesComponent;
    protected final SearchBox searchBox = new SearchBox();
    protected final DropdownMenu ellipsisMenu = new DropdownMenu("ellipsis-v-grey", "ellipsis-v-white", true);
    protected final DropdownMenu notificationsSettingsMenu = new DropdownMenu("icon-notification-all-grey", "icon-notification-all-white", true);
    protected DropdownBisqMenuItem helpButton, infoButton;
    private NotificationSettingMenuItem globalDefault, all, mention, off;
    protected Subscription channelIconPin, selectedNotificationSettingPin, notificationsSettingsMenuIsShowingPin;

    private final KeyCodeCombination searchShortcut = new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN);
    private final EventHandler<KeyEvent> searchShortcutHandler;

    public BaseChatView(BaseChatModel model,
                        BaseChatController<?, ?> controller,
                        Pane chatMessagesComponent,
                        Pane channelSidebar) {
        super(new ScrollPane(), model, controller);

        this.chatMessagesComponent = chatMessagesComponent;
        this.channelSidebar = channelSidebar;

        setUpEllipsisMenu();
        setupNotificationsSettingMenu();

        configTitleHBox();
        configCenterVBox();
        configSideBarVBox();
        configContainerHBox();

        root.setFitToWidth(true);
        root.setFitToHeight(true);

        searchShortcutHandler = event -> {
            if (searchShortcut.match(event) && !searchBox.isDisabled()) {
                searchBox.requestFieldFocus();
                event.consume();
            }
        };
    }

    protected abstract void configTitleHBox();

    protected abstract void configCenterVBox();

    protected abstract void configSideBarVBox();

    protected abstract void configContainerHBox();

    @Override
    protected void onViewAttached() {
        channelTitle.textProperty().bind(model.getChannelTitle());
        channelDescription.textProperty().bind(model.getChannelDescription());
        channelSidebar.visibleProperty().bind(model.getChannelSidebarVisible());
        channelSidebar.managedProperty().bind(model.getChannelSidebarVisible());
        sideBar.visibleProperty().bind(model.getSideBarVisible());
        sideBar.managedProperty().bind(model.getSideBarVisible());
        searchBox.textProperty().bindBidirectional(model.getSearchText());

        if (helpButton != null) {
            helpButton.setLabelText(controller.getHelpButtonText());
            helpButton.setOnAction(e -> controller.onOpenHelp());
        }
        if (infoButton != null) {
            infoButton.setOnAction(e -> controller.onToggleChannelInfo());
        }
        if (globalDefault != null) {
            globalDefault.setOnAction(e -> controller.onSetNotificationType(globalDefault.getType()));
        }
        if (all != null) {
            all.setOnAction(e -> controller.onSetNotificationType(all.getType()));
        }
        if (mention != null) {
            mention.setOnAction(e -> controller.onSetNotificationType(mention.getType()));
        }
        if (off != null) {
            off.setOnAction(e -> controller.onSetNotificationType(off.getType()));
        }

        channelIconPin = EasyBind.subscribe(model.getChannelIconId(), channelIconId -> {
            ImageView image = ImageUtil.getImageViewById(channelIconId);
            image.setScaleX(1.25);
            image.setScaleY(1.25);
            channelIcon.setGraphic(image);
        });

        selectedNotificationSettingPin = EasyBind.subscribe(model.getSelectedNotificationSetting(),
                this::applySelectedNotificationSetting);

        notificationsSettingsMenuIsShowingPin = EasyBind.subscribe(notificationsSettingsMenu.getIsMenuShowing(),
                this::updateMenuItemsStyle);

        // Apply initial state of the icon when view is attached
        if (model.getSelectedNotificationSetting().get() != null) {
            applySelectedNotificationSetting(model.getSelectedNotificationSetting().get());
        }

        root.addEventFilter(KeyEvent.KEY_PRESSED, searchShortcutHandler);
    }

    @Override
    protected void onViewDetached() {
        channelTitle.textProperty().unbind();
        channelDescription.textProperty().unbind();
        channelSidebar.visibleProperty().unbind();
        channelSidebar.managedProperty().unbind();
        sideBar.visibleProperty().unbind();
        sideBar.managedProperty().unbind();
        searchBox.textProperty().unbindBidirectional(model.getSearchText());

        if (helpButton != null) {
            helpButton.setOnAction(null);
        }
        if (infoButton != null) {
            infoButton.setOnAction(null);
        }
        if (globalDefault != null) {
            globalDefault.dispose();
        }
        if (all != null) {
            all.dispose();
        }
        if (mention != null) {
            mention.dispose();
        }
        if (off != null) {
            off.dispose();
        }

        channelIconPin.unsubscribe();
        selectedNotificationSettingPin.unsubscribe();
        notificationsSettingsMenuIsShowingPin.unsubscribe();

        if (searchShortcutHandler != null) {
            root.removeEventFilter(KeyEvent.KEY_PRESSED, searchShortcutHandler);
        }
    }

    private void setUpEllipsisMenu() {
        helpButton = new DropdownBisqMenuItem("icon-help-grey", "icon-help-white");
        infoButton = new DropdownBisqMenuItem("icon-info-grey", "icon-info-white",
                Res.get("chat.ellipsisMenu.channelInfo"));
        ellipsisMenu.addMenuItems(helpButton, infoButton);
        ellipsisMenu.setTooltip(Res.get("chat.ellipsisMenu.tooltip"));
    }

    private void setupNotificationsSettingMenu() {
        DropdownTitleMenuItem title = new DropdownTitleMenuItem(Res.get("chat.notificationsSettingsMenu.title"));
        globalDefault = createAndGetNotificationSettingMenuItem(ChatChannelNotificationType.GLOBAL_DEFAULT,
                Res.get("chat.notificationsSettingsMenu.globalDefault"));
        all = createAndGetNotificationSettingMenuItem(ChatChannelNotificationType.ALL,
                Res.get("chat.notificationsSettingsMenu.all"));
        mention = createAndGetNotificationSettingMenuItem(ChatChannelNotificationType.MENTION,
                Res.get("chat.notificationsSettingsMenu.mention"));
        off = createAndGetNotificationSettingMenuItem(ChatChannelNotificationType.OFF,
                Res.get("chat.notificationsSettingsMenu.off"));
        notificationsSettingsMenu.getStyleClass().add("notifications-settings-menu");
        notificationsSettingsMenu.addMenuItems(title, globalDefault, all, mention, off);
        notificationsSettingsMenu.setTooltip(Res.get("chat.notificationsSettingsMenu.tooltip"));
    }

    private NotificationSettingMenuItem createAndGetNotificationSettingMenuItem(ChatChannelNotificationType type, String text) {
        ImageView defaultIcon = ImageUtil.getImageViewById(getIconIdsForNotificationType(type).getFirst());
        ImageView activeIcon = ImageUtil.getImageViewById(getIconIdsForNotificationType(type).getSecond());
        Label label = new Label(text);
        return new NotificationSettingMenuItem(type, label, defaultIcon, activeIcon);
    }

    private Pair<String, String> getIconIdsForNotificationType(ChatChannelNotificationType type) {
        return switch (type) {
            case ALL -> new Pair<>("icon-notification-all-grey", "icon-notification-all-white");
            case MENTION -> new Pair<>("icon-notification-mention-grey", "icon-notification-mention-white");
            case OFF -> new Pair<>("icon-notification-off-grey", "icon-notification-off-white");
            default -> {
                ReadOnlyObservable<ChatNotificationType> globalDefault = controller.serviceProvider.getSettingsService().getChatNotificationType();
                yield switch (globalDefault.get()) {
                    case ChatNotificationType.ALL -> getIconIdsForNotificationType(ChatChannelNotificationType.ALL);
                    case ChatNotificationType.OFF -> getIconIdsForNotificationType(ChatChannelNotificationType.OFF);
                    case ChatNotificationType.MENTION ->
                            getIconIdsForNotificationType(ChatChannelNotificationType.MENTION);
                };
            }
        };
    }

    private void applySelectedNotificationSetting(ChatChannelNotificationType type) {
        globalDefault.updateSelection(type == globalDefault.getType());
        all.updateSelection(type == all.getType());
        mention.updateSelection(type == mention.getType());
        off.updateSelection(type == off.getType());

        Pair<String, String> icons = getIconIdsForNotificationType(type);
        notificationsSettingsMenu.setIcons(icons.getFirst(), icons.getSecond());
    }

    private void updateMenuItemsStyle(boolean isMenuShowing) {
        if (isMenuShowing) {
            if (globalDefault.isSelected()) {
                globalDefault.showAsActive();
            } else if (all.isSelected()) {
                all.showAsActive();
            } else if (mention.isSelected()) {
                mention.showAsActive();
            } else if (off.isSelected()) {
                off.showAsActive();
            }
        } else {
            globalDefault.resetStyle();
            all.resetStyle();
            mention.resetStyle();
            off.resetStyle();
        }
    }

    @Getter
    private static final class NotificationSettingMenuItem extends DropdownMenuItem {
        private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");
        private static final String NOTIFICATIONS_SETTINGS_MENU_LABEL_ACTIVE_STYLE = "notifications-settings-menu-label-active";
        private static final String NOTIFICATIONS_SETTINGS_MENU_LABEL_DEFAULT_STYLE = "notifications-settings-menu-label-default";

        private final ChatChannelNotificationType type;
        private final ImageView defaultIcon, activeIcon;
        private final Label displayLabel;

        private NotificationSettingMenuItem(ChatChannelNotificationType type, Label displayLabel,
                                            ImageView defaultIcon, ImageView activeIcon) {
            super("check-white", "check-white", displayLabel);

            this.type = type;
            this.defaultIcon = defaultIcon;
            this.activeIcon = activeIcon;
            this.displayLabel = displayLabel;

            getStyleClass().add("dropdown-menu-item");
            updateSelection(false);
            initialize();
        }

        private void initialize() {
            displayLabel.setGraphicTextGap(10);
            resetStyle();

            getContent().setOnMouseClicked(e -> showAsActive());
            getContent().setOnMouseEntered(e -> showAsActive());
            getContent().setOnMouseExited(e -> showAsDefault());
        }

        public void dispose() {
            setOnAction(null);
            getContent().setOnMouseClicked(null);
            getContent().setOnMouseEntered(null);
            getContent().setOnMouseExited(null);
        }

        void updateSelection(boolean isSelected) {
            getContent().pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, isSelected);
        }

        boolean isSelected() {
            return getContent().getPseudoClassStates().contains(SELECTED_PSEUDO_CLASS);
        }

        void resetStyle() {
            displayLabel.setGraphic(defaultIcon);
            resetNotificationSettingsMenuLabelStyle();
            displayLabel.getStyleClass().add(NOTIFICATIONS_SETTINGS_MENU_LABEL_DEFAULT_STYLE);
        }

        private void showAsActive() {
            displayLabel.setGraphic(activeIcon);
            resetNotificationSettingsMenuLabelStyle();
            displayLabel.getStyleClass().add(NOTIFICATIONS_SETTINGS_MENU_LABEL_ACTIVE_STYLE);
        }

        private void showAsDefault() {
            if (isSelected()) {
                showAsActive();
                return;
            }
            resetStyle();
        }

        private void resetNotificationSettingsMenuLabelStyle() {
            displayLabel.getStyleClass().remove(NOTIFICATIONS_SETTINGS_MENU_LABEL_DEFAULT_STYLE);
            displayLabel.getStyleClass().remove(NOTIFICATIONS_SETTINGS_MENU_LABEL_ACTIVE_STYLE);
        }
    }
}
