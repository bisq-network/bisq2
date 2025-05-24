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
    protected Subscription channelIconPin, selectedNotificationSettingPin;

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
        globalDefault = new NotificationSettingMenuItem("check-white", "check-white",
                Res.get("chat.notificationsSettingsMenu.globalDefault"), ChatChannelNotificationType.GLOBAL_DEFAULT);
        all = new NotificationSettingMenuItem("check-white", "check-white",
                Res.get("chat.notificationsSettingsMenu.all"), ChatChannelNotificationType.ALL);
        mention = new NotificationSettingMenuItem("check-white", "check-white",
                Res.get("chat.notificationsSettingsMenu.mention"), ChatChannelNotificationType.MENTION);
        off = new NotificationSettingMenuItem("check-white", "check-white",
                Res.get("chat.notificationsSettingsMenu.off"), ChatChannelNotificationType.OFF);
        notificationsSettingsMenu.getStyleClass().add("notifications-settings-menu");
        notificationsSettingsMenu.addMenuItems(title, globalDefault, all, mention, off);
        notificationsSettingsMenu.setTooltip(Res.get("chat.notificationsSettingsMenu.tooltip"));
    }

    private Pair<String, String> getIconIdsForNotificationType(ChatChannelNotificationType type) {
        switch (type) {
            case ALL:
                return new Pair<>("icon-notification-all-grey", "icon-notification-all-white");
            case MENTION:
                return new Pair<>("icon-notification-mention-grey", "icon-notification-mention-white");
            case OFF:
                return new Pair<>("icon-notification-off-grey", "icon-notification-off-white");
            case GLOBAL_DEFAULT:
            default:
                ReadOnlyObservable<ChatNotificationType> globalDefault = controller.serviceProvider.getSettingsService().getChatNotificationType();
                return switch (globalDefault.get()) {
                    case ChatNotificationType.ALL -> getIconIdsForNotificationType(ChatChannelNotificationType.ALL);
                    case ChatNotificationType.OFF -> getIconIdsForNotificationType(ChatChannelNotificationType.OFF);
                    case ChatNotificationType.MENTION ->
                            getIconIdsForNotificationType(ChatChannelNotificationType.MENTION);
                };
        }
    }

    private void applySelectedNotificationSetting(ChatChannelNotificationType type) {
        globalDefault.updateSelection(type == globalDefault.getType());
        all.updateSelection(type == all.getType());
        mention.updateSelection(type == mention.getType());
        off.updateSelection(type == off.getType());

        Pair<String, String> icons = getIconIdsForNotificationType(type);
        notificationsSettingsMenu.setIcons(icons.getFirst(), icons.getSecond());
    }

    @Getter
    private static final class NotificationSettingMenuItem extends DropdownBisqMenuItem {
        private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");

        private final ChatChannelNotificationType type;

        private NotificationSettingMenuItem(String defaultIconId,
                                            String activeIconId,
                                            String text,
                                            ChatChannelNotificationType type) {
            super(defaultIconId, activeIconId, text);

            this.type = type;
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
    }
}
